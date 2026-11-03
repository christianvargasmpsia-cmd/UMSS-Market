param(
    [ValidateRange(1, 20)][int]$Rondas = 5,
    [ValidateRange(10, 600)][int]$DuracionSegundos = 30,
    [ValidateRange(5, 120)][int]$CalentamientoSegundos = 15,
    [ValidateRange(1, 100)][int]$RatePorRuta = 5
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repo

if ((git branch --show-current).Trim() -ne 'feature/e3-extraccion-resenas') {
    throw 'Rama incorrecta. Se requiere feature/e3-extraccion-resenas.'
}

$necesarios = @(
    'bench/e3/.env',
    'bench/e3/compose.yaml',
    'bench/e3/seed.sql',
    'bench/e3/carga.js'
)
foreach ($f in $necesarios) {
    if (-not (Test-Path -LiteralPath $f)) { throw "Falta archivo: $f" }
}

$dcA = @('--env-file', 'bench/e3/.env', '-f', 'bench/e3/compose.yaml', '-p', 'umss-e3', '--profile', 'a')
$dcB = @('--env-file', 'bench/e3/.env', '-f', 'bench/e3/compose.yaml', '-p', 'umss-e3', '--profile', 'b')
$network = 'umss-e3_default'
$id = '22222222-2222-4222-8222-222222222222'
$agente = 'umss-e3-client/1.0'

# Verificaciones previas antes de crear carpetas de evidencias.
# El docker inspect original podia fallar sin explicar si faltaba la imagen
# o si Docker Desktop/contexto no estaba disponible.
$dockerVersion = & docker version --format '{{.Server.Version}}' 2>&1
if ($LASTEXITCODE -ne 0 -or -not $dockerVersion) {
    throw "No responde el motor Docker. Revisar Docker Desktop y docker context show. Detalle: $dockerVersion"
}
Write-Host "Docker disponible: $dockerVersion"

$imagenApi = & docker image inspect --format '{{.Id}}' umss-e3-benchmark:local 2>$null
if ($LASTEXITCODE -ne 0 -or -not $imagenApi) {
    throw 'No se encontro umss-e3-benchmark:local en el contexto Docker activo. No se modifica la imagen de API.'
}

$imagenK6 = & docker image inspect --format '{{.Id}}' grafana/k6:1.8.2 2>$null
if ($LASTEXITCODE -ne 0 -or -not $imagenK6) {
    Write-Host 'k6 1.8.2 no se encuentra en el contexto actual. Intentando descargar la imagen...'
    & docker pull grafana/k6:1.8.2
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo descargar grafana/k6:1.8.2; revisar Docker Desktop, contexto y conexion.'
    }
    $imagenK6 = & docker image inspect --format '{{.Id}}' grafana/k6:1.8.2 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $imagenK6) {
        throw 'La imagen k6 sigue sin estar disponible tras docker pull. Ejecutar docker image inspect manualmente.'
    }
}
Write-Host "Imagen API verificada: $imagenApi"
Write-Host "Imagen k6 verificada: $imagenK6"

$marca = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss-fff') + 'Z'
$lote = Join-Path $repo "bench/e3/evidencias/formales/$marca"
if (Test-Path -LiteralPath $lote) { throw "Carpeta ya existente: $lote" }
New-Item -ItemType Directory -Path $lote -Force | Out-Null

$bench = (Resolve-Path 'bench/e3').Path
$montajeScripts = "${bench}:/scripts:ro"
$montajeResultados = "${lote}:/results"
$gitHash = (git rev-parse HEAD).Trim()

