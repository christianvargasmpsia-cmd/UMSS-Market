param([string]$MavenCommand = '', [string]$MavenRepository = '')
$ErrorActionPreference = 'Stop'
$agentRoot = Split-Path $PSScriptRoot -Parent
$repoRoot = Split-Path $agentRoot -Parent
$backend = Join-Path $repoRoot 'backend/umss-market-api'
$attacks = Join-Path $agentRoot 'ataques'
$reportDir = Join-Path $agentRoot 'reports/redteam'
if (-not $MavenCommand) { $MavenCommand = Join-Path $backend 'mvnw.cmd' }
$mavenArgs = @('-Dtest=RedTeamSecurityTest,ChatInputPolicyTest,AIServiceImplTest',
    "-Dredteam.attacksPath=$attacks", 'test', 'jacoco:report')
if ($MavenRepository) { $mavenArgs += "-Dmaven.repo.local=$MavenRepository" }
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
$started = Get-Date
Push-Location $backend
try {
    Get-Command $MavenCommand -ErrorAction Stop | Out-Null
    # Windows PowerShell can turn harmless JVM stderr warnings into errors.
    # Maven's exit code remains the authoritative success criterion.
    $ErrorActionPreference = 'Continue'
    try {
        & $MavenCommand @mavenArgs
        $result = $LASTEXITCODE
    } finally { $ErrorActionPreference = 'Stop' }
    $report = Join-Path $backend 'target/redteam/despues.json'
    if ((Test-Path $report) -and (Get-Item $report).LastWriteTime -ge $started) {
        Copy-Item -LiteralPath $report -Destination (Join-Path $reportDir 'despues.json')
        $data = Get-Content $report -Raw | ConvertFrom-Json
        $data.resultados | Group-Object ataque | ForEach-Object {
            $success = @($_.Group | Where-Object exito_ataque).Count
            Write-Host "$($_.Name): $success/$($_.Count) ataques exitosos"
        }
    } elseif ($result -eq 0) { throw 'No se genero evidencia nueva' }
    if ($result -ne 0) { throw "Maven fallo con codigo $result; revisar la salida" }
} finally { Pop-Location }
