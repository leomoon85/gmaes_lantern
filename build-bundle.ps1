param(
    [string]$Keystore = (Join-Path $PSScriptRoot 'release.keystore'),
    [string]$Alias = 'fareforge',
    [string]$Password = 'fareforge123'
)

$ErrorActionPreference = 'Stop'

if (!(Test-Path $Keystore)) {
    throw "Keystore not found at $Keystore. Run generate-keystore.ps1 first."
}

$env:LANTERN_KEYSTORE = (Resolve-Path $Keystore).Path
$env:LANTERN_KEY_ALIAS = $Alias
$env:LANTERN_STORE_PASSWORD = $Password
$env:LANTERN_KEY_PASSWORD = $Password

Write-Output "Building signed production bundle (AAB) with keystore: $env:LANTERN_KEYSTORE..."
& "$PSScriptRoot\gradlew.bat" engineTest lintDebug bundleRelease

if ($LASTEXITCODE -ne 0) {
    throw "Failed to build release bundle."
}

$aab = Join-Path $PSScriptRoot "build\outputs\bundle\release\Fareforge-release.aab"
$distDir = Join-Path $PSScriptRoot "dist"
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
$destAab = Join-Path $distDir "Fareforge.aab"
Copy-Item -Path $aab -Destination $destAab -Force

Write-Output "SUCCESS! Signed Play Store bundle created at: $destAab"
