param([string]$Apk = (Join-Path $PSScriptRoot 'dist\Lantern-Dispatch.apk'))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Apk))
try {
    $webAssets=@($archive.Entries | Where-Object { $_.FullName -match '^assets/.*\.(html?|js|css)$' })
    if($webAssets.Count){throw 'APK contains browser game assets'}
    $dataEntry=$archive.GetEntry('assets/history.json')
    if(!$dataEntry){throw 'Native historical dataset missing'}
    $reader=[IO.StreamReader]::new($dataEntry.Open())
    try{$data=$reader.ReadToEnd() | ConvertFrom-Json}finally{$reader.Dispose()}
    if($data.events.Count -ne 14 -or $data.quotes.Count -ne 11){throw 'Unexpected historical record count'}
    foreach($entry in $archive.Entries | Where-Object { $_.FullName -match '^classes\d*\.dex$' }){
        $stream=$entry.Open();$buffer=[IO.MemoryStream]::new()
        try{$stream.CopyTo($buffer);$dex=[Text.Encoding]::ASCII.GetString($buffer.ToArray());if($dex -match 'Landroid/webkit/WebView;|Landroid/webkit/WebViewClient;'){throw 'APK references a browser game runtime'}}finally{$stream.Dispose();$buffer.Dispose()}
    }
    Write-Output 'PASS: native APK, no WebView references, no HTML/JavaScript/CSS assets, 14 historical records and 11 price observations.'
} finally {$archive.Dispose()}
