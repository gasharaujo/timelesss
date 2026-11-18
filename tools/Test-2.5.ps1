param(
    [ValidateSet('20.1', '21', '26.1.2')]
    [string[]]$Versions = @('20.1', '21', '26.1.2'),
    [string]$InstancesRoot = "$env:USERPROFILE\curseforge\minecraft\Instances",
    [string]$Jdk25 = 'C:\Program Files\Java\jdk-25.0.2'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJava = $env:JAVA_HOME
$projects = @{ '20.1' = 'forge-1.20.1'; '21' = 'neoforge-1.21.1'; '26.1.2' = 'neoforge-26.1.2' }
$ports = @{ '20.1' = 25575; '21' = 25576; '26.1.2' = 25577 }

try {
    foreach ($version in $Versions) {
        $projectPath = Join-Path $projectRoot $projects[$version]
        $instancePath = Join-Path $InstancesRoot $version
        if (-not (Test-Path -LiteralPath $instancePath)) { throw "Instância ausente: $instancePath" }
        $testPath = Join-Path $instancePath '.timelesss-2.5-tests'
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

        # Use the installed optional API; no dependency is added to release JARs.
        $mekanism = Get-ChildItem -LiteralPath (Join-Path $instancePath 'mods') -Filter 'Mekanism-*.jar' | Select-Object -First 1
        if ($mekanism -and $version -eq '20.1' -and $mekanism.Name -eq 'Mekanism-1.20.1-10.4.16.80.jar') {
            Copy-Item -LiteralPath $mekanism.FullName -Destination (Join-Path $projectPath 'test-mods\Mekanism-1.20.1-10.4.16.80.jar') -Force
        } elseif ($mekanism -and $version -eq '21') {
            New-Item -ItemType Directory -Force -Path (Join-Path $testPath 'mods') | Out-Null
            Copy-Item -LiteralPath $mekanism.FullName -Destination (Join-Path $testPath "mods\$($mekanism.Name)") -Force
        }

        $env:JAVA_HOME = $previousJava
        if ($version -eq '26.1.2' -and (Test-Path -LiteralPath $Jdk25)) { $env:JAVA_HOME = $Jdk25 }
        $gradleArgs = @('runServer', '-PtimelesssSelfTest', "-PtimelesssTestDir=$testPath", '--console=plain')
        if ($version -ne '20.1') { $gradleArgs += '--no-problems-report' }
        $logPath = Join-Path $projectRoot ".test$version.log"
        Push-Location $projectPath
        try {
            & .\gradlew.bat @gradleArgs *> $logPath
            if ($LASTEXITCODE -ne 0) { throw "Gradle falhou: $logPath" }
        } finally { Pop-Location }
        if (-not (Select-String -LiteralPath $logPath -Pattern 'TIMELESSS_SELFTEST_PASSED' -Quiet)) {
            throw "Teste falhou: $logPath"
        }
        Select-String -LiteralPath $logPath -Pattern 'TIMELESSS_SELFTEST_PASSED' | ForEach-Object { "$version`: $($_.Line)" }
    }
} finally { $env:JAVA_HOME = $previousJava }
