# Preparação local no Windows

Em 27/10/2026, foram preparados nesta cópia do projeto, dentro de `.tools/` (ignorada pelo Git): Microsoft OpenJDK 17.0.21, Android Command-line Tools 22.0, plataforma Android 35, Build Tools 35.0.0, Platform Tools (adb) e Gradle Wrapper 8.11.1. Não é necessário instalar Gradle globalmente. As ferramentas locais não acompanham o repositório em outra máquina.

Fontes oficiais: [JDK Microsoft](https://learn.microsoft.com/en-us/java/openjdk/download), [ferramentas Android e Android Studio](https://developer.android.com/studio). O checksum SHA-256 do ZIP das ferramentas Android foi conferido com o publicado pelo Google.

## Usar as ferramentas já preparadas

Abra PowerShell na raiz do projeto. A política de execução desta máquina bloqueou scripts; a alteração abaixo vale apenas para o processo atual:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
. .\scripts\use-local-tools.ps1
.\gradlew.bat -PdspOnly :dsp:verifyDsp
.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
```

O script seleciona JDK, SDK e cache locais apenas nesse terminal. A primeira compilação requer rede para os repositórios Google/Maven/Gradle.

## Instalar no celular

É necessário Android 8.0/API 26 ou superior. Ative as opções do desenvolvedor e a depuração USB no celular, conecte por cabo de dados e autorize o computador no aparelho. Se o Windows não o reconhecer, instale o driver USB do fabricante.

Depois de um build bem-sucedido, no mesmo terminal:

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n br.com.lumen/.MainActivity
```

Permita o microfone quando solicitado. Em 27/10/2026, `adb devices` não listou aparelho conectado; instalação, execução e medições físicas continuam pendentes.

## Instalação em outra cópia

Instale JDK 17 e Android Studio, ou somente as ferramentas de linha de comando Android. Pelo SDK Manager, instale Android SDK Platform 35, Android SDK Build-Tools 35.0.0 e Android SDK Platform-Tools; aceite as licenças apresentadas. Configure `JAVA_HOME` para o JDK 17 e `ANDROID_HOME` para o SDK (ou configure o SDK em `local.properties`, sem versioná-lo). No Android Studio, selecione JDK 17 como Gradle JDK. O script local acima exige a estrutura `.tools/` desta cópia; com instalações externas, configure as variáveis manualmente.

Para usar um emulador, instale adicionalmente Android Emulator e uma imagem de sistema pelo Android Studio. Isso não foi instalado nem testado nesta sessão; para validação acústica, use um aparelho físico.
