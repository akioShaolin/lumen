# Lúmen — Afinador cromático

Pacote de projeto e prompts • 22/10/2026

## Como usar

Nome proposto: **Lúmen**. Não foi feita verificação de disponibilidade de marca ou nome em lojas.

1. **Gemini AI Plus:** em um chat novo com pesquisa web, envie o prompt 1. Salve a resposta como `research.md`. É uma checagem técnica curta, não uma implementação paralela.
2. **Codex:** abra a pasta local destinada ao projeto (preferencialmente no computador com Android Studio/SDK). Envie o prompt 2, a imagem original, a proposta visual criada aqui e `research.md`, se disponível. O prompt é autossuficiente: a pesquisa não bloqueia o início. Ele deve implementar e testar, sem commit, push ou publicação.
3. **Claude Free:** após a primeira implementação, envie o prompt 3 com os arquivos pequenos que ele lista. Use para revisão pontual de DSP; não envie o projeto inteiro. Se atingir o limite, continue depois, sem bloquear o desenvolvimento.
4. **Codex:** envie o prompt 4 com o parecer do Claude. Ele deve avaliar e corrigir os problemas comprovados.
5. **ChatGPT Work:** use para avaliar telas, resultados reais e criar variantes das artes pelo prompt 5. Não é necessário contratar APIs de IA; o aplicativo não usará IA em nuvem.

As respostas precisam ser transferidas manualmente entre os serviços. Esta divisão é uma proposta de fluxo, não uma garantia sobre limites ou recursos de cada assinatura.

## Decisões de produto

- Android nativo, Kotlin e Jetpack Compose; operação offline, sem anúncios, conta, rastreamento ou assinatura.
- Afinador monofônico para uma corda por vez; espectro FFT independente para observar várias notas; bateria em modo experimental de ressonâncias.
- “Pitch” significa referência de afinação: Lá4 padrão de 440 Hz, ajustável entre 400 e 480 Hz em passos de 0,1 Hz. Temperamento igual de 12 notas na primeira versão.
- Nível numérico em **dBFS**, relativo ao limite digital. Não é medidor de pressão sonora em dB SPL sem calibração e caracterização do microfone.
- Análise configurável até 20 Hz, mas sem prometer que o microfone do celular capture essa frequência adequadamente. Em 20 Hz, um ciclo dura 50 ms: detectar com estabilidade exige vários ciclos e maior latência.
- Harmônicos são múltiplos inteiros acima da fundamental. Um detector pode confundir períodos e escolher uma suboitava; não se deve presumir que existam “harmônicos abaixo de 1”. Sinais ambíguos não permitem garantia universal: o aplicativo deve indicar baixa confiança e oferecer alvo manual.
- Um acorde de Dó maior contém Dó, Mi e Sol mais os harmônicos de cada nota. Esses três picos não precisam ser os mais altos; dependem do instrumento, voicing, execução e captação. Não fabricar picos para ilustrar um acorde.
- Membranas de bateria têm modos frequentemente inarmônicos. Identificar a ressonância da pele perto de um parafuso é diferente de medir a nota global do tambor.

## Identidade e interface

Estética neon inspirada na descrição da Galeria -2048, sem alegar copiar sua paleta exata. Fundo `#090B12`, cartões `#141925`, texto `#F4F7FF`, secundário `#A7B0C0`, ciano `#35E8FF`, violeta `#A78BFA`, rosa `#FF4FD8`, verde afinado `#46F5AE`, alerta `#FFBC66`.

| Nota | Cor |
|---|---|
| C | #35E8FF |
| C♯ | #54C7FF |
| D | #789BFF |
| D♯ | #A78BFA |
| E | #D279FF |
| F | #FF4FD8 |
| F♯ | #FF6B9D |
| G | #FF9671 |
| G♯ | #FFD166 |
| A | #DBF56A |
| A♯ | #8FF59B |
| B | #46F5CE |

Ordem da tela: cabeçalho com Lá4 → modos → faixa de 12 notas → nota e oitava grandes → Hz/cents/dBFS → espectro → barra inferior de afinação. Alvo automático ou manual visível. Cents negativos: “Apertar”; positivos: “Afrouxar”; centro: “Afinado”. Nunca orientar rotação horária/anti-horária da tarraxa.

Cor da nota não deve ser confundida com status de afinação. Usar texto, posição e ícone além de cor. Tipografia legível, números tabulares, brilho discreto, áreas de toque de pelo menos 48 dp, suporte a fontes ampliadas e TalkBack. Marca: diapasão abstrato em forma de L com onda central. A imagem conceitual é referência estética; números, textos e gráficos finais devem ser componentes nativos, não uma imagem usada como tela.

