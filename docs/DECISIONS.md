# Registro de decisões e fontes

Consulta web em 22/10/2026. Fontes de pesquisa anexadas foram tratadas como afirmações a verificar, não resultados físicos.

| Decisão | Fundamentação e fonte primária |
|---|---|
| AGP 8.9.2 + Gradle 8.11.1 + JDK 17 + SDK35 | Matriz oficial de compatibilidade da série 8.9: https://developer.android.com/build/releases/agp-8-9-0-release-notes . São versões estáveis fixadas, sem alegar serem as mais recentes. |
| Kotlin e plugin Compose na mesma versão 2.1.20 | Guia oficial https://kotlinlang.org/docs/compose-compiler-migration-guide.html e release https://kotlinlang.org/docs/whatsnew2120.html . |
| Compose BOM fixada | A BOM coordena versões das bibliotecas, separada do compilador: https://developer.android.com/develop/ui/compose/bom/bom-mapping . |
| UNPROCESSED condicionado a suporte | AudioManager define PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED: https://developer.android.com/reference/android/media/AudioManager . CDD: https://android.googlesource.com/platform/compatibility/cdd/+/ad3d038/5_multimedia/5_11_unprocessed-audio.md . Suporte reportado não é medição própria do microfone. |
| PCM mono e taxa real, liberação por lifecycle | API https://developer.android.com/reference/android/media/AudioRecord e fontes https://developer.android.com/reference/android/media/MediaRecorder.AudioSource . Tentativa de abrir a página AudioRecord pelo navegador desta sessão expirou; contrato de API precisa de confirmação final por compilação/lint. |
| NSDF/MPM, periodicidade curta com suporte | McLeod/Wyvill, A Smarter Way to Find Pitch: https://www.cs.otago.ac.nz/graphics/Geoff/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf . Download da publicação expirou nesta sessão; fórmula implementada e validada sinteticamente, sem alegar nova leitura integral. Alternativa YIN: https://labrosa.ee.columbia.edu/doc/yin.html . |
| Não adicionar framework DSP | Implementação original O(N log N), sem NDK, para reduzir dependências e permitir testes da produção. As afirmações de manutenção/licença do comparativo da pesquisa não foram adotadas como verificadas. KMath não é assumida biblioteca oficial da JetBrains. |
| FFT relativa separada de RMS dBFS | Definições matemáticas em DSP.md, checadas com senoide conhecida. A fórmula da pesquisa que divide pelo máximo produz dB relativos, não dBFS calibrado. Hann exige correção pelo ganho coerente. |
| Bateria sem inferência de modos físicos | Fontes de acústica indicadas na pesquisa: Rossing, Acoustics of Drums, https://doi.org/10.1063/1.881331 . Nesta versão, nenhum rótulo de modo físico ou razão ideal é atribuído a um pico real. A publicação não foi consultada integralmente nesta sessão. |

Implementação tomou o Prompt 2 como escopo. Prompts 1, 3, 4 e 5 não foram executados. Não há chamadas de IA no app. Identidade segue cores especificadas e paths originais; nenhuma disponibilidade de marca foi verificada.
