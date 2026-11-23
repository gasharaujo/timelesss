param(
    [ValidateSet('20.1', '21', '26.1.2', '26.3')]
    [string[]]$Versions = @('20.1', '21', '26.1.2', '26.3'),
    [string]$InstancesRoot = "$env:USERPROFILE\curseforge\minecraft\Instances",
    [string]$Jdk25 = 'C:\Program Files\Java\jdk-25.0.2',
    [switch]$WithIntegrations,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJava = $env:JAVA_HOME
$projects = @{ '20.1' = 'forge-1.20.1'; '21' = 'neoforge-1.21.1'; '26.1.2' = 'neoforge-26.1.2'; '26.3' = 'neoforge-26.3' }
$ports = @{ '20.1' = 25585; '21' = 25586; '26.1.2' = 25587; '26.3' = 25588 }

try {
    foreach ($version in $Versions) {
        $projectPath = Join-Path $projectRoot $projects[$version]
        $instancePath = Join-Path $InstancesRoot $version
        if (-not (Test-Path -LiteralPath $instancePath)) { throw "Instância ausente: $instancePath" }
        $modVersion = ((Select-String -LiteralPath (Join-Path $projectPath 'gradle.properties') -Pattern '^mod_version=').Line -split '=', 2)[1]
        $testFolder = ".timelesss-$modVersion-tests"
        if ($WithIntegrations) { $testFolder += '-integrations' }
        $testPath = Join-Path $instancePath $testFolder
        New-Item -ItemType Directory -Force -Path $testPath | Out-Null
        Set-Content -LiteralPath (Join-Path $testPath 'eula.txt') -Value 'eula=true' -Encoding ascii
        Set-Content -LiteralPath (Join-Path $testPath 'options.txt') -Value 'soundCategory_master:0.0' -Encoding ascii
        if (-not (Test-Path -LiteralPath (Join-Path $testPath 'server.properties'))) {
            @"
online-mode=false
server-ip=127.0.0.1
server-port=$($ports[$version])
level-name=world
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
view-distance=2
simulation-distance=2
max-players=1
"@ | Set-Content -LiteralPath (Join-Path $testPath 'server.properties') -Encoding ascii
        }

        if ($WithIntegrations) {
            # Use the installed optional API; no dependency is added to release JARs.
            $mekanism = Get-ChildItem -LiteralPath (Join-Path $instancePath 'mods') -Filter 'Mekanism-*.jar' | Select-Object -First 1
            if ($mekanism -and $version -eq '20.1' -and $mekanism.Name -eq 'Mekanism-1.20.1-10.4.16.80.jar') {
                Copy-Item -LiteralPath $mekanism.FullName -Destination (Join-Path $projectPath 'test-mods\Mekanism-1.20.1-10.4.16.80.jar') -Force
            } elseif ($mekanism -and $version -eq '21') {
                New-Item -ItemType Directory -Force -Path (Join-Path $testPath 'mods') | Out-Null
                Copy-Item -LiteralPath $mekanism.FullName -Destination (Join-Path $testPath "mods\$($mekanism.Name)") -Force
            }

            $configured = Get-ChildItem -LiteralPath (Join-Path $instancePath 'mods') -Filter 'configured-*.jar' | Select-Object -First 1
            if ($configured -and $version -eq '20.1' -and $configured.Name -eq 'configured-forge-1.20.1-2.2.3.jar') {
                Copy-Item -LiteralPath $configured.FullName -Destination (Join-Path $projectPath "test-mods\$($configured.Name)") -Force
            } elseif ($configured -and $version -ne '20.1') {
                New-Item -ItemType Directory -Force -Path (Join-Path $testPath 'mods') | Out-Null
                Copy-Item -LiteralPath $configured.FullName -Destination (Join-Path $testPath "mods\$($configured.Name)") -Force
            }
        }

        $env:JAVA_HOME = $previousJava
        if ($version -in @('26.1.2', '26.3') -and (Test-Path -LiteralPath $Jdk25)) { $env:JAVA_HOME = $Jdk25 }
        $gradleArgs = @('runServer', '-PtimelesssSelfTest', "-PtimelesssTestDir=$testPath", '--console=plain', '--max-workers=2')
        if (-not $WithIntegrations) { $gradleArgs += '-PtimelesssWithoutIntegrations' }
        if ($Offline) { $gradleArgs += '--offline' }
        if ($version -ne '20.1') { $gradleArgs += '--no-problems-report' }
        $logName = ".test-$($projects[$version])-$modVersion"
        if ($WithIntegrations) { $logName += '-integrations' }
        $logPath = Join-Path $projectRoot "$logName.log"
        Push-Location $projectPath
        try {
            & .\gradlew.bat @gradleArgs *> $logPath
            if ($LASTEXITCODE -ne 0) { throw "Gradle falhou: $logPath" }
        } finally { Pop-Location }
        if (-not (Select-String -LiteralPath $logPath -Pattern 'TIMELESSS_SELFTEST_PASSED' -Quiet)) {
            throw "Teste falhou: $logPath"
        }
        Select-String -LiteralPath $logPath -Pattern 'TIMELESSS_SELFTEST_PASSED|TIMELESSS_MULTI_SELFTEST_PASSED|TIMELESSS_ADVANCEMENTS_SELFTEST_PASSED|CONFIGURED_MODEL_TEST_PASSED' | ForEach-Object { "$version`: $($_.Line)" }
    }
} finally { $env:JAVA_HOME = $previousJava }
