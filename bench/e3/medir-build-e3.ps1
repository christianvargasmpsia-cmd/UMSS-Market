# E3 - Medición de build de la imagen compartida por A y B
# Ejecutar desde la raíz del repositorio en PowerShell 5.1+.
# No elimina contenedores, volúmenes ni evidencias previas.
[CmdletBinding()]
param(
    [ValidateRange(1,10)][int]$Rondas = 5,
    [string]$Dockerfile = "backend/umss-market-api/Dockerfile",
    [string]$Contexto = "backend/umss-market-api",
    [string]$Etiqueta = "umss-e3-build:local"
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Evita que PowerShell 7 trate stderr de comandos nativos
# como errores terminantes. Docker BuildKit usa stderr
# para mostrar información de progreso.
if ($PSVersionTable.PSVersion.Major -ge 7) {
    $PSNativeCommandUseErrorActionPreference = $false
}
$raiz = (Get-Location).Path
$df = Join-Path $raiz $Dockerfile
$ctx = Join-Path $raiz $Contexto
if (-not (Test-Path -LiteralPath $df -PathType Leaf)) { throw "No existe Dockerfile: $df" }
if (-not (Test-Path -LiteralPath $ctx -PathType Container)) { throw "No existe contexto: $ctx" }
$docker = & docker version --format '{{.Server.Version}}' 2>&1
if ($LASTEXITCODE -ne 0 -or -not $docker) { throw "Docker no disponible: $docker" }

$utc = [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmss-fffZ")
$outDir = Join-Path $raiz "bench/e3/evidencias/build/$utc"
New-Item -ItemType Directory -Path $outDir -Force:$false | Out-Null
$csv = Join-Path $outDir "build.csv"
$logDir = Join-Path $outDir "logs"
New-Item -ItemType Directory -Path $logDir | Out-Null
$rows = [System.Collections.Generic.List[object]]::new()

# A y B usan la MISMA imagen y Dockerfile en el Compose E3.
# Por ello se mide el build de la imagen compartida, no se atribuye una diferencia
# artificial a cada arquitectura. Se etiqueta el resultado como build compartido.
for ($i=1; $i -le $Rondas; $i++) {
    $log = Join-Path $logDir ("build-{0:D2}.log" -f $i)
    $sw = [Diagnostics.Stopwatch]::StartNew()
    # --no-cache aproxima build limpio de capas; no borra imágenes/volúmenes existentes.
    # Ejecutar Docker Build usando cmd.exe para capturar
# correctamente stdout y stderr en Windows PowerShell 5.1

$dockerArgs = @(
    "build"
    "--no-cache"
    "--file"
    $df
    "--tag"
    $Etiqueta
    $ctx
)

$command = "docker " + ($dockerArgs -join " ")

cmd.exe /c "$command > `"$log`" 2>&1"

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker build fallo en la ronda $i"
    Get-Content $log
    exit 1
}
    $exitCode = $LASTEXITCODE
    $sw.Stop()
    if ($exitCode -ne 0) { throw "docker build falló en ronda $i. Revisar $log" }
    $rows.Add([pscustomobject]@{
        ronda = $i
        componente = "imagen_compartida_A_B"
        dockerfile = $Dockerfile
        contexto = $Contexto
        etiqueta = $Etiqueta
        cache = "no-cache"
        build_ms = [Math]::Round($sw.Elapsed.TotalMilliseconds,3)
        build_s = [Math]::Round($sw.Elapsed.TotalSeconds,3)
        docker_version = ([string]$docker).Trim()
        log = "logs/$(Split-Path $log -Leaf)"
    })
    Write-Host ("Build {0}/{1}: {2:N3} s" -f $i,$Rondas,$sw.Elapsed.TotalSeconds)
}
$rows | Export-Csv -LiteralPath $csv -NoTypeInformation -Encoding UTF8
$vals = @($rows | ForEach-Object { [double]$_.build_s } | Sort-Object)
$n=$vals.Count
if ($n%2) {$med=$vals[[int][Math]::Floor($n/2)]} else {$med=($vals[$n/2-1]+$vals[$n/2])/2}
$min=$vals[0]; $max=$vals[-1]
$md = @(
"# E3 - Build de imagen compartida",
"",
"Fecha UTC: $([DateTime]::UtcNow.ToString('o'))",
"",
"## Resultado",
"",
"- Imagen: ``$Etiqueta``",
"- Dockerfile: ``$Dockerfile``",
"- Contexto: ``$Contexto``",
"- Rondas: $Rondas",
"- Build con ``--no-cache``: mediana **$([Math]::Round($med,3)) s**, rango **$([Math]::Round($min,3))–$([Math]::Round($max,3)) s**.",
"- A y B reutilizan la misma imagen Docker y el mismo Dockerfile; por tanto, este resultado es **un build compartido**, no dos builds arquitectónicos diferentes.",
"- No se midió despliegue completo, arranque de PostgreSQL ni tiempo de disponibilidad funcional. H4 permanece separado.",
"",
"## Evidencia",
"",
"- ``build.csv`` contiene los tiempos por ronda.",
"- ``logs/`` contiene salida de Docker build por ronda.",
"",
"## Interpretación",
"",
"El build se repite sin caché de capas para reducir el efecto de builds incrementales. Los tiempos pueden variar por CPU, disco, red y dependencias descargadas. No comparar con otra máquina como si fuera una prueba controlada."
)
[IO.File]::WriteAllLines((Join-Path $outDir "RESUMEN-BUILD.md"),[string[]]$md,[Text.UTF8Encoding]::new($false))
Write-Host "Evidencias guardadas en: $outDir"
