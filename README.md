# Lúmen — Afinador cromático

Aplicativo Android nativo em Kotlin e Jetpack Compose. Analisa áudio no aparelho: afinador monofônico, espectro FFT e ressonâncias de bateria em modo experimental. Sem anúncios, contas, rastreamento, gravação de áudio ou permissão de Internet.

![Identidade vetorial do projeto, não captura de tela](assets/github-cover.svg)

Referência Lá4 entre 400 e 480 Hz, em passos de 0,1 Hz; temperamento igual; notas internacionais e equivalentes em português, oitavas científicas, sustenidos/bemóis, alvo automático com histerese ou alvo manual por nota/oitava/Hz. Presets de violão e baixo fixam alvos somente por seleção explícita. A nota detectada não é remapeada às cordas.

Interface escura em português, cores próprias para as 12 notas, Hz/cents/dBFS, confiança, saturação e barra ±50 cents. Cents negativos: **Apertar**; positivos: **Afrouxar**; centro: **Afinado**, com tolerância configurável de ±3 cents inicialmente. Sinal ambíguo não recebe instrução mecânica.

## Estado da entrega

**APK debug 0.2.0 compilado nesta máquina em 27/10/2026**, com JDK 17 e SDK Android 35 preparados localmente. Assinatura do APK verificada com `apksigner`. Os testes JVM de produção passaram nas **245 verificações** (DSP, apresentação e espectro): 22 senoides em 44,1/48 kHz, pior erro limpo 0,00563 cent e zero erros de oitava nesse conjunto. Testes Compose compilados, mas não executados; nenhum aparelho conectado/emulador foi encontrado. Instalação, captura de microfone e interface no celular ainda não foram testadas nesta sessão. Consulte [TESTING](docs/TESTING.md) para os resultados e limites.

## Compilar e instalar

Pré-requisitos: JDK 17, Android SDK com `platforms;android-35`, `build-tools;35.0.0` e platform-tools. Configure `JAVA_HOME` e `ANDROID_HOME` no seu ambiente, ou selecione o JDK 17 e SDK pelo Android Studio. Não versionar `local.properties`. Gradle Wrapper 8.11.1 incluído; dependências fixadas, sem exigir instalação global do Gradle. A primeira compilação precisa de rede para baixar ferramentas/dependências; o aplicativo funciona offline.

**Nesta cópia Windows, as ferramentas já foram preparadas em `.tools/`.** Veja [WINDOWS.md](docs/WINDOWS.md) para ativá-las no terminal e instalar o APK no celular. Android Studio é opcional para compilar por linha de comando. APK local: `app/build/outputs/apk/debug/app-debug.apk`.

Build e lint concluídos com sucesso; lint com zero erros e 14 avisos documentados em [TESTING](docs/TESTING.md).

PowerShell, na raiz:

```powershell
.\gradlew.bat -PdspOnly :dsp:verifyDsp
.\gradlew.bat :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n br.com.lumen/.MainActivity
```

Linux/macOS: `sh ./gradlew -PdspOnly :dsp:verifyDsp` e `sh ./gradlew :app:assembleDebug :app:lintDebug`. Para repetir os testes locais com a JVM 8, veja a alternativa independente em [TESTING](docs/TESTING.md).

Ao abrir, permita o microfone. Se negar, use o botão de permissões do aplicativo. A captura pausa ao sair da Activity e é liberada; retoma ao voltar quando autorizada. Não há serviço de microfone em segundo plano. Use Pausar/Retomar para recuperar uma entrada interrompida.

## Como interpretar

- **Afinador:** toque uma corda de cada vez. O número principal é a nota medida; o alvo manual fica separado. A barra não transforma erros de mais de 50 cents em outra nota.
- **Espectro:** FFT medida com vista musical da referência até a 13ª maior (+21 semitons), alvo manual para acordes e faixa completa opcional de 20 Hz a Nyquist. Desenho relativo com gate, referência suavizada e realce visual de picos; não é RMS dBFS. Ciano indica som atual; violeta, histórico retido por 1 s e depois liberado a 16 dB/s. Congelamento guarda gráfico e referência juntos. A lista numérica conserva dB relativos ao máximo do quadro original. O marcador rosa é f₀ monofônico, separado dos máximos medidos. Não reconhece acordes.
- **Bateria experimental:** escolha Pele/parafuso ou Som global, toque, aguarde a janela pós-ataque e selecione a ressonância pretendida. Compare frequências e repetições, sem presumir que o pico mais forte seja a fundamental. Não há alvos universais para tambores.

dBFS é nível digital RMS, com piso de −120 dBFS; **não é dB SPL**. 20–2000 Hz é o intervalo de estimativa do detector; 20 Hz não é sensibilidade comprovada do hardware. Microfones, processamento do fabricante, ruído e latência limitam o resultado. Graves usam janela de até 32768 amostras (683 ms a 48 kHz; 743 ms a 44,1 kHz), além da latência de captura. Não há promessa de precisão física universal.

## Estrutura e documentação

`app/`: captura, estado persistente, interface e recursos Android. `dsp/`: matemática musical, FFT, MPM e bateria sem dependências Android. `assets/`: fontes vetoriais originais. `scripts/`: verificação local. `.github/workflows/`: CI preparada, ainda não executada remotamente.

[Arquitetura](docs/ARCHITECTURE.md) · [DSP](docs/DSP.md) · [Design](docs/DESIGN.md) · [Testes](docs/TESTING.md) · [Privacidade](docs/PRIVACY.md) · [Limitações](docs/LIMITATIONS.md) · [Decisões e fontes](docs/DECISIONS.md) · [Dependências](THIRD_PARTY_NOTICES.md)

Na versão 0.2.0, status e leituras reservam espaço para evitar saltos de layout. A nota usa confirmação de 300 ms e suavização de 140 ms; perdas breves mostram **Leitura retida** por até 500 ms, sem orientação mecânica. Fórmulas, referências e limites em [Apresentação e espectro](docs/PRESENTATION.md).

Próximas etapas: teste de captura/lifecycle/acessibilidade no Samsung M55 ou outro Android, corpus de gravações reais, desempenho térmico, revisão dos limiares de ambiguidade e experiência visual no dispositivo. Fontes ampliadas e TalkBack precisam de validação instrumental. Não há screenshots de produção nesta entrega.

## Licença

Recomenda-se MIT ou Apache-2.0 para o código original, a decidir pelo titular. Nenhuma licença do projeto nem identidade de proprietário foi declarada. Dependências e Gradle Wrapper mantêm suas próprias licenças. Nenhum commit, stage, push ou publicação foi realizado.
