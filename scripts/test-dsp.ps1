param([string]$Java = 'java')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot)
try {
    if (-not (Test-Path '.tools/compiler.jar')) { throw 'Compilador isolado ausente. Use Gradle 8.11.1: gradle :dsp:verifyDsp, ou prepare as dependências descritas em docs/TESTING.md.' }
    New-Item -ItemType Directory -Force 'dsp/build' | Out-Null
    $sources = @(Get-ChildItem 'dsp/src' -Filter '*.kt' -Recurse | ForEach-Object FullName)
    & $Java -cp '.tools/*' org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath '.tools/stdlib.jar;.tools/annotations.jar' -jvm-target 1.8 -d 'dsp/build/verification.jar' @sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação Kotlin.' }
    & $Java -cp 'dsp/build/verification.jar;.tools/stdlib.jar' br.com.lumen.dsp.VerificationKt
    if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes DSP.' }
} finally { Pop-Location }
