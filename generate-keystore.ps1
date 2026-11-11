$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { Split-Path (Split-Path (Get-Command javac.exe).Source) }
$keytool = Join-Path $java "bin\keytool.exe"

$keystorePath = Join-Path $PSScriptRoot "release.keystore"
if (Test-Path $keystorePath) {
    Write-Output "release.keystore already exists at $keystorePath"
    exit 0
}

Write-Output "Generating release.keystore using $keytool..."
& $keytool -genkeypair -v `
    -keystore $keystorePath `
    -alias fareforge `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storetype PKCS12 `
    -storepass fareforge123 `
    -keypass fareforge123 `
    -dname "CN=Fareforge Production, OU=Mobility Tycoon, O=Fareforge, C=US"

if ($LASTEXITCODE -ne 0) {
    throw "Failed to create release.keystore"
}

Write-Output "Successfully generated: $keystorePath"
