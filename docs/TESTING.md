# Verificação e validação física

## Versão 0.2.0 — apresentação e espectro, 27/10/2026

Comando final executado: `gradlew.bat :dsp:verifyDsp :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest --console=plain --max-workers=2`. Resultado: **BUILD SUCCESSFUL**, 75 tarefas, 1 min 58 s. Testes de produção JVM: **245 verificações passaram** (155 anteriores e 90 novas de apresentação/espectro). Mantidos 22 tons limpos, pior erro 0,00563 cent e zero erros de oitava nesse conjunto sintético.

Os novos testes cobrem fronteira de semitom, confirmação em 300 ms, evidência descontínua, troca E2→E1, suavização logarítmica, coerência frequência/cents/alvo manual, retenção de 500 ms, watchdog, status, ruído, parcial senoidal fraco ao lado de outro forte, faixa +21 semitons, remapeamento real de picos históricos e decaimento idêntico em atualizações de 20/100 ms. Duração da suíte nessa execução concorrente: 6493,7 ms; microbenchmark DSP 38,43 ms/quadro de 32768. Não são medições Android, nem comparação controlada de desempenho.

Lint final: **zero erros e 14 avisos** — versões mais recentes, plural, backup, ícones/qualificador, sugestões KTX e dois recursos antigos sem uso. Sem supressão/baseline. A primeira tentativa desta rodada falhou por HTTP 502 no download de uma dependência de testes; a repetição resolveu. Uma compilação intermediária dos testes pegou o cabeçalho recém-editado antes da recompilação principal; a execução final recompilou ambos e passou.

`StableLayoutTest` foi compilado e empacotado em `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Verifica limites dos componentes reais em 320 dp, fontes 100%/200%, alternância de textos curtos/longos, leitura ao vivo/retida/inválida e acesso por rolagem. **Não executado:** não há emulador instalado, e `adb devices` não listou aparelhos. Portanto, não se declara validação visual, TalkBack ou microfone em dispositivo.

APK para instalar: `app/build/outputs/apk/debug/app-debug.apk`, versão **0.2.0**, versionCode **2**, 9.789.566 bytes. Assinatura debug v2 validada com `apksigner verify --verbose`. SHA-256: `44DD938D94EC5CE91C5543CD8849CC5D9C2723CF123899CB1B1E73F9B71497BF`. Manifesto mesclado conferido: sem permissão de Internet. Nenhum novo APK foi fornecido com o vídeo/texto para comparar os binários.

Para executar a instrumentação quando houver aparelho/emulador disponível:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --max-workers=2
```

Comportamento, temporizações, fórmulas e limitações: [PRESENTATION](PRESENTATION.md). Os resultados a seguir são históricos da preparação anterior.

## Preparação da versão 0.1.0 em 27/10/2026

Nesta máquina Windows, com Microsoft OpenJDK 17.0.21, os mesmos testes de produção passaram tanto pelo script isolado quanto por `gradlew.bat -PdspOnly :dsp:verifyDsp`: **155 verificações, 22 senoides, pior erro limpo 0,00563 cent, zero erros de oitava nesse conjunto**. O Gradle terminou com `BUILD SUCCESSFUL`.

Script isolado: duração 2089,0 ms e benchmark 14,37 ms/quadro de 32768. Gradle, com preparação Android concorrente: duração 8974,1 ms e benchmark 73,52 ms/quadro. A variação sob carga impede tratar esses tempos como desempenho de referência; nenhum desses resultados mede CPU ou latência Android.

JDK e SDK foram preparados localmente em `.tools/`; veja [preparação Windows](WINDOWS.md). `adb devices` não listou aparelho conectado. Nenhum teste físico foi realizado nesta sessão. Os resultados abaixo de 22/10 são históricos.

Build Android: `gradlew.bat :app:assembleDebug :app:lintDebug --console=plain` terminou com **BUILD SUCCESSFUL** após corrigir a verificação explícita de `RECORD_AUDIO` em `CaptureEngine`, incluindo tratamento de `SecurityException` caso a permissão seja revogada. A primeira tentativa havia gerado APK, mas falhado no lint por `MissingPermission`; o erro foi corrigido sem supressão nem baseline.

