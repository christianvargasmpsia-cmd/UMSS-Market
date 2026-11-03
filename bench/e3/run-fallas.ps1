param(
    [ValidateRange(1, 10)][int]$Rondas = 1,
    [ValidateRange(15, 120)][int]$DuracionSegundos = 30,
    [ValidateRange(5, 60)][int]$CalentamientoSegundos = 15,
    [ValidateRange(1, 100)][int]$RatePorRuta = 5
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repo
if ((git branch --show-current).Trim() -ne 'feature/e3-extraccion-resenas') {
    throw 'Rama incorrecta: se requiere feature/e3-extraccion-resenas.'
}
foreach ($f in @('bench/e3/.env','bench/e3/compose.yaml','bench/e3/fallas.js')) {
    if (-not (Test-Path -LiteralPath $f)) { throw "Falta archivo: $f" }
}

$dcA = @('--env-file','bench/e3/.env','-f','bench/e3/compose.yaml','-p','umss-e3','--profile','a')
$dcB = @('--env-file','bench/e3/.env','-f','bench/e3/compose.yaml','-p','umss-e3','--profile','b')
$proxyUri = 'http://127.0.0.1:8474/proxies/reviews'
$agente = 'umss-e3-client/1.0'
$id = '22222222-2222-4222-8222-222222222222'
$urlPublicacion = "http://127.0.0.1:18080/api/publications/$id"
$urlResenas = "$urlPublicacion/reviews?limit=20"

$null = docker image inspect grafana/k6:1.8.2 --format '{{.Id}}' 2>&1
if ($LASTEXITCODE -ne 0) { throw 'No esta disponible la imagen grafana/k6:1.8.2.' }
$serviciosB = @(docker compose @dcB ps --services --status running)
if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer el estado de Docker Compose.' }
foreach ($s in @('db','api-b','reviews-b','toxiproxy')) {
    if ($serviciosB -notcontains $s) { throw "El servicio $s no esta en ejecucion. No alterar Docker automaticamente." }
}
$serviciosA = @(docker compose @dcA ps --services --status running)
if ($LASTEXITCODE -ne 0) { throw 'No se pudo comprobar el estado de A.' }
if ($serviciosA -contains 'api-a') { throw 'api-a sigue activa. Detenla antes de ejecutar H2/H3.' }

function Get-Proxy {
    $p = Invoke-RestMethod -Uri $proxyUri -Method Get -UserAgent $agente
    if ($p.name -ne 'reviews' -or $p.upstream -ne 'reviews-b:8081') {
        throw 'Proxy reviews inexistente o con destino inesperado.'
    }
    if ($null -ne $p.toxics -and @($p.toxics).Count -gt 0) {
        throw 'Hay toxics configurados. No modificar ni ejecutar H2/H3.'
    }
    return $p
}
function Set-Proxy([bool]$habilitado) {
    $json = if ($habilitado) { '{"enabled":true}' } else { '{"enabled":false}' }
    $p = Invoke-RestMethod -Uri $proxyUri -Method Post -UserAgent $agente `
        -ContentType 'application/json' -Body $json
    if ($p.enabled -ne $habilitado -or $p.name -ne 'reviews') {
        throw "Toxiproxy no confirmo enabled=$habilitado."
    }
}
function Get-Codigo([string]$url) {
    $resultado = & curl.exe --silent --max-time 5 --output NUL --write-out '%{http_code}' $url
    if ($LASTEXITCODE -ne 0) { throw "Sin respuesta HTTP valida: $url" }
    return "$resultado".Trim()
}
function Confirmar-HTTP([string]$esperadoResenas) {
    $p = Get-Codigo $urlPublicacion
    $r = Get-Codigo $urlResenas
    if ($p -ne '200' -or $r -ne $esperadoResenas) {
        throw "Rutas E3 inesperadas: publicacion=$p, resenas=$r (esperado=$esperadoResenas)."
    }
}
function Comprobar-Seed {
    $sql = 'SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM publications), (SELECT COUNT(*) FROM product_reviews);'
    $conteos = docker compose @dcB exec -T db psql -X -U e3 -d e3_umss -At -c $sql
    if ($LASTEXITCODE -ne 0 -or "$conteos".Trim() -ne '1|1|100') {
        throw "Seed E3 no coincide (se observa $conteos). No borrar ni reinsertar datos."
    }
}
function Percentil([double[]]$numeros, [double]$p) {
    if ($numeros.Length -eq 0) { throw 'No hay muestras de latencia.' }
    [Array]::Sort($numeros)
    $pos = ($numeros.Length - 1) * $p
    $lo = [int][Math]::Floor($pos)
    $hi = [int][Math]::Ceiling($pos)
    return [double]($numeros[$lo] + ($numeros[$hi]-$numeros[$lo])*($pos-$lo))
}
function Fmt([double]$v) {
    return $v.ToString('F2', [System.Globalization.CultureInfo]::InvariantCulture)
}

$proxy = Get-Proxy
if ($proxy.enabled -ne $true) { throw 'El proxy debe comenzar habilitado.' }
Confirmar-HTTP '200'
Comprobar-Seed

$marca = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss-fff') + 'Z'
$lote = Join-Path $repo "bench/e3/evidencias/fallas/$marca"
if (Test-Path -LiteralPath $lote) { throw "No sobrescribir evidencia: $lote" }
New-Item -ItemType Directory -Path $lote | Out-Null
$bench = (Resolve-Path 'bench/e3').Path
$montajeScripts = "${bench}:/scripts:ro"
$montajeResultados = "${lote}:/results"
$gitHash = (git rev-parse HEAD).Trim()
$hashJs = (Get-FileHash -LiteralPath 'bench/e3/fallas.js' -Algorithm SHA256).Hash
$manifesto = @(
    "git=$gitHash",
    "sha256_fallas_js=$hashJs",
    'image_k6=grafana/k6:1.8.2',
    'arquitectura=B',
    "rondas=$Rondas",
    "duracion_fase=${DuracionSegundos}s",
    "calentamiento_excluido=${CalentamientoSegundos}s",
    "rate_por_ruta=${RatePorRuta}/s",
    'orden_por_ronda=antes,falla,despues',
    'falla_inyectada=deshabilitar_proxy_reviews',
    'falla_http_resenas_esperado=503',
    'publicaciones_durante_falla_esperado=200',
    'postgres_compartido=si',
    'sin_reiniciar_db=si'
)
[System.IO.File]::WriteAllLines((Join-Path $lote 'manifest.txt'), [string[]]$manifesto,
    [System.Text.UTF8Encoding]::new($false))

function Medir([string]$nombre, [string]$fase, [int]$segundos, [bool]$guardar) {
    $argsDocker = @(
        'run','--rm','--network','umss-e3_default',
        '-e','E3_BASE_URL=http://api-b:8080',
        '-e',"E3_PHASE=$fase",
        '-e',"E3_RATE=$RatePorRuta",
        '-e',"E3_DURATION=${segundos}s",
        '-v',$montajeScripts,
        '-v',$montajeResultados,
        'grafana/k6:1.8.2','run','--summary-mode=full'
    )
    if ($guardar) {
        foreach ($s in @('.json','.raw.json','.log')) {
            if (Test-Path -LiteralPath (Join-Path $lote "$nombre$s")) {
                throw "No sobrescribir evidencia: $nombre$s"
            }
        }
        $argsDocker += @('--summary-export',"/results/$nombre.json",'--out',"json=/results/$nombre.raw.json")
    }
    $argsDocker += '/scripts/fallas.js'
    $log = Join-Path $lote "$nombre.log"
    & docker @argsDocker 2>&1 | Tee-Object -FilePath $log
    $salida = $LASTEXITCODE
    if ($salida -ne 0) { throw "k6 fallo en $nombre (salida=$salida)." }
    if ($guardar) {
        foreach ($s in @('.json','.raw.json','.log')) {
            $f = Join-Path $lote "$nombre$s"
            if (-not (Test-Path -LiteralPath $f) -or (Get-Item -LiteralPath $f).Length -eq 0) {
                throw "Evidencia incompleta: $f"
            }
        }
    }
}

function Extraer-Metricas([string]$nombre, [string]$fase) {
    $res = Get-Content -LiteralPath (Join-Path $lote "$nombre.json") -Raw | ConvertFrom-Json
    if ($fase -ne 'falla' -and [double]$res.metrics.checks.value -ne 1) {
        throw "$nombre contiene comprobaciones fallidas en una fase sana."
    }
    if ($fase -eq 'falla' -and [double]$res.metrics.'checks{ruta:resenas}'.value -ne 1) {
        throw "$nombre no devolvio HTTP 503 en todas las consultas de resenas."
    }
    $descartadas = 0
    if ($null -ne $res.metrics.PSObject.Properties['dropped_iterations']) {
        $descartadas = [int]$res.metrics.dropped_iterations.count
    }
    if ($descartadas -ne 0) { throw "$nombre descarto iteraciones." }
    $valores = @{
        publicacion = [System.Collections.Generic.List[double]]::new()
        resenas = [System.Collections.Generic.List[double]]::new()
    }
    $errores = @{publicacion=0;resenas=0}
    $esperadoResenas = if ($fase -eq 'falla') { '503' } else { '200' }
    $esperado = @{publicacion='200';resenas=$esperadoResenas}
    foreach ($linea in [System.IO.File]::ReadLines((Join-Path $lote "$nombre.raw.json"))) {
        if ([string]::IsNullOrWhiteSpace($linea)) { continue }
        $punto = $linea | ConvertFrom-Json
        if ($punto.type -ne 'Point' -or $punto.metric -ne 'http_req_duration') { continue }
        $ruta = [string]$punto.data.tags.ruta
        if (-not $valores.ContainsKey($ruta)) { throw "Etiqueta ruta desconocida en $nombre : $ruta" }
        $valores[$ruta].Add([double]$punto.data.value)
        if ([string]$punto.data.tags.status -ne $esperado[$ruta]) { $errores[$ruta]++ }
    }
    $nP = $valores.publicacion.Count
    $nR = $valores.resenas.Count
    if ($nP -lt 50 -or $nR -lt 50) { throw "$nombre tiene pocas muestras ($nP/$nR)." }
    if (($nP+$nR) -ne [int]$res.metrics.http_reqs.count) {
        throw "$nombre no coincide entre el JSON raw y el resumen de k6."
    }
    if ($errores.resenas -ne 0 -or ($fase -ne 'falla' -and $errores.publicacion -ne 0)) {
        throw "$nombre tuvo estados inesperados: publicaciones=$($errores.publicacion), resenas=$($errores.resenas)."
    }
    return [pscustomobject]@{
        corrida=$nombre;fase=$fase;solicitudes=($nP+$nR)
        publicaciones=$nP;resenas=$nR
        erroresPublicaciones=$errores.publicacion;erroresResenasInesperados=$errores.resenas
        checks=[double]$res.metrics.checks.value;droppedIterations=$descartadas
        p95PublicacionMs=[Math]::Round((Percentil ($valores.publicacion.ToArray()) 0.95),3)
        p95ResenasMs=[Math]::Round((Percentil ($valores.resenas.ToArray()) 0.95),3)
    }
}

$filas = [System.Collections.Generic.List[object]]::new()
Write-Host "`nLote H2/H3: $lote"
Write-Host 'Durante la falla: 503 en resenas es ESPERADO; publicaciones deben seguir en 200.'
for ($i=1; $i -le $Rondas; $i++) {
    $sufijo = '{0:D2}' -f $i
    Write-Host "`n=== RONDA $i/${Rondas}: CALENTAMIENTO ==="
    Confirmar-HTTP '200'
    Medir "warmup-$sufijo" 'antes' $CalentamientoSegundos $false

    Write-Host "`n=== RONDA ${i}: ANTES ==="
    $nombre = "r${sufijo}-antes"
    Medir $nombre 'antes' $DuracionSegundos $true
    $filas.Add((Extraer-Metricas $nombre 'antes'))

    try {
        Write-Host "`n=== RONDA ${i}: FALLA CONTROLADA ==="
        Set-Proxy $false
        Start-Sleep -Seconds 1
        Confirmar-HTTP '503'
        $nombre = "r${sufijo}-falla"
        Medir $nombre 'falla' $DuracionSegundos $true
        $filas.Add((Extraer-Metricas $nombre 'falla'))
    }
    finally {
        Write-Host 'Restaurando exclusivamente el proxy reviews...'
        Set-Proxy $true
        Write-Host 'Toxiproxy habilitado.'
    }

    Write-Host "`n=== RONDA ${i}: DESPUES (SIN CALENTAMIENTO ADICIONAL) ==="
    Start-Sleep -Seconds 1
    Confirmar-HTTP '200'
    $nombre = "r${sufijo}-despues"
    Medir $nombre 'despues' $DuracionSegundos $true
    $filas.Add((Extraer-Metricas $nombre 'despues'))
    Comprobar-Seed
    Write-Host "RONDA $i TERMINADA"
}

$csv = Join-Path $lote 'resumen-fases.csv'
$md = Join-Path $lote 'resumen-h2-h3.md'
if ((Test-Path -LiteralPath $csv) -or (Test-Path -LiteralPath $md)) {
    throw 'No se sobrescribiran resumenes ya existentes.'
}
$filas | Export-Csv -LiteralPath $csv -NoTypeInformation -Encoding UTF8

$doc = [System.Collections.Generic.List[string]]::new()
$doc.Add('# E3 - Aislamiento bajo carga (H2) y recuperacion (H3)')
$doc.Add('')
$doc.Add("Lote: ``$marca``. Arquitectura B, tasa $RatePorRuta solicitudes/s por ruta, $DuracionSegundos s por ventana.")
$doc.Add('')
$doc.Add('En la ventana de falla el HTTP 503 de resenas es intencional y se comprueba como comportamiento esperado.')
$doc.Add('')
$doc.Add('| Ronda | Falla: publicaciones incorrectas / total | Antes p95 pub. | Despues p95 pub. | Ratio pub. | Antes p95 resenas | Despues p95 resenas | Ratio resenas |')
$doc.Add('|---|---:|---:|---:|---:|---:|---:|---:|')
$fallosPub = 0
$totalPub = 0
$ratiosPub = [System.Collections.Generic.List[double]]::new()
$ratiosResenas = [System.Collections.Generic.List[double]]::new()
for ($i=1; $i -le $Rondas; $i++) {
    $sufijo = '{0:D2}' -f $i
    $antes = @($filas | Where-Object { $_.corrida -eq "r${sufijo}-antes" })[0]
    $falla = @($filas | Where-Object { $_.corrida -eq "r${sufijo}-falla" })[0]
    $despues = @($filas | Where-Object { $_.corrida -eq "r${sufijo}-despues" })[0]
    $fallosPub += [int]$falla.erroresPublicaciones
    $totalPub += [int]$falla.publicaciones
    $rp = [double]$despues.p95PublicacionMs / [double]$antes.p95PublicacionMs
    $rr = [double]$despues.p95ResenasMs / [double]$antes.p95ResenasMs
    $ratiosPub.Add($rp)
    $ratiosResenas.Add($rr)
    $doc.Add("| $i | $($falla.erroresPublicaciones) / $($falla.publicaciones) | $(Fmt $antes.p95PublicacionMs) | $(Fmt $despues.p95PublicacionMs) | $(Fmt (100*$rp))% | $(Fmt $antes.p95ResenasMs) | $(Fmt $despues.p95ResenasMs) | $(Fmt (100*$rr))% |")
}
$h2porcentaje = if ($totalPub -eq 0) { 100.0 } else { 100.0*$fallosPub/$totalPub }
$medRP = Percentil ($ratiosPub.ToArray()) 0.5
$medRR = Percentil ($ratiosResenas.ToArray()) 0.5
$doc.Add('')
$doc.Add('## H2: publicaciones durante la falla')
$doc.Add("- Errores inesperados en publicaciones: $fallosPub de $totalPub solicitudes, **$(Fmt $h2porcentaje)%**.")
$doc.Add('- Umbral: porcentaje de errores en publicaciones menor al 1%.')
$doc.Add("- Resultado para el lote: $(if ($h2porcentaje -lt 1) {'CUMPLE'} else {'NO CUMPLE'}).")
$doc.Add('- El total HTTP de k6 considera fallidos los 503 de resenas: son esperados, no deben contabilizarse como fallas de publicaciones.')
$doc.Add('')
$doc.Add('## H3: p95 posterior frente al anterior')
$doc.Add("- Mediana de ratios posteriores/anteriores de publicaciones: **$(Fmt (100*$medRP))%**.")
$doc.Add("- Mediana de ratios posteriores/anteriores de resenas: **$(Fmt (100*$medRR))%**.")
$doc.Add('- Umbral de referencia: p95 posterior no superior a 120% del previo. Revisar cada ronda, no solo la mediana.')
$doc.Add('- Las ventanas son sucesivas pero cada fase se ejecuta como una instancia nueva de k6; no se ha medido una transicion en una sola sesion HTTP.')
$doc.Add('- Las mediciones usan PostgreSQL E3 compartido y el mismo equipo local; no extrapolar a produccion.')
[System.IO.File]::WriteAllLines($md, [string[]]$doc, [System.Text.UTF8Encoding]::new($false))
Write-Host "`nLISTO: $($Rondas*3) ventanas medidas."
Write-Host "H2: $fallosPub / $totalPub fallos en publicaciones durante la falla ($(Fmt $h2porcentaje)%)."
Write-Host "H3: mediana ratio p95 publicaciones=$(Fmt (100*$medRP))%; resenas=$(Fmt (100*$medRR))%."
Write-Host "CSV: $csv"
Write-Host "Informe: $md"
Write-Host "Proxy habilitado al finalizar: $((Get-Proxy).enabled)"
