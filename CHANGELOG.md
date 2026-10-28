# Histórico

## 0.2.0 — 27/10/2026

- Status curtos com detalhes separados e regiões de texto dimensionadas para evitar deslocamentos entre quadros.
- Nota aceita por confiança/histerese/persistência, frequência suavizada em log Hz e retenção explicitamente histórica, sem orientação de afinação durante falhas.
- Espectro musical até a 13ª maior (+21 semitons), referência estável/manual e faixa completa opcional; bateria conserva faixa completa.
- Normalização visual com gate e referência suavizada, limiar suave de ruído, traços atual/histórico e persistência por tempo, congelamento coerente e limpeza.
- Testes JVM dos filtros de produção e testes Compose para limites de layout/fonte ampliada. Resultados reais da execução em docs/TESTING.md.

## 0.1.0 — 22/10/2026

- Projeto Android Kotlin/Compose e módulo DSP JVM independente.
- AudioRecord em workers separados, permissão e lifecycle, diagnóstico e fila limitada.
- MPM por autocorrelação FFT, checagem espectral, confiança, validação temporal e alvo com histerese.
- FFT Hann medida, espectro logarítmico, picos, congelamento e retenção.
- Bateria experimental por golpe/decaimento/persistência, sem fundamental presumida.
- Configurações persistentes, PT-BR, cores cromáticas e ativos vetoriais originais.
- Testes sintéticos executados na JVM; Android ainda sem build/aparelho validados. CI preparada, sem execução remota.
