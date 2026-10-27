# Ejecucion de pruebas de Demo 2 con evidencia por suite.
# Para pruebas de PostgreSQL real se requiere una ejecucion separada; estas usan H2.
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backend = Join-Path $repo "backend/umss-market-api"
$evidence = Join-Path $repo "docs/demos/evidencia-local"
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$suites = @("JpaPublicationAtomicReservationTest", "TransactionalOutboxTest", "InventoryAlertsIntegrationTest", "OpenApiContractExportTest")
$failed = @()
Push-Location $backend
try {
    foreach ($suite in $suites) {
        Write-Host "Ejecutando $suite..."
        # cmd.exe une stderr con stdout antes de enviarlo a PowerShell.
        # Asi, los avisos de Mockito/JVM no se convierten en NativeCommandError
        # que aborta el script con $ErrorActionPreference = "Stop".
        $command = "call .\mvnw.cmd -Dtest=$suite test 2>&1"
        & cmd.exe /d /c $command | Tee-Object -FilePath (Join-Path $evidence "$suite.log")
        $code = $LASTEXITCODE
        if ($code -ne 0) {
            $failed += $suite
            Write-Warning "Falló $suite (código de salida: $code). Revisar el log."
        }
    }
} finally { Pop-Location }
if ($failed.Count -gt 0) {
    Write-Error ("Suites fallidas: " + ($failed -join ", ") + ". Ver logs en docs/demos/evidencia-local")
    exit 1
}
Write-Host "Todas las suites seleccionadas pasaron. Adjunte capturas y resultados de CI."