$manifiesto = @(
    "git=$gitHash",
    "image_api=$imagenApi",
    "image_k6=$imagenK6",
    'db=postgres:16-alpine (proyecto umss-e3)',
    "rondas=$Rondas",
    "duracion_medida=${DuracionSegundos}s",
    "calentamiento_excluido=${CalentamientoSegundos}s",
    "rate_por_ruta=${RatePorRuta}/s",
    'orden=A1 B1 A2 B2 ...',
    'misma_base_datos=si (solo consultas GET)',
    'toxiproxy_sin_toxicos=esperado'
)
foreach ($f in @('bench/e3/compose.yaml','bench/e3/carga.js','bench/e3/seed.sql')) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $f).Hash
    $manifiesto += "sha256_$($f.Replace('/','_'))=$hash"
}
$manifiesto | Set-Content -LiteralPath (Join-Path $lote 'manifest.txt') -Encoding UTF8

function Comprobar-Seed {
    $sql = 'SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM publications), (SELECT COUNT(*) FROM product_reviews);'
    $conteos = docker compose @dcA exec -T db psql -X -U e3 -d e3_umss -At -c $sql
    if ($LASTEXITCODE -ne 0 -or "$conteos".Trim() -ne '1|1|100') {
        throw "Dataset E3 alterado, conteos=$conteos. No se borrara ni restaurara nada."
    }
}