Lint final: zero erros e dez avisos (versões mais novas de dependências, plural de acessibilidade, regras de backup, qualificadores/ícone e sugestões KTX). Relatório local: `app/build/reports/lint-results-debug.html`. As versões fixadas não foram atualizadas apenas para eliminar avisos.

APK: `app/build/outputs/apk/debug/app-debug.apk`; assinatura debug v2 validada por `apksigner verify --verbose`. SHA-256: `FE206BB5737C1DC96A690650A2378EA0A781B9D2DB1E1AA6C670C73026F12549`. Não houve instalação no celular, emulador ou execução remota de CI.

## Execução local realizada em 22/10/2026

Compilador Kotlin embeddable 2.1.20, alvo bytecode 1.8, Oracle JRE 1.8.0_431, Windows 11 x64, Intel Core i5-1135G7 @2,40 GHz (4 núcleos/8 threads). Fontes de produção de `dsp/src/main` e verificações de `dsp/src/test` compiladas juntas, sem reimplementação do algoritmo. O build normal usa JDK17; o caminho isolado permitiu testar no ambiente com apenas JRE8.

Resultado da execução final: **155 verificações passaram**, 22 senoides limpas cobrindo ambas as taxas, **pior erro absoluto 0,00563 cent**, **0/22 erros de oitava** entre as senoides limpas. Não se extrapola essa taxa para sinais reais, outros instrumentos ou todos os casos de fundamental ausente. Duração total da suíte final: 4772,4 ms no host. Microbenchmark, 10 aquecimentos + 50 iterações, quadro de 32768: média **24,36 ms** na execução final; execuções anteriores produziram 20,14 e 27,70 ms. Isso mostra variação do host, não consumo de CPU/latência Android. Não medimos porcentagem de CPU, alocações com profiler, energia ou temperatura.

Conjunto executado:

- Senoides 20/30/40/41,2034/55/82,4069/110/261,6256/440/443/1000 Hz em 44100 e 48000 amostras/s, tolerância ≤5 cents abaixo de 40 Hz e ≤2 cents de 40 a 1000 Hz.
- E1/E2 harmônicos, fundamental fraca e ausente com 2º/3º harmônicos; suporte somente em pares retorna a periodicidade observável sem inventar suboitava. Ausência da fundamental marca ambiguidade.
- Silêncio, piso finito −120 dBFS, DC, ruído com seed fixa, clipping e limpeza do resultado após silêncio.
- Acorde senoidal C4+E4+G4: três picos esperados dentro de um bin da FFT medida; bloqueio de orientação monofônica confiante.
- Ganho coerente Hann com senoide alinhada a bin; amplitude 0,5 e RMS de aproximadamente −9,031 dBFS.
- Envelope pinçado amortecido, vibrato pequeno e troca de nota sem prender pitch anterior.
- Bateria sintética inarmônica amortecida de 100/159,3/213,5 Hz, em ambas as taxas: detecção de golpe e persistência dos três picos, sem assert de fundamental harmônica.
- Nomes e oitavas, Lá4 configurável, sinal dos cents, histerese e confirmação de saltos de oitava sem alterar a medição.
- 30 sinais aleatórios com seed fixa, janela curta, 70–900 Hz e ruído leve; 20 sinais de baixa periodicidade devem produzir estado incerto ou inválido, sem exceção.

Critérios não foram afrouxados após medir. Precisão de senoides ideais é uma propriedade desses ensaios, não garantia de exatidão acústica. Interpolação tem precisão diferente de resolução física. Os testes não cobrem um corpus de palhetadas reais, bateria real, todo acorde ou todas as fases/relações de harmônicos possíveis.

## Repetir os testes

Com JDK17:

