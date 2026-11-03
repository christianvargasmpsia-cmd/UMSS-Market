param(
    [string]$Lote = ''
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ((git -C $repo branch --show-current).Trim() -ne 'feature/e3-extraccion-resenas') {
    throw 'Rama incorrecta: se requiere feature/e3-extraccion-resenas.'
}

$base = Join-Path $repo 'bench/e3/evidencias/formales'
if (-not (Test-Path -LiteralPath $base)) { throw 'No existe la carpeta de evidencias formales.' }

if ($Lote) {
    $directorio = (Resolve-Path -LiteralPath $Lote).Path
} else {
    $candidatos = @(
        Get-ChildItem -LiteralPath $base -Directory |
            Sort-Object Name -Descending |
            Where-Object {
                $manifest = Join-Path $_.FullName 'manifest.txt'
                (Test-Path -LiteralPath $manifest) -and
                [bool](Select-String -LiteralPath $manifest -Pattern '^rondas=5\s*$' -Quiet)
            }
    )
    if ($candidatos.Count -eq 0) { throw 'No se encontro un lote de cinco rondas. Indica -Lote con su ruta.' }
    $directorio = $candidatos[0].FullName
}

$manifesto = Join-Path $directorio 'manifest.txt'
if (-not (Test-Path -LiteralPath $manifesto) -or
    -not (Select-String -LiteralPath $manifesto -Pattern '^rondas=5\s*$' -Quiet)) {
    throw 'El lote seleccionado no declara exactamente cinco rondas.'
}

$csvSalida = Join-Path $directorio 'p95-por-ruta.csv'
$mdSalida = Join-Path $directorio 'resumen-h1.md'
foreach ($salida in @($csvSalida, $mdSalida)) {
    if (Test-Path -LiteralPath $salida) { throw "No se sobrescribira un informe existente: $salida" }
}

function Percentil([double[]]$valores, [double]$proporcion) {
    if ($valores.Length -eq 0) { throw 'No hay muestras para calcular un percentil.' }
    [Array]::Sort($valores)
    $pos = ($valores.Length - 1) * $proporcion
    $izq = [int][Math]::Floor($pos)
    $der = [int][Math]::Ceiling($pos)
    return [double]($valores[$izq] + ($valores[$der] - $valores[$izq]) * ($pos - $izq))
}

function Fmt([double]$valor) {
    return $valor.ToString('F2', [System.Globalization.CultureInfo]::InvariantCulture)
}

$filas = [System.Collections.Generic.List[object]]::new()
foreach ($numero in 1..5) {
    foreach ($modo in @('a', 'b')) {
        $nombre = '{0}-{1:D2}' -f $modo, $numero
        $raw = Join-Path $directorio "$nombre.raw.json"
        $res = Join-Path $directorio "$nombre.json"
        foreach ($archivo in @($raw, $res)) {
            if (-not (Test-Path -LiteralPath $archivo)) { throw "Falta evidencia: $archivo" }
        }

        $resumen = Get-Content -LiteralPath $res -Raw | ConvertFrom-Json
        if ($resumen.metrics.checks.value -ne 1 -or
            $resumen.metrics.http_req_failed.value -ne 0) {
            throw "La corrida $nombre no tiene checks=100% y errorRate=0%."
        }
        $descartadas = 0
        if ($null -ne $resumen.metrics.PSObject.Properties['dropped_iterations']) {
            $descartadas = [int]$resumen.metrics.dropped_iterations.count
        }
        if ($descartadas -ne 0) { throw "La corrida $nombre tiene iteraciones descartadas." }

        $muestras = @{
            publicacion = [System.Collections.Generic.List[double]]::new()
            resenas     = [System.Collections.Generic.List[double]]::new()
        }

        foreach ($linea in [System.IO.File]::ReadLines($raw)) {
            if ([string]::IsNullOrWhiteSpace($linea)) { continue }
            $punto = $linea | ConvertFrom-Json
            if ($punto.type -ne 'Point' -or $punto.metric -ne 'http_req_duration') { continue }
            $ruta = [string]$punto.data.tags.ruta
            if (-not $muestras.ContainsKey($ruta)) {
                throw "Etiqueta ruta inesperada en ${nombre}: '$ruta'."
            }
            $muestras[$ruta].Add([double]$punto.data.value)
        }

        $nPublicacion = $muestras.publicacion.Count
        $nResenas = $muestras.resenas.Count
        $nTotal = $nPublicacion + $nResenas
        if ($nPublicacion -lt 100 -or $nResenas -lt 100) {
            throw "Muestras insuficientes en ${nombre}: publicacion=$nPublicacion, resenas=$nResenas."
        }
        if ($nTotal -ne [int]$resumen.metrics.http_reqs.count) {
            throw "Muestras inconsistentes en ${nombre}: raw=$nTotal, summary=$($resumen.metrics.http_reqs.count)."
        }

        $p95P = Percentil ($muestras.publicacion.ToArray()) 0.95
        $p95R = Percentil ($muestras.resenas.ToArray()) 0.95
        $filas.Add([pscustomobject]@{
            corrida             = $nombre
            arquitectura        = $modo.ToUpperInvariant()
            solicitudes         = $nTotal
            checks              = $resumen.metrics.checks.value
            errorRate           = $resumen.metrics.http_req_failed.value
            droppedIterations   = $descartadas
            muestrasPublicacion = $nPublicacion
            muestrasResenas     = $nResenas
            p95PublicacionMs    = [Math]::Round($p95P, 3)
            p95ResenasMs        = [Math]::Round($p95R, 3)
        })
        Write-Host "$nombre OK: publicaciones p95=$(Fmt $p95P) ms; resenas p95=$(Fmt $p95R) ms"
    }
}

$A = @($filas | Where-Object { $_.arquitectura -eq 'A' } | Sort-Object corrida)
$B = @($filas | Where-Object { $_.arquitectura -eq 'B' } | Sort-Object corrida)
if ($A.Count -ne 5 -or $B.Count -ne 5) { throw 'No hay cinco corridas validas por arquitectura.' }

$aResenas = [double[]]@($A | ForEach-Object { $_.p95ResenasMs })
$bResenas = [double[]]@($B | ForEach-Object { $_.p95ResenasMs })
$aPublicaciones = [double[]]@($A | ForEach-Object { $_.p95PublicacionMs })
$bPublicaciones = [double[]]@($B | ForEach-Object { $_.p95PublicacionMs })
$medA = Percentil ([double[]]$aResenas.Clone()) 0.5
$medB = Percentil ([double[]]$bResenas.Clone()) 0.5
$deltaMedianas = $medB - $medA
$parejas = [System.Collections.Generic.List[double]]::new()
for ($i = 0; $i -lt 5; $i++) {
    $parejas.Add([double]($B[$i].p95ResenasMs - $A[$i].p95ResenasMs))
}
$medDeltaParejas = Percentil ($parejas.ToArray()) 0.5
$vecesDentro = @($parejas | Where-Object { $_ -le 15 }).Count

$filas | Export-Csv -LiteralPath $csvSalida -NoTypeInformation -Encoding UTF8

$documento = [System.Collections.Generic.List[string]]::new()
$documento.Add('# E3 - Analisis de cinco corridas formales por arquitectura')
$documento.Add('')
$documento.Add("Lote: ``$([IO.Path]::GetFileName($directorio))``. Se excluye el calentamiento y el lote piloto de una ronda.")
$documento.Add('')
$documento.Add('| Corrida | p95 pub. A (ms) | p95 pub. B (ms) | p95 resenas A (ms) | p95 resenas B (ms) | Delta resenas B-A (ms) |')
$documento.Add('|---|---:|---:|---:|---:|---:|')
for ($i = 0; $i -lt 5; $i++) {
    $documento.Add("| $($i + 1) | $(Fmt $A[$i].p95PublicacionMs) | $(Fmt $B[$i].p95PublicacionMs) | $(Fmt $A[$i].p95ResenasMs) | $(Fmt $B[$i].p95ResenasMs) | $(Fmt $parejas[$i]) |")
}
$documento.Add('')
$documento.Add('## Resumen de los p95 entre las cinco corridas')
$documento.Add('')
$documento.Add('| Ruta | Arquitectura | Mediana de p95 (ms) | Rango min-max (ms) |')
$documento.Add('|---|---|---:|---:|')
foreach ($entrada in @(
    @{ruta='Publicaciones'; modo='A'; datos=$aPublicaciones},
    @{ruta='Publicaciones'; modo='B'; datos=$bPublicaciones},
    @{ruta='Resenas'; modo='A'; datos=$aResenas},
    @{ruta='Resenas'; modo='B'; datos=$bResenas}
)) {
    $md = Percentil ([double[]]$entrada.datos.Clone()) 0.5
    $min = ($entrada.datos | Measure-Object -Minimum).Minimum
    $max = ($entrada.datos | Measure-Object -Maximum).Maximum
    $documento.Add("| $($entrada.ruta) | $($entrada.modo) | $(Fmt $md) | $(Fmt $min)-$(Fmt $max) |")
}
$documento.Add('')
$documento.Add('## H1: sobrecosto del p95 de resenas no superior a 15 ms')
$documento.Add('')
$documento.Add("- Diferencia de medianas de p95 (B - A): **$(Fmt $deltaMedianas) ms**.")
$documento.Add("- Mediana de diferencias por parejas A/B: **$(Fmt $medDeltaParejas) ms**.")
$documento.Add("- Parejas con diferencia <=15 ms: **$vecesDentro / 5**.")
$documento.Add('- H1 solo se refiere a condiciones sin fallas, a la tasa probada. No demuestra aislamiento, recuperacion ni tiempos de arranque.')
$documento.Add('- Cada p95 se calcula de muestras de la misma ruta y misma corrida; luego se resume entre las cinco corridas.')
$documento.Add('- Metodo para el percentil: interpolacion lineal entre posiciones ordenadas con indice (n-1)*0.95.')
$documento.Add('- Limitacion: las corridas A/B se ejecutan en la misma maquina y comparten el PostgreSQL E3; no se infiere comportamiento en produccion.')

[System.IO.File]::WriteAllLines($mdSalida, [string[]]$documento, [System.Text.UTF8Encoding]::new($false))

Write-Host ''
Write-Host "LOTE ANALIZADO: $directorio"
Write-Host "Mediana p95 resenas A: $(Fmt $medA) ms"
Write-Host "Mediana p95 resenas B: $(Fmt $medB) ms"
Write-Host "Sobrecosto (diferencia de medianas): $(Fmt $deltaMedianas) ms"
Write-Host "Parejas dentro del umbral H1: $vecesDentro / 5"
Write-Host "CSV: $csvSalida"
Write-Host "Informe: $mdSalida"
