# UMSS Market - E3 / H4
# Mide tiempo de arranque funcional de la arquitectura A frente a B.
# Mantiene PostgreSQL E3 y Toxiproxy encendidos durante toda la medicion.
# No elimina datos, volumenes, imagenes, ni cambia la configuracion de Toxiproxy.
# Compatible con Windows PowerShell 5.1.
[CmdletBinding()]
param(
    [ValidateRange(1, 20)]
    [int]$Rondas = 1,

    [ValidateRange(20, 300)]
    [int]$TimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$raiz = (Get-Location).Path
$compose = Join-Path $raiz 'bench/e3/compose.yaml'
$envPath = Join-Path $raiz 'bench/e3/.env'
$directorio = Join-Path $raiz 'bench/e3/evidencias/arranque'
$nombreA = 'umss-e3-api-a-1'
$nombreApiB = 'umss-e3-api-b-1'
$nombreReviewsB = 'umss-e3-reviews-b-1'
$nombreDb = 'umss-e3-db-1'
$nombreProxy = 'umss-e3-toxiproxy-1'
$nombresApp = @($nombreA, $nombreApiB, $nombreReviewsB)
$idPublicacion = '22222222-2222-4222-8222-222222222222'
$urlPublicacion = "http://127.0.0.1:18080/api/publications/$idPublicacion"
$urlResenas = "$urlPublicacion/reviews?limit=20"
$cultura = [Globalization.CultureInfo]::InvariantCulture
$dc = @('--env-file', $envPath, '-f', $compose, '-p', 'umss-e3', '--profile', 'a', '--profile', 'b')

function Comprobar-Exit([string]$mensaje) {
    if ($LASTEXITCODE -ne 0) {
        throw $mensaje
    }
}

function Estado-Contenedor([string]$nombre) {
    $resultado = & docker inspect --format '{{.State.Running}}' $nombre 2>$null
    Comprobar-Exit "No se pudo inspeccionar el contenedor $nombre."
    return (([string]$resultado).Trim().ToLowerInvariant() -eq 'true')
}

function Detener-Aplicaciones {
    foreach ($nombre in $nombresApp) {
        if (Estado-Contenedor $nombre) {
            & docker stop -t 10 $nombre | Out-Null
            Comprobar-Exit "No se pudo detener $nombre."
        }
    }
    foreach ($nombre in $nombresApp) {
        if (Estado-Contenedor $nombre) {
            throw "El contenedor $nombre continua activo."
        }
    }
}

function Verificar-Seed {
    $consulta = 'SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM publications), (SELECT COUNT(*) FROM product_reviews);'
    $conteos = & docker compose @dc exec -T db psql -U e3 -d e3_umss -At -c $consulta
    Comprobar-Exit 'Fallo la consulta del seed E3.'
    if (([string]$conteos).Trim() -ne '1|1|100') {
        throw 'El seed E3 debe contener exactamente 1 usuario, 1 publicacion y 100 resenas.'
    }
}

function Verificar-Proxy {
    $p = Invoke-RestMethod -Uri 'http://127.0.0.1:8474/proxies/reviews' -Method Get -UserAgent 'umss-e3-client/1.0'
    if ($p.name -ne 'reviews' -or $p.enabled -ne $true -or $p.upstream -ne 'reviews-b:8081') {
        throw 'El proxy de resenas no coincide con la configuracion E3 o esta deshabilitado.'
    }
    $toxicJson = ConvertTo-Json -InputObject $p.toxics -Compress -Depth 6
    if ($toxicJson -notin @('{}', '[]', 'null')) {
        throw 'Hay toxics activos en el proxy; no se modificara su configuracion.'
    }
}

function Comprobar-Http([string]$url) {
    $codigo = & curl.exe --silent --max-time 1 --output NUL --write-out '%{http_code}' $url
    if ($LASTEXITCODE -ne 0) {
        return '000'
    }
    return ([string]$codigo).Trim()
}

function Arrancar-Y-Medir([string]$forma, [int]$numero) {
    Detener-Aplicaciones
    Start-Sleep -Milliseconds 400
    Verificar-Seed
    Verificar-Proxy

    # Mantener un arreglo real incluso cuando solo se inicia un contenedor.
    $objetivos = @()
    if ($forma -eq 'A') {
        $objetivos += $nombreA
    } else {
        $objetivos += $nombreReviewsB
        $objetivos += $nombreApiB
    }
    Write-Host ("Ronda {0} / {1}: arrancando {2}..." -f $numero, $Rondas, $forma)
    $reloj = [Diagnostics.Stopwatch]::StartNew()

    & docker start @objetivos | Out-Null
    Comprobar-Exit "Docker no pudo iniciar la arquitectura $forma."

    $codigoP = '000'
    $codigoR = '000'
    while ($reloj.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        $codigoP = Comprobar-Http $urlPublicacion
        $codigoR = Comprobar-Http $urlResenas
        if ($codigoP -eq '200' -and $codigoR -eq '200') {
            $reloj.Stop()
            $ms = [Math]::Round($reloj.Elapsed.TotalMilliseconds, 3)
            Write-Host ("{0}-{1:00}: listo en {2} ms (publicaciones 200, resenas 200)" -f $forma, $numero, $ms.ToString('0.000', $cultura))
            return [pscustomobject]@{
                ronda = $numero
                arquitectura = $forma
                arranqueMs = $ms.ToString('0.000', $cultura)
                publicacionesHttp = $codigoP
                resenasHttp = $codigoR
            }
        }
        Start-Sleep -Milliseconds 200
    }
    throw ("Tiempo agotado en arquitectura {0} tras {1} segundos. Ultimos HTTP: publicaciones={2}; resenas={3}." -f $forma, $TimeoutSeconds, $codigoP, $codigoR)
}

function Mediana([double[]]$valores) {
    $ordenados = @($valores | Sort-Object)
    if ($ordenados.Count -eq 0) { throw 'No hay muestras para la mediana.' }
    $centro = [int][Math]::Floor($ordenados.Count / 2)
    if ($ordenados.Count % 2 -eq 1) { return [double]$ordenados[$centro] }
    return ([double]$ordenados[$centro - 1] + [double]$ordenados[$centro]) / 2.0
}

if ((& git branch --show-current).Trim() -ne 'feature/e3-extraccion-resenas') {
    throw 'Rama incorrecta: utilizar feature/e3-extraccion-resenas.'
}
foreach ($ruta in @($compose, $envPath, (Join-Path $raiz 'bench/e3/seed.sql'))) {
    if (-not (Test-Path -LiteralPath $ruta)) {
        throw "Falta archivo requerido: $ruta"
    }
}
& docker info --format '{{.ServerVersion}}' | Out-Null
Comprobar-Exit 'Docker no esta accesible.'
foreach ($nombre in @($nombresApp + @($nombreDb, $nombreProxy))) {
    & docker inspect --format '{{.Id}}' $nombre | Out-Null
    Comprobar-Exit "Falta el contenedor ya creado: $nombre. No se recreara automaticamente."
}
if (-not (Estado-Contenedor $nombreDb) -or -not (Estado-Contenedor $nombreProxy)) {
    throw 'PostgreSQL E3 y Toxiproxy deben estar activos antes de medir.'
}
$saludDb = (& docker inspect --format '{{.State.Health.Status}}' $nombreDb).Trim()
Comprobar-Exit 'No se pudo consultar la salud de PostgreSQL.'
if ($saludDb -ne 'healthy') { throw 'PostgreSQL E3 no esta saludable.' }

$imagenes = @()
foreach ($nombre in $nombresApp) {
    $imagen = (& docker inspect --format '{{.Image}}' $nombre).Trim()
    Comprobar-Exit "No se pudo consultar la imagen de $nombre."
    $imagenes += $imagen
}
if (@($imagenes | Select-Object -Unique).Count -ne 1) {
    throw 'Las imagenes de las tres aplicaciones no coinciden.'
}
Verificar-Seed
Verificar-Proxy

$originalA = Estado-Contenedor $nombreA
$originalApiB = Estado-Contenedor $nombreApiB
$originalReviewsB = Estado-Contenedor $nombreReviewsB
if ($originalA -and $originalApiB) {
    throw 'A y B estan simultaneamente activas; se detiene por seguridad.'
}

$identificador = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fffZ') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
$lote = Join-Path $directorio $identificador
New-Item -ItemType Directory -Path $lote -Force:$false | Out-Null

$manifest = @(
    'experimento=E3-H4',
    "rondas=$Rondas",
    "fechaUTC=$([DateTime]::UtcNow.ToString('o'))",
    "imagen=$($imagenes[0])",
    'mecanismo=docker start contenedores preexistentes; hasta HTTP 200 simultaneo en dos rutas',
    'condicion=arranque de procesos Java con PostgreSQL E3 y Toxiproxy ya activos',
    'nota=el proxy Toxiproxy y la base de datos NO forman parte del tiempo medido',
    'nota2=las operaciones docker start son secuenciales para reviews-b y api-b',
    'nota3=sin build, sin recrear contenedores, sin eliminar volumenes ni reinsertar seed',
    'nota4=desviacion explicita de HIPOTESIS.md: NO se usa docker compose down -v para preservar el volumen E3',
    'nota5=poll HTTP cada 200ms con timeout de 1s por URL; contiene el overhead del comando Docker',
    "timeoutSegundos=$TimeoutSeconds"
)
[IO.File]::WriteAllLines((Join-Path $lote 'manifest.txt'), $manifest, [Text.UTF8Encoding]::new($false))
$resultados = New-Object 'System.Collections.Generic.List[object]'
$csv = Join-Path $lote 'tiempos-arranque.csv'

try {
    for ($n = 1; $n -le $Rondas; $n++) {
        $resultados.Add((Arrancar-Y-Medir 'A' $n))
        $resultados | Export-Csv -Path $csv -NoTypeInformation -Encoding UTF8
        $resultados.Add((Arrancar-Y-Medir 'B' $n))
        $resultados | Export-Csv -Path $csv -NoTypeInformation -Encoding UTF8
    }

    $a = @($resultados | Where-Object arquitectura -eq 'A' | ForEach-Object { [double]::Parse($_.arranqueMs, $cultura) })
    $b = @($resultados | Where-Object arquitectura -eq 'B' | ForEach-Object { [double]::Parse($_.arranqueMs, $cultura) })
    $medA = Mediana $a
    $medB = Mediana $b
    if ($medA -le 0) { throw 'La mediana A no puede ser cero.' }
    $ratio = $medB / $medA
    $porcentaje = 100 * ($ratio - 1)
    $lineas = @(
        '# E3 - H4: tiempo de arranque funcional',
        '',
        "Lote: ``$identificador``; $Rondas rondas A/B.",
        '',
        '| Ronda | A (ms) | B (ms) | B/A |',
        '|---|---:|---:|---:|'
    )
    for ($n = 1; $n -le $Rondas; $n++) {
        $i = $n - 1
        $lineas += ('| {0} | {1:N2} | {2:N2} | {3:P2} |' -f $n, $a[$i], $b[$i], ($b[$i] / $a[$i]))
    }
    $lineas += @(
        '',
        ('- Mediana de arranque A: **{0:N2} ms**.' -f $medA),
        ('- Mediana de arranque B: **{0:N2} ms**.' -f $medB),
        ('- Sobrecosto relativo de medianas: **{0:N2}%**.' -f $porcentaje),
        ('- H4: **{0}** usando medianas (criterio B/A <= 130%).' -f $(if ($ratio -le 1.3) {'CUMPLE'} else {'NO CUMPLE'})),
        '',
        ('- A, rango min-max: {0:N2}-{1:N2} ms.' -f ($a | Measure-Object -Minimum).Minimum, ($a | Measure-Object -Maximum).Maximum),
        ('- B, rango min-max: {0:N2}-{1:N2} ms.' -f ($b | Measure-Object -Minimum).Minimum, ($b | Measure-Object -Maximum).Maximum),
        '',
        '## Alcance y limitaciones',
        '- El reloj comienza justo antes de docker start y termina cuando ambas rutas responden HTTP 200.',
        '- Medicion funcional de un reinicio de los procesos de aplicacion en contenedores existentes.',
        '- PostgreSQL y Toxiproxy permanecen activos para ambas arquitecturas; no se cronometra su arranque.',
        '- Para B se inician secuencialmente reviews-b y api-b, respetando la misma operacion en cada ronda.',
        '- Se incluye el overhead de Docker CLI y del sondeo HTTP. La resolucion efectiva no es de microsegundos.',
        '- No equivale al tiempo de despliegue completo en produccion ni al tiempo de build.',
        '- HIPOTESIS.md proponia down -v entre corridas, pero aqui se preserva el volumen existente; documentar la desviacion.',
        '- Una diferencia desfavorable para B no invalida la ganancia demostrada en aislamiento H2; cambia la decision de trade-offs.'
    )
    $informe = Join-Path $lote 'resumen-h4.md'
    [IO.File]::WriteAllLines($informe, $lineas, [Text.UTF8Encoding]::new($false))

    Write-Host ('MEDICIONES H4 TERMINADAS: {0} parejas' -f $Rondas)
    Write-Host ('Mediana A: {0:N2} ms | Mediana B: {1:N2} ms | Sobrecosto: {2:N2}%' -f $medA, $medB, $porcentaje)
    Write-Host ('Resultado H4 por medianas: {0}' -f $(if ($ratio -le 1.3) {'CUMPLE'} else {'NO CUMPLE'}))
    Write-Host "CSV: $csv"
    Write-Host "Informe: $informe"
}
finally {
    # Se intenta restaurar que aplicaciones estaban encendidas antes de iniciar.
    Write-Host 'Restaurando estado de ejecucion inicial de A/B...'
    try {
        Detener-Aplicaciones
        $restaurar = @()
        if ($originalA) { $restaurar += $nombreA }
        if ($originalReviewsB) { $restaurar += $nombreReviewsB }
        if ($originalApiB) { $restaurar += $nombreApiB }
        if ($restaurar.Count -gt 0) {
            & docker start @restaurar | Out-Null
            Comprobar-Exit 'No se pudieron restaurar las aplicaciones activas inicialmente.'
        }
        Write-Host 'Estado de aplicaciones restaurado. PostgreSQL y Toxiproxy no se tocaron.'
    }
    catch {
        Write-Warning "La restauracion automatica necesita revision: $($_.Exception.Message)"
    }
}