## Prompt 1 — Gemini: pesquisa técnica

```text
Act as a technical research assistant for Lúmen, an offline Android tuner. Research current PRIMARY sources only: Android audio capture documentation, original YIN/MPM or related pitch estimation publications, and authoritative acoustics references on drum membrane modes. Do not write the application.

Requirements: native Kotlin/Compose, microphone PCM input, adjustable A4 400–480 Hz, monophonic string tuning without systematic octave errors, live Hz/cents/dBFS, independent FFT spectrum showing measured peaks from chords, and experimental drum resonance analysis down to 20 Hz when the hardware allows it. No ads, accounts, telemetry or paid/cloud APIs.

Return a concise research.md in Portuguese, maximum 1,200 words, with source URLs and access date. Cover: AudioRecord source selection and UNPROCESSED availability/fallbacks; lifecycle and permission handling; sample rate/window/latency tradeoffs; YIN or MPM plus spectral octave validation; limitations of missing fundamental estimation; why dBFS is not dB SPL; why drum modes need a separate interpretation; FFT amplitude normalization and peak interpolation; low-frequency microphone limitations. Compare at most 3 suitable DSP dependency choices, including license, maintenance evidence and pure-Kotlin feasibility. Mark unverified statements. Suggest reproducible tests for 82.4069 Hz E2 versus 41.2034 Hz E1, harmonic-dominant inputs, true bass notes, chords and damped inharmonic signals. Do not claim universal 20 Hz sensitivity or perfect octave detection. Separate evidence from design recommendations.
```

## Prompt 2 — Codex: projeto e implementação completa

