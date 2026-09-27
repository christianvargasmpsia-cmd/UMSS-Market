param(
    [string]$ArchivoNormal = ".\monitoring\data\normal.jsonl",
    [string]$ArchivoDegradado = ".\monitoring\data\degraded.jsonl"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "CI LOCAL - MONITOREO IA"
Write-Host "========================================"

Write-Host ""
Write-Host "[1/3] Tests del monitor"
python -m pytest .\monitoring\tests

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: los tests fallaron."
    exit 1
}

Write-Host ""
Write-Host "[2/3] Escenario NORMAL - debe dar Gate 0"
python .\monitoring\src\monitor.py $ArchivoNormal

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: el escenario normal no pasó."
    exit 1
}

Write-Host ""
Write-Host "[3/3] Escenario DEGRADADO - debe dar Gate 1"

python .\monitoring\src\monitor.py $ArchivoDegradado
$degradedExitCode = $LASTEXITCODE

if ($degradedExitCode -ne 1) {
    Write-Host "ERROR: el escenario degradado debía producir Gate 1."
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "CI LOCAL COMPLETADO CORRECTAMENTE"
Write-Host "========================================"
Write-Host "Tests: PASS"
Write-Host "Normal: Gate 0"
Write-Host "Degradado: Gate 1 esperado"
Write-Host "CI: PASS"
exit 0
