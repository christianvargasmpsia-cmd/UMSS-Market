# Ejecutar desde cualquier carpeta. Requiere Java 21, conexion a Maven y Node.js.
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backend = Join-Path $repo "backend/umss-market-api"
Push-Location $backend
try {
    Write-Host "Generando contrato Springdoc desde Java..."
    & .\mvnw.cmd "-Dtest=OpenApiContractExportTest" test
    if ($LASTEXITCODE -ne 0) { throw "El exportador Maven fallo: $LASTEXITCODE" }
    $generated = Join-Path $backend "target/generated-openapi.json"
    if (-not (Test-Path $generated)) { throw "No se genero $generated" }
    Copy-Item $generated (Join-Path $repo "openapi.json") -Force
} finally { Pop-Location }
Push-Location $repo
try {
    Write-Host "Validando contrato con Spectral..."
    & npx.cmd --yes @stoplight/spectral-cli@6.17.0 lint openapi.json --ruleset .spectral.yaml --fail-severity error
    if ($LASTEXITCODE -ne 0) { throw "Spectral fallo: $LASTEXITCODE" }
    Write-Host "openapi.json generado y validado. Agreguelo a su commit (git add openapi.json)."
} finally { Pop-Location }