```text
You are the implementation owner of Lúmen, a polished native Android chromatic tuner. Build the actual app, tests, original vector assets and GitHub-ready documentation in the current workspace. All user-facing UI, errors, accessibility labels and primary documentation must be Brazilian Portuguese. Code identifiers may be English. Do not commit, stage, push, tag, publish or create a remote repository. Inspect existing files and AGENTS.md first; preserve unrelated work. Work autonomously through implementation and verification, not just planning. If the Android toolchain is unavailable, still deliver source and runnable DSP tests where possible, reporting exactly which checks could not run. Never fabricate successful builds or physical-device measurements.

Inputs: attached legacy tuner screenshot as functional inspiration, Lúmen visual concept as aesthetic direction, and optional research.md. Treat external research as claims to verify. This prompt is authoritative when an artistic mockup conflicts with DSP correctness.

PRODUCT
Native Kotlin + Jetpack Compose, offline, no ads, login, subscriptions, trackers, cloud processing or Internet permission. Choose and document compatible stable build dependencies after checking the actual environment. Avoid unnecessary frameworks or NDK complexity. Keep DSP independently testable on the JVM, capture separate from analysis, and UI separate from both. Use unidirectional state and persistent settings. Provide a buildable Android project with reproducible build instructions; produce a debug APK when tooling permits, never a misleading placeholder APK.

Three modes: Afinador (monophonic), Espectro (multi-frequency observation, not automatic chord recognition), Bateria (experimental resonance analysis). A4 reference default 440 Hz, editable 400–480 Hz step 0.1 Hz; equal temperament. Persist it and display it. Offer international note names with Portuguese equivalent, scientific octave numbers (A4 = MIDI 69), sharp/flat preference, auto target and manual note/octave or frequency target. Include standard guitar E2 A2 D3 G3 B3 E4 and bass E1 A1 D2 G2 presets plus fully chromatic mode. Presets must not silently remap actual pitches to expected strings.

Live display: detected note and octave, measured Hz, signed cents and RMS level in dBFS, with confidence/signal status. Use Portuguese decimal formatting. Frequency and cents resolution must reflect actual confidence, not imply guaranteed accuracy. Formula m = 69 + 12*log2(f/A4); target frequency = A4*2^((m_target-69)/12); cents = 1200*log2(f/f_target). Invalid/silent states show em dashes, not invented or stale numbers. Keep manual target fixed; automatically selected target uses sensible hysteresis. In manual mode show actual detected note separately from target.

Bottom tuning bar spans -50..+50 cents with visible center and a configurable in-tune tolerance (default ±3 cents). Negative cents means 'Apertar'; positive means 'Afrouxar'; centered means 'Afinado'. Use off-scale indicators for larger errors and show real numeric cents, never wrap them. Never instruct clockwise/counterclockwise rotation. Ambiguous or polyphonic audio must not present confident mechanical tuning instructions.

AUDIO
Use AudioRecord PCM mono, preferably actual supported 48 kHz with fallback to 44.1 kHz or a verified supported rate. Use the actual rate in all calculations. Request RECORD_AUDIO at runtime with clear PT-BR rationale; handle denial, permanent denial, mic contention, routing changes and capture errors. Prefer UNPROCESSED only after checking support; evaluate VOICE_RECOGNITION/MIC fallback, report selected source, avoid promising raw audio. Disable supported AGC/noise suppression/echo processing where possible without assuming all devices comply. No background microphone service; release capture on background/navigation out of capture and recover safely on resume. Do not store or upload raw audio by default. Allow supported external input routes without claiming every USB microphone is compatible.

Use bounded reusable buffers, a capture worker and analysis worker; no per-frame unbounded allocations or main-thread DSP. Track overruns and input clipping. UI throttling must be independent of capture. RMS dBFS = 20*log10(max(rms(normalized PCM), epsilon)); label it dBFS. Define the finite silence floor and distinguish RMS from peak. Do not label arbitrary PCM level as dB SPL. Use conservative adaptive noise gating with configurable sensitivity; sensitivity changes thresholds, not hardware bandwidth.

PITCH AND OCTAVE ROBUSTNESS — CRITICAL
Do not use the largest FFT bin alone or the lowest matching harmonic divisor. Implement YIN or MPM periodicity estimation with sub-sample refinement and confidence, supported by spectral candidate checks. Evaluate plausible f, f/2 and 2f candidates using periodicity, spectral support, harmonic coverage, unexplained energy and temporal stability. Reward the shortest well-supported period rather than arbitrary doubled periods. A proposed suboctave whose only support lies at its even harmonics must not win merely because those bins also match a lower harmonic grid. Avoid a simplistic harmonic-product-spectrum implementation that silently chooses subharmonics.

Test that ordinary E2 at 82.4069 Hz is not labeled E1 at 41.2034 Hz. Also test REAL E1 so the fix does not just double every bass estimate. Cover strong 2nd/3rd harmonics, weak and missing fundamental, realistic plucked envelopes, transients, vibrato, noise and sudden note changes. Missing-fundamental inference is inherently ambiguous for some signals; display low confidence or ask for a manual target, never claim infallibility. A manual target is a prior, not permission to fabricate a measurement. Do not force an octave based solely on the selected instrument. Exclude initial pick transients when justified and do not smooth so heavily that the display sticks to the previous note.

Separate analysis window sizes from UI rate. Use multiresolution buffers: shorter windows for normal string pitches, longer windows for bass/sub-bass. At 48 kHz, 4096/8192/32768 samples cover about 85/171/683 ms; treat these as design starting points, not mandatory magic constants. Efficient decimation is allowed only with correct anti-alias filtering. Avoid O(N^2) brute force at large windows; benchmark CPU and latency. Distinguish interpolation precision, FFT bin spacing and true resolution; zero padding does not create physical resolution. A range reaching 20 Hz is a processing capability, not proof of microphone response.

SPECTRUM
Compute a real measured FFT/STFT with a documented window such as Hann, overlap, amplitude normalization and correct one-sided scaling. Default logarithmic frequency axis 20 Hz–5 kHz; allow broader view up to actual Nyquist. Offer peak hold/decay and freeze. Clearly label relative spectral dB (relative to frame maximum) versus calibrated digital amplitude; never confuse the graph's normalization with RMS dBFS. Label measured peaks with frequency and nearest note where confidence allows; frequency labels remain valid independently of pitch selection. Support display of all 12 chromatic notes, not merely a seven-note scale. Optional chroma view must be distinct from FFT and must not replace octave-aware detection.

A synthetic C4+E4+G4 fixture at approximately 261.63, 329.63 and 392.00 Hz must display three corresponding peaks. Real C-major playing includes each note's harmonics; do not assume the root/third/fifth are the three highest peaks or generate artificial peaks. Mark selected monophonic f0 separately from measured spectral maxima. Do not label peaks as root/third/fifth unless a verified/manual reference context supports those labels. No chord classifier is required for v1.

DRUMS
Implement a clearly labeled experimental mode, not the string detector renamed. Detect a strike, ignore the unreliable broadband onset, analyze an adjustable post-onset decay segment, estimate prominent stable resonance peaks, and hold a result for comparison. Provide 'Pele / parafuso' and 'Som global' contexts, a manually selected reference resonance/frequency, repeated-hit comparison and a way to select the intended peak. Do not call the loudest mode the musical fundamental by definition. Avoid silently switching modes between hits. Display Hz primarily; optional nearest note/cents are a reference rather than a guaranteed drum pitch. Explain in PT-BR that tuning near each tension rod and the assembled drum's overall resonance are different measurements. No universal drum note targets or guarantees for cymbals/snare noise. Target processing range down to 20 Hz where signal quality permits, with long-window latency explained and a visible low-confidence state. Do not remove 20 Hz via an aggressive high-pass filter; document any DC removal/filter transfer response. No automatic claim that the microphone supports 20 Hz unless actually measured with suitable equipment.

VISUAL DESIGN AND ORIGINAL ASSETS
Brand Lúmen, subtitle 'Afinador cromático'. Background #090B12, surface #141925, text #F4F7FF, secondary #A7B0C0, cyan #35E8FF, violet #A78BFA, pink #FF4FD8, tuned green #46F5AE, warning #FFBC66. Twelve pitch-class colors: C #35E8FF, C# #54C7FF, D #789BFF, D# #A78BFA, E #D279FF, F #FF4FD8, F# #FF6B9D, G #FF9671, G# #FFD166, A #DBF56A, A# #8FF59B, B #46F5CE. Note color is independent of tuning status. Refine contrast/accessibility as necessary and document changes.
Layout: top brand/reference pitch/settings; mode navigation; chromatic note strip; dominant note+octave; Hz/cents/dBFS cards; real spectrum; prominent tuning bar near bottom above system insets. Small screens may use mode-specific expanded spectrum rather than shrinking everything. Dark theme, subtle glow, restrained animation, tabular digits, 48 dp touch targets, font scaling and TalkBack. No color-only communication or decorative fake measurements. States: permission required, listening, weak signal, clipping, uncertain pitch, tuned, paused. Portuguese strings in resources.
Create ORIGINAL code-native vector art: abstract tuning-fork L logo, SVG master, Android adaptive foreground/background and monochrome icon, wordmark, GitHub cover. Convert formats properly, no renamed SVG pretending to be VectorDrawable. Follow Android icon safe zones. Make diagrams/mockups clearly distinguishable from actual screenshots. Do not depend on generated raster artwork to render functional UI. Include sources and asset provenance; do not copy third-party logos or invent an exact Galeria -2048 palette.

VERIFICATION AND DELIVERY
Write meaningful deterministic DSP tests and fixtures with fixed seeds: silence/noise/DC/clipping; pure tones across range; 20/30/41.2034/82.4069/440 Hz; detuned tones and configurable A4; harmonic-rich E2 and real E1; missing fundamental including inherently ambiguous cases; note changes/vibrato; C-major mixture; damped inharmonic drum signals; both supported sample rates. Exercise production DSP code, not just a separate prototype. Verify spectrum normalization, cents signs, octave naming and stale-result clearing. Distinguish ideal synthetic pass criteria from field performance. Aim at ≤2 cents sustained synthetic error for clean 40–1000 Hz tones after settling and ≤5 cents at 20–40 Hz with longer windows; report measured results and justify deviations instead of silently loosening assertions. Report octave error rate separately. Pure C-major sine mixture must resolve expected peaks within measured resolution. Drum tests assess resonance tracking, not a fictional harmonic f0. Include meaningful Android permission/lifecycle/UI checks when environment supports them.
Benchmark on available hardware with honest distinction between host and phone. Provide a PT-BR physical validation checklist suitable for Samsung M55 or another available Android, using known-reference tones and actual guitar/bass/drums. Do not infer accuracy from a self-loop phone speaker test at 20 Hz. Report acquisition time, settled error, octave errors, dropouts, CPU load, thermal behavior and background/resume recovery. State untested cases.

Write README.md (PT-BR), docs/ARCHITECTURE.md, docs/DSP.md, docs/DESIGN.md, docs/TESTING.md, docs/PRIVACY.md, docs/LIMITATIONS.md, CONTRIBUTING.md, CHANGELOG.md, dependency/license notices and a source-linked decision log. Include prerequisites, exact build/test/install commands, microphone permission, supported range versus measured range, dBFS meaning, latency tradeoffs, chord/drum limitations, screenshots only if actually produced, repository structure and roadmap. Recommend a project license but do not invent owner identity or declare a license choice the user has not made. Do not include keys, absolute personal paths or APK signing secrets. Add a CI workflow if meaningful; don't claim it ran remotely.

Proceed in milestones: runnable capture/UI shell; tested DSP/spectrum; experimental drum mode; final assets/docs/verification. End with concise PT-BR summary listing implemented features, actual test/build results, remaining limitations, artifact paths and exact next local commands. Leave all changes uncommitted and unstaged. Do not stop after generating a plan.
```

