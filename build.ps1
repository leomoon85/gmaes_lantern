param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Java = $env:JAVA_HOME,
    [string]$Work = (Join-Path $PSScriptRoot '.build'),
    [string]$Output = (Join-Path $PSScriptRoot 'dist\Lantern-Dispatch.apk')
)
$ErrorActionPreference = 'Stop'
if (!$Java) { $Java = Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source) }
$Work = [IO.Path]::GetFullPath($Work)
$Output = [IO.Path]::GetFullPath($Output)
$env:JAVA_HOME = $Java
$env:PATH = "$Java\bin;$env:PATH"
$tools = Join-Path $Sdk 'build-tools\35.0.0'
$android = Join-Path $Sdk 'platforms\android-35\android.jar'
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build tool failed with exit code $LASTEXITCODE" } }
New-Item -ItemType Directory -Force -Path $Work,"$Work\classes","$Work\dex","$Work\tests" | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
& "$Java\bin\javac.exe" -encoding UTF-8 -d "$Work\tests" "$PSScriptRoot\src\com\lantern\dispatch\Game.java" "$PSScriptRoot\tests\GameTest.java"
Check-Exit
& "$Java\bin\java.exe" -cp "$Work\tests" com.lantern.dispatch.GameTest
Check-Exit
$browserHtml = [IO.File]::ReadAllText("$PSScriptRoot\assets\index.html").Replace('href="bulletins.css"', 'href="assets/bulletins.css"').Replace('src="bulletins.js"', 'src="assets/bulletins.js"').Replace('src="history-data.js"', 'src="assets/history-data.js"')
$browserHtml = $browserHtml.Replace('href="map-feedback.css"', 'href="assets/map-feedback.css"').Replace('src="map-runtime.js"', 'src="assets/map-runtime.js"').Replace('href="gameplay-modes.css"', 'href="assets/gameplay-modes.css"').Replace('src="gameplay-modes.js"', 'src="assets/gameplay-modes.js"')
[IO.File]::WriteAllText("$PSScriptRoot\index.html", $browserHtml, [Text.UTF8Encoding]::new($false))
& "$tools\aapt.exe" package -f -M "$PSScriptRoot\AndroidManifest.xml" -S "$PSScriptRoot\res" -A "$PSScriptRoot\assets" -I $android -F "$Work\base.apk"
Check-Exit
$sources = Get-ChildItem -LiteralPath "$PSScriptRoot\src" -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& "$Java\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android;$tools\core-lambda-stubs.jar" -d "$Work\classes" @sources
Check-Exit
& "$Java\bin\jar.exe" "-J-Djava.io.tmpdir=$Work" cf "$Work\classes.jar" -C "$Work\classes" .
Check-Exit
& "$tools\d8.bat" --lib $android --min-api 26 --output "$Work\dex" "$Work\classes.jar"
Check-Exit
Copy-Item -LiteralPath "$Work\base.apk" -Destination "$Work\unsigned.apk" -Force
Push-Location "$Work\dex"
try { & "$tools\aapt.exe" add "$Work\unsigned.apk" classes.dex; Check-Exit } finally { Pop-Location }
& "$tools\zipalign.exe" -f 4 "$Work\unsigned.apk" "$Work\aligned.apk"
Check-Exit
# A local development certificate. Keep this key to sign compatible updates.
if (!(Test-Path -LiteralPath "$Work\development.p12")) {
    & "$Java\bin\keytool.exe" -genkeypair -keystore "$Work\development.p12" -storepass android -keypass android -alias railways -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Lantern Dispatch Development' -storetype PKCS12
    Check-Exit
}
& "$tools\apksigner.bat" sign --ks "$Work\development.p12" --ks-pass pass:android --key-pass pass:android --ks-key-alias railways --out $Output "$Work\aligned.apk"
Check-Exit
& "$tools\apksigner.bat" verify --verbose $Output
Check-Exit
Write-Output "APK: $Output"

