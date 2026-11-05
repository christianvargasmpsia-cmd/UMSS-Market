
$ErrorActionPreference = "Stop"

$BaseUrl = if ($env:BASE_URL) {
    $env:BASE_URL
} else {
    "http://host.docker.internal:8080"
}

$Rate = if ($env:RATE) {
    $env:RATE
} else {
    "10"
}

$Duration = if ($env:DURATION) {
    $env:DURATION
} else {
    "60s"
}

$OutDir = if ($env:OUT_DIR) {
    $env:OUT_DIR
} else {
    "bench/results"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

for ($run = 1; $run -le 3; $run++) {

    Write-Host ""
    Write-Host "=== k6 corrida $run/3 ==="
    Write-Host "URL: $BaseUrl"
    Write-Host "Rate: $Rate req/s"
    Write-Host "Duration: $Duration"

    docker run --rm `
        -v "${PWD}/bench:/scripts" `
        grafana/k6 run `
        -e BASE_URL=$BaseUrl `
        -e RATE=$Rate `
        -e DURATION=$Duration `
        --summary-export="/scripts/results/run$run.json" `
        /scripts/catalog.js

    if ($LASTEXITCODE -ne 0) {
        throw "La corrida $run de k6 falló."
    }
}

Write-Host ""
Write-Host "=== Generando resumen ==="

python bench/summarize.py $OutDir --output "$OutDir/summary.md"

if ($LASTEXITCODE -ne 0) {
    throw "No se pudo generar el resumen de k6."
}

Write-Host ""
Write-Host "Resumen generado: $OutDir/summary.md"