function Preparar-Proxy {
    $url = 'http://127.0.0.1:8474/proxies'
    $listado = $null
    for ($n = 1; $n -le 30; $n++) {
        try {
            $listado = Invoke-RestMethod -Uri $url -Method Get -UserAgent $agente
            break
        }
        catch { Start-Sleep -Seconds 1 }
    }
    if ($null -eq $listado) { throw 'No responde la API de Toxiproxy.' }

    if ($listado.PSObject.Properties.Name -contains 'reviews') {
        $p = $listado.reviews
        if ($p.upstream -ne 'reviews-b:8081') { throw 'Destino inesperado del proxy reviews.' }
        if ($null -ne $p.toxics -and @($p.toxics.PSObject.Properties).Count -gt 0) {
            throw 'Toxiproxy contiene toxics. No ejecutar prueba de rendimiento normal.'
        }
        if ($p.enabled -ne $true) {
            $p = Invoke-RestMethod -Uri "$url/reviews" -Method Post -UserAgent $agente `
                -ContentType 'application/json' -Body '{"enabled":true}'
        }
    }
    else {
        $body = @{name='reviews';listen='0.0.0.0:8666';upstream='reviews-b:8081';enabled=$true} | ConvertTo-Json -Compress
        $p = Invoke-RestMethod -Uri $url -Method Post -UserAgent $agente `
            -ContentType 'application/json' -Body $body
    }
    if ($p.enabled -ne $true -or $p.upstream -ne 'reviews-b:8081') {
        throw 'No se pudo confirmar Toxiproxy habilitado.'
    }
}

function Esperar-API([string]$modo) {
    $base = 'http://127.0.0.1:18080/api/publications/' + $id
    for ($n = 1; $n -le 60; $n++) {
        $p = curl.exe --silent --max-time 2 --output NUL --write-out '%{http_code}' $base
        $okP = ($LASTEXITCODE -eq 0 -and "$p".Trim() -eq '200')
        if ($okP) {
            $r = curl.exe --silent --max-time 2 --output NUL --write-out '%{http_code}' "$base/reviews?limit=20"
            if ($LASTEXITCODE -eq 0 -and "$r".Trim() -eq '200') { return }
        }
        Start-Sleep -Seconds 1
    }
    throw "La arquitectura $modo no responde HTTP 200 en ambas rutas."
}

function Cambiar-Arquitectura([string]$modo) {
    if ($modo -eq 'a') {
        docker compose @dcB stop api-b reviews-b toxiproxy | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al detener B.' }
        docker compose @dcA stop api-a | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al detener A.' }
        docker compose @dcA up -d db api-a | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al arrancar A.' }
    }
    else {
        docker compose @dcA stop api-a | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al detener A.' }
        docker compose @dcB stop api-b reviews-b toxiproxy | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al detener B.' }
        docker compose @dcB up -d db reviews-b toxiproxy | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al arrancar servicio y proxy.' }
        Preparar-Proxy
        docker compose @dcB up -d api-b | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Error al arrancar api-b.' }
    }
    Esperar-API $modo
    Comprobar-Seed
}

function Ejecutar-K6([string]$modo, [string]$nombre, [int]$duracion, [bool]$formal) {
    $baseUrl = "http://api-${modo}:8080"
    $argsDocker = @(
        'run','--rm','--network',$network,
        '-e',"E3_BASE_URL=$baseUrl",
        '-e',"E3_RATE=$RatePorRuta",
        '-e',"E3_DURATION=${duracion}s",
        '-v',$montajeScripts,
        '-v',$montajeResultados,
        'grafana/k6:1.8.2','run','--summary-mode=full'
    )
    if ($formal) {
        $argsDocker += @(
            '--summary-export',"/results/${nombre}.json",
            '--out',"json=/results/${nombre}.raw.json"
        )
    }
    $argsDocker += '/scripts/carga.js'
    $log = Join-Path $lote "${nombre}.log"
    if (Test-Path -LiteralPath $log) { throw "No se sobrescribira: $log" }
    & docker @argsDocker 2>&1 | Tee-Object -FilePath $log
    $codigo = $LASTEXITCODE
    if ($codigo -ne 0) { throw "k6 fallo en $nombre (exit=$codigo)." }
    if ($formal) {
        foreach ($sufijo in @('.log','.json','.raw.json')) {
            $file = Join-Path $lote "$nombre$sufijo"
            if (-not (Test-Path -LiteralPath $file) -or (Get-Item $file).Length -eq 0) {
                throw "Evidencia ausente/vacia: $file"
            }
        }
        $resumen = Get-Content -LiteralPath (Join-Path $lote "${nombre}.json") -Raw | ConvertFrom-Json
        if ($resumen.metrics.checks.value -lt 1 -or $resumen.metrics.http_req_failed.value -ne 0) {
            throw "La corrida $nombre tuvo fallas. Revisar logs; no se considera valida."
        }
        $dropped = 0
        if ($null -ne $resumen.metrics.PSObject.Properties['dropped_iterations']) {
            $dropped = [int]$resumen.metrics.dropped_iterations.count
        }
        if ($dropped -ne 0) { throw "La corrida $nombre descarto $dropped iteraciones." }
        $fila = [pscustomobject]@{
            corrida = $nombre
            arquitectura = $modo.ToUpperInvariant()
            solicitudes = $resumen.metrics.http_reqs.count
            checks = $resumen.metrics.checks.value
            errorRate = $resumen.metrics.http_req_failed.value
            droppedIterations = $dropped
            p95GlobalMs = $resumen.metrics.http_req_duration.'p(95)'
        }
        $csv = Join-Path $lote 'resumen-global.csv'
        $fila | Export-Csv -LiteralPath $csv -NoTypeInformation -Append -Encoding UTF8
    }
}

Write-Host "LOTE E3: $lote"
Write-Host "Ejecuciones formales: $($Rondas * 2). Orden alternado A/B."
for ($vuelta = 1; $vuelta -le $Rondas; $vuelta++) {
    foreach ($modo in @('a','b')) {
        $nombre = '{0}-{1:D2}' -f $modo, $vuelta
        Write-Host "`n=== ${nombre}: ARRANQUE, VERIFICACION Y CALENTAMIENTO ==="
        Cambiar-Arquitectura $modo
        Ejecutar-K6 $modo "warmup-$nombre" $CalentamientoSegundos $false
        Write-Host "`n=== ${nombre}: MEDICION FORMAL ==="
        Ejecutar-K6 $modo $nombre $DuracionSegundos $true
        Comprobar-Seed
        Write-Host "=== ${nombre}: OK ==="
    }
}
Write-Host "`nCOMPLETADAS $($Rondas * 2) CORRIDAS FORMALES."
Write-Host "Evidencias: $lote"
Write-Host 'Siguiente paso: calcular p95 por ruta desde cada archivo .raw.json.'
