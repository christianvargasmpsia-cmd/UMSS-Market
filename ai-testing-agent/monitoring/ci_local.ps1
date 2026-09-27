param(
    [string]$Archivo = ".\data\normal.jsonl"
)

Write-Host "========================================"
Write-Host "CI LOCAL - MONITOREO IA"
Write-Host "========================================"

Write-Host ""
Write-Host "[1/2] Ejecutando tests..."
python -m pytest .\tests

if ($LASTEXITCODE -ne 0) {
    Write-Host "TESTS FALLARON"
    exit 1
}

Write-Host ""
Write-Host "[2/2] Ejecutando Quality Gate..."
python .\src\monitor.py $Archivo

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "QUALITY GATE: FALLÓ"
    exit 1
}

Write-Host ""
Write-Host "QUALITY GATE: PASS"
exit 0
