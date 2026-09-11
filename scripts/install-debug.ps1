# Installs the freshly built debug APK on one explicitly selected device.
# Usage: .\scripts\install-debug.ps1 -Serial 192.168.0.210:44875
param([Parameter(Mandatory = $true)][string] $Serial)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw "APK missing: $apk. Run scripts\build-and-test.ps1 first; never install a stale APK." }

$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

& $adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }
& $adb -s $Serial shell dumpsys package com.mani.controlcentre | Select-String -Pattern 'versionName|lastUpdateTime'