## Prompt 3 — Claude Free: revisão focada

Envie primeiro `docs/DSP.md`, o detector de frequência, a seleção de oitavas e os testes correspondentes. Se couber, acrescente a normalização FFT e análise de bateria. Não envie APK, build, dependências ou logs extensos.

```text
Review the attached DSP design, production code and tests for Lúmen, an offline Android tuner. Do not rewrite the project. Focus on falsifiable correctness defects and minimal fixes. User-facing output must be Portuguese; maximum 900 words.

Priority 1: prevent normal guitar E2 (82.4069 Hz) from becoming E1 (41.2034 Hz), without breaking real E1. Inspect doubled-period/subharmonic candidates, even-only harmonic support, weak/missing fundamentals, invalid confidence and smoothing locks. Explain genuinely ambiguous cases instead of promising perfect detection. Priority 2: verify Hz→note/octave/cents math with configurable A4, sign of tighten/loosen guidance, actual sample rate, FFT normalization, RMS dBFS and silence handling. Priority 3: 20 Hz window/latency/filter feasibility and inharmonic drum resonance tracking. Check for tests that merely reproduce implementation assumptions, and whether production code is really exercised.

Return only: (1) blocker/high/medium findings with file/function references and concrete counterexample signals; (2) minimal corrections; (3) up to six missing regression tests; (4) what cannot be concluded from supplied files. Do not claim to have run code unless you actually can and did. Request only the smallest missing relevant file. Do not demand a cloud service or paid API.
```

