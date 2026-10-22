$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot)
try {
    New-Item -ItemType Directory -Force .tools | Out-Null
    $dependencies = @{
        compiler = 'org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.20/kotlin-compiler-embeddable-2.1.20.jar'
        stdlib = 'org/jetbrains/kotlin/kotlin-stdlib/2.1.20/kotlin-stdlib-2.1.20.jar'
        script = 'org/jetbrains/kotlin/kotlin-script-runtime/2.1.20/kotlin-script-runtime-2.1.20.jar'
        reflect = 'org/jetbrains/kotlin/kotlin-reflect/1.6.10/kotlin-reflect-1.6.10.jar'
        trove = 'org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar'
        coroutines = 'org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.8.0/kotlinx-coroutines-core-jvm-1.8.0.jar'
        annotations = 'org/jetbrains/annotations/13.0/annotations-13.0.jar'
    }
    foreach ($name in $dependencies.Keys) {
        $url = 'https://repo.maven.apache.org/maven2/' + $dependencies[$name]
        $file = '.tools/' + $name + '.jar'
        Invoke-WebRequest -Uri $url -OutFile $file
        $checksumResponse = (Invoke-WebRequest -Uri ($url + '.sha1')).Content
        $expected = if ($checksumResponse -is [byte[]]) { [System.Text.Encoding]::UTF8.GetString($checksumResponse).Trim() } else { $checksumResponse.ToString().Trim() }
        if ((Get-FileHash $file -Algorithm SHA1).Hash.ToLowerInvariant() -ne $expected.ToLowerInvariant()) { throw "Integridade inválida: $name" }
    }
    Write-Output 'Ferramentas Kotlin locais preparadas em .tools; nenhum componente foi instalado no sistema.'
} finally { Pop-Location }
