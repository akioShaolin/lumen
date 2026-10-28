# Carregue no terminal atual: . .\scripts\use-local-tools.ps1
$projectRoot = Split-Path $PSScriptRoot
$jdkRoot = Join-Path $projectRoot '.tools/jdk'
$localJdk = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } |
    Select-Object -First 1
if (-not $localJdk) { throw 'JDK local ausente em .tools/jdk. Instale JDK 17 e configure JAVA_HOME manualmente.' }
$env:JAVA_HOME = $localJdk.FullName
$env:ANDROID_HOME = Join-Path $projectRoot '.tools/android-sdk'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools/android-user'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-home'
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
Write-Host 'JDK e SDK locais selecionados somente para este terminal.'