## Prompt 4 — Codex: aplicar revisão

```text
Evaluate the attached Claude DSP review against the actual Lúmen code. Reproduce each substantive finding with a deterministic regression test before fixing it where feasible. Accept valid findings, explain rejected ones with evidence, and preserve all requirements from the original implementation prompt, particularly true E1 detection, E2 octave robustness, real measured spectrum, honest low-confidence behavior, Portuguese UI, no ads/cloud, and no git staging/commits/pushes. Run relevant tests and build if available. Update DSP/limitations/testing documentation with measured results. Do not claim physical 20 Hz sensitivity from synthetic tests. Return a concise Portuguese change/test/remaining-limitations report. Leave changes uncommitted.
```

## Prompt 5 — ChatGPT Work: artes e avaliação visual

```text
Create/refine original branding and a polished Android UI concept for Lúmen — Afinador cromático. Use the attached reference and current app screenshots, if provided. Dark #090B12, navy #141925, cyan #35E8FF, violet #A78BFA, pink #FF4FD8, mint #46F5AE. Restrained neon inspired by an electronics project gallery; no copied brand assets. All visible text must be Brazilian Portuguese. Brand motif: abstract tuning-fork L and waveform. Use a large E₂ / Mi readout, coherent 82,41 Hz / 0,0 cents at A4 440 Hz, and separate −24 dBFS level. Chromatic strip, real-app spectrum area, bottom centered tuning bar with Apertar / Afinado / Afrouxar. Tabs Afinador / Espectro / Bateria. No ads or pricing. Note color and tuning status must remain distinct.
Generate an artistic concept board with a primary screen and original icon concept. Treat the mockup as visual direction, not scientific evidence or a production screenshot. Do not paint fabricated spectra onto actual app screenshots as though measured. Production icons, text and graphs will be reconstructed as native/vector components by Codex. If reviewing current screenshots, prioritize readability, Portuguese terminology, font scaling, contrast and touch target sizes before decoration.
```

## Fontes técnicas iniciais

- Android AudioSource: https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
- Android, disponibilidade de UNPROCESSED e captura: https://developer.android.com/media/platform/mediarecorder
- YIN, artigo original: https://iro.umontreal.ca/~pift6080/H09/documents/papers/yin_pitch_tracker.pdf
- Material de implementação YIN: https://labrosa.ee.columbia.edu/doc/yin.html

Estas fontes fundamentam a direção inicial; a pesquisa e a implementação devem verificar compatibilidade, licenças e limitações específicas. Metas de precisão e desempenho acima são critérios propostos para teste, não resultados já obtidos.