```powershell
.\gradlew.bat -PdspOnly :dsp:verifyDsp
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Sem SDK/JDK17, com JVM8+ e PowerShell, caminho isolado empregado aqui:

```powershell
.\scripts\prepare-dsp-tools.ps1
.\scripts\test-dsp.ps1
```

O primeiro comando baixa ferramentas Maven Central para `.tools/`, ignorada pelo Git, e verifica SHA1 publicado pelo mesmo repositório para integridade de transporte. Não fornece autenticidade independente. O segundo compila as mesmas fontes da produção e executa a suíte; qualquer falha encerra com erro. Artefato de teste: `dsp/build/verification.jar`, sem runtime embutido, executado com stdlib no classpath. Não é APK.

Gradle Wrapper oficial 8.11.1 foi baixado e `--version` executou. `:app:assembleDebug` foi tentado e **falhou** ao resolver AGP: “Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM.” O projeto deve usar JDK17 conforme matriz AGP, não apenas o mínimo mencionado pelo erro de resolução. Diagnóstico em `.tools/android-build.log`, arquivo local ignorado. Não houve APK, emulador, execução remota de CI ou teste em aparelho. A checagem de XML/vetores/strings é estrutural; não substitui aapt2, lint ou compilador Android.

## Checklist no Samsung M55 ou outro Android

Registre aparelho, versão Android, microfone/rota, fonte solicitada, taxa real, sensibilidade, distância, referência e condições. Não pressupor que UNPROCESSED realmente remove todo processamento. Faça cada ensaio por pelo menos dez repetições, guardando valores observados e falhas; não invente resultados.

| Ensaio | Procedimento e evidência a registrar |
|---|---|
| Permissão | Instalar limpo; permitir, negar, negar permanentemente, reativar pelo sistema; verificar mensagens e ausência de captura não autorizada. |
| Lifecycle | Ouvir, bloquear tela, ir ao início, trocar app, voltar; conferir indicador Android do microfone, pausa, liberação e retomada sem crash/leituras antigas. Revogar permissão com app fora da tela. |
| Disputa e falhas | Abrir outro gravador/chamada; observar silenciamento, erro ou sinal fraco, pausar/retomar. Sem promessa de comportamento idêntico entre fabricantes. |
| Rotas | Conectar/remover entrada USB compatível ou headset; conferir nome/taxa, limpeza de histórico e recuperação. Não assumir compatibilidade universal de USB/Bluetooth. |
| Tons conhecidos | Gerador externo estável e referência independente: 41,2034/82,4069/110/440/1000 Hz. Medir tempo até aquisição, erro após estabilizar, oitavas incorretas e dropouts. Testar referência Lá4 442 Hz e ±10/±60 cents. |
| Grave 20/30 Hz | Somente com transdutor capaz, gerador e medição de referência apropriados. Estimar SNR e resposta real do microfone. Se equipamento ausente, registrar NÃO TESTADO. Não usar alto-falante do próprio telefone como prova. |
| Violão e baixo reais | E2 versus E1 real, harmônicos dominantes, ataque forte, decaimento, vibrato e troca rápida de corda; usar uma corda por vez. Registrar erro de oitava separadamente de erro em cents. |
| Alvo manual | Fixar E2 e tocar E1: leitura deve continuar E1, desvio próximo de −1200 cents, indicador fora da escala, sem remapeamento à corda esperada. Testar nome/oitava, Hz manual, persistência e sustenido/bemol. |
| Acorde | Reproduzir mistura C4/E4/G4 por fonte externa e depois acorde real. Comparar FFT com referência; não exigir que os três picos mais fortes do instrumento real sejam as três notas básicas. |
| Bateria | Contexto Pele/parafuso: repetir golpes na mesma região, selecionar ressonância e comparar; depois medir Som global separadamente. Alterar descarte do ataque e testar dois modos próximos/ruído. Não rotular automaticamente modo físico. |
| Silêncio e clipping | Silenciar fonte e conferir traços; saturar digitalmente via fonte apropriada e conferir aviso. Nível deve indicar dBFS, nunca SPL. |
| Acessibilidade | TalkBack: ordem, nomes de controles, alvo versus medição, picos e status. Fonte 100/150/200%, tela estreita/rotação, contraste, áreas 48 dp, barras do sistema. Corrigir qualquer sobreposição. |
| Desempenho | Sessão de 15–30 minutos com profiler: tempo DSP e UI, perdas, carga CPU, memória/GC, bateria e temperatura. Comparar janela curta/longa e modo bateria. Repetir background/resume ao aquecer. |

Planilha sugerida: instante, frequência esperada, Hz medido, cents, confiança, aquisição ms, oitava correta, dropout, rota/taxa, CPU e temperatura. Conte erros de oitava por tentativas e dropouts por tempo; informe tamanho da amostra e casos não testados. Resultados físicos desta tabela estão todos pendentes.
