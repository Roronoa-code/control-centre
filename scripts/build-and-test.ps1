# Builds the debug APK, runs unit tests and lint. Stops on the first failing Gradle exit code.
# Usage: .\scripts\build-and-test.ps1            (default tasks)
#        .\scripts\build-and-test.ps1 :app:assembleDebug
param([string[]] $Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug'))

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $candidates = @(
        'C:\HA\HEALTH APP\.local\toolchains\jdk-17.0.20.1+1',
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr')
    )
    $env:JAVA_HOME = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
}
if (-not $env:JAVA_HOME) { throw 'No JDK found. Set JAVA_HOME to a JDK 17 or newer.' }

$verification = Join-Path $root 'verification'
New-Item -ItemType Directory -Force $verification | Out-Null
$log = Join-Path $verification ('build-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')

Push-Location $root
try {
    & .\gradlew.bat @Tasks --console=plain | Tee-Object -FilePath $log
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE. Log: $log" }
} finally {
    Pop-Location
}
Write-Output "Build log: $log"
