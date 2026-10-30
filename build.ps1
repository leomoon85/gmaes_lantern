param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Java = $env:JAVA_HOME,
    [string]$Work = (Join-Path $PSScriptRoot '.build'),
    [string]$Output = (Join-Path $PSScriptRoot 'dist\Fareforge.apk'),
    [switch]$Offline
)
$ErrorActionPreference='Stop'
if(!$Java){$Java=Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source)}
if($env:LANTERN_KEYSTORE){throw 'Use gradlew bundleRelease for publisher signing. This script produces a development-signed APK.'}
$env:JAVA_HOME=$Java
$env:ANDROID_HOME=$Sdk
$Work=[IO.Path]::GetFullPath($Work)
$Output=[IO.Path]::GetFullPath($Output)
New-Item -ItemType Directory -Force -Path $Work,(Split-Path $Output) | Out-Null
$gradleArgs=@('--no-daemon','engineTest','lintDebug','assembleRelease')
if($Offline){$gradleArgs=@('--offline')+$gradleArgs}
& "$PSScriptRoot\gradlew.bat" @gradleArgs
if($LASTEXITCODE -ne 0){throw "Android build failed: $LASTEXITCODE"}
$unsigned=Join-Path $PSScriptRoot 'build\outputs\apk\release\Fareforge-release-unsigned.apk'
if(!(Test-Path -LiteralPath $unsigned)){throw 'Unsigned release APK missing'}
$key=Join-Path $Work 'development.p12'
if(!(Test-Path -LiteralPath $key)){
    & "$Java\bin\keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias railways -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Fareforge Development' -storetype PKCS12
    if($LASTEXITCODE -ne 0){throw 'Could not create development signing key'}
}
& "$Sdk\build-tools\35.0.0\apksigner.bat" sign --ks $key --ks-pass pass:android --key-pass pass:android --ks-key-alias railways --out $Output $unsigned
if($LASTEXITCODE -ne 0){throw 'APK signing failed'}
& "$Sdk\build-tools\35.0.0\apksigner.bat" verify --verbose $Output
if($LASTEXITCODE -ne 0){throw 'APK signature verification failed'}
Write-Output "APK: $Output"
