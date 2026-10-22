# Pesquisa Técnica e Arquitetura DSP: Projeto Lúmen

**Data de Acesso às Fontes:** 22 de Outubro de 2026  
**Escopo:** Afinador de alta precisão offline para Android em Kotlin nativo, sem dependências de nuvem ou telemetria.

---

## 1. Captura de Áudio no Android

### Fonte de Áudio: UNPROCESSED e Fallbacks
* **Evidência:** A API Android MediaRecorder/AudioRecord define `MediaRecorder.AudioSource.UNPROCESSED` (adicionado no API level 24). A documentação oficial do Android para áudio bruto e CDD (Android Compatibility Definition Document) estabelece que `UNPROCESSED` visa fornecer áudio de microfone sem cancelamento de eco (AEC), controle automático de ganho (AGC) ou redução de ruído (NS). No entanto, nem todos os dispositivos possuem suporte de hardware/HAL certificado para `UNPROCESSED`. O comportamento em dispositivos sem suporte direto é regido pelo *fallback* automático do sistema para `AudioSource.DEFAULT` ou falha na inicialização do `AudioRecord`.
* **Recomendação de Projeto:** Inicializar o `AudioRecord` tentando primariamente `AudioSource.UNPROCESSED`. Capturar exceções do tipo `IllegalArgumentException` ou verificar o estado `AudioRecord.STATE_INITIALIZED`. Caso a inicialização falhe, executar o *fallback* gracioso na seguinte sequência: `AudioSource.VOICE_RECOGNITION` (frequentemente possui pré-processamento mínimo em comparação a `VOICE_COMMUNICATION`) e, por fim, `AudioSource.DEFAULT`.

### Lifecycle, Coroutines e Permissões
* **Evidência:** O acesso ao microfone exige a permissão em tempo de execução `android.permission.RECORD_AUDIO`. No Android 14/15/16, serviços em primeiro plano para captura de áudio exigem a declaração do tipo `foregroundServiceType="microphone"`.
* **Recomendação de Projeto:** Integrar o fluxo de captura com o ciclo de vida do Compose (`DisposableEffect` ou `LifecycleEventObserver`). Inicializar a captura dentro de uma Coroutine vinculada ao `ViewModel` (`viewModelScope`) utilizando `Dispatchers.IO` dedicado e laços de leitura bloqueantes (`AudioRecord.read()`). Garantir o encerramento do *loop*, encerramento e liberação (`stop()` e `release()`) do `AudioRecord` no cancelamento do Job para evitar vazamento do recurso do sistema.

---

## 2. Parâmetros de Áudio, Latência e Janelamento

### Tradeoffs: Taxa de Amostragem vs. Tamanho de Janela
* **Evidência:** A resolução em frequência da FFT é dada por $\Delta f = \frac{f_s}{N}$, onde $f_s$ é a taxa de amostragem e $N$ o tamanho do buffer/janela. Para detectar notas graves como $E_1 \approx 41.2034\text{ Hz}$, a diferença entre $E_1$ e $F_1$ ($43.65\text{ Hz}$) é de apenas $2.45\text{ Hz}$.
* **Análise Matematica:**
  * Em $f_s = 44100\text{ Hz}$ e $N = 4096$, $\Delta f = 10.77\text{ Hz}$ (insuficiente para separação direta por FFT sem interpolação).
  * Em $N = 8192$, $\Delta f = 5.38\text{ Hz}$. O tempo de acumulação da janela é $\frac{8192}{44100} \approx 185.7\text{ ms}$.
  * Para estimativa no domínio do tempo (YIN/MPM), $N = 4096$ em $f_s = 44100\text{ Hz}$ cobre um atraso máximo ($\tau$) de $92.8\text{ ms}$, permitindo capturar pelo menos 3 períodos completos de uma onda de $41.2\text{ Hz}$ ($\sim 24.27\text{ ms}$ por período).
* **Latência vs. Estabilidade:** O atraso total do pipeline é determinado por: $\text{Latência Total} = \text{Buffer HAL} + \text{Tempo de Preenchimento da Janela} + \text{Processamento DSP}$. Usar *hop size* de $50\%$ a $75\%$ (sobreposição de janelas) permite atualizar a interface do usuário a cada $20\text{–}40\text{ ms}$, mantendo a resolução temporal do display sem sacrificar a janela de integração dos algoritmos.

---

## 3. Estimativa de Pitch e Resolução de Otavas

### Comparação YIN vs. MPM
* **Evidência YIN (de Cheveigné & Kawahara, 2002):** Utiliza a Função de Diferença Média Cumulativa Normalizada ($d'(\tau)$). Reduz erros de sub-otava ao normalizar a função de diferença e aplicar um limiar absoluto (tipicamente entre $0.10$ e $0.15$) para selecionar o primeiro mínimo local abaixo do limiar, priorizando o período fundamental em vez de seus harmônicos.
* **Evidência MPM / McLeod Pitch Method (McLeod & Wyvill, 2005):** Utiliza a Função de Autocorrelação Quadrática Normalizada (NSDF). O MPM calcula uma pontuação de "clareza" ( amplitude dos picos da NSDF, de $-1$ a $+1$). A seleção de pitch utiliza um limiar relativo (ex: $80\%$ do maior pico) para escolher o primeiro pico temporalmente válido, reduzindo drasticamente erros de otava em sinais com harmônicos proeminentes (como cordas de violão e baixo).

### Validação Espectral Cruzada
* **Recomendação de Projeto:** Utilizar o MPM ou YIN como estimador primário no domínio do tempo. Implementar validação cruzada no domínio da frequência: dada a frequência estimada $f_0$, verificar na FFT se existem picos significativos em $f_0 / 2$. Se o pico em $f_0 / 2$ possuir energia relativa acima do limiar e apresentar estrutura harmônica condizente, corrigir o *octave error*.

### Limitações da Fundamental Ausente (*Missing Fundamental*)
* **Evidência:** Em instrumentos como o baixo elétrico ou microfones acoplados a celulares, a frequência fundamental $f_0$ de $E_1$ ($41.2\text{ Hz}$) pode estar atenuada em mais de $-30\text{ dB}$ em relação ao segundo ou terceiro harmônico ($2f_0 = 82.4\text{ Hz}$, $3f_0 = 123.6\text{ Hz}$).
* **Impacto:** Algoritmos baseados unicamente em autocorrelação simples falham travando em $82.4\text{ Hz}$ ($E_2$). O MPM e a análise de padrão de espaço entre harmônicos na FFT ($\Delta f = f_{k} - f_{k-1} \approx f_0$) são essenciais para deduzir a fundamental ausente.

---

## 4. Acústica, Calibração e Análise de Tambores

### Diferença Crítica: dBFS vs. dB SPL
* **Evidência:** 
  * $\text{dBFS}$ (Decibels relative to Full Scale) é uma unidade digital estritamente delimitada pelo ponto de saturação do conversor A/D ($0\text{ dBFS}$). O valor em $\text{dBFS}$ depende unicamente da amplitude dos amostradores PCM no código ($16\text{-bit int}$ ou $32\text{-bit float}$).
  * $\text{dB SPL}$ (Sound Pressure Level) é uma medição física da pressão acústica relativa a $20\ \mu\text{Pa}$. É **impossível** converter $\text{dBFS}$ para $\text{dB SPL}$ sem calibração prévia do hardware com um decibelímetro externo padrão de referência, pois o ganho do pré-amplificador e a sensibilidade da cápsula do microfone do Android variam entre fabricantes e modelos.

### Modos de Membrana de Tambores vs. Cordas
* **Evidência (Fletcher & Rossing, *The Physics of Musical Instruments*):**
  * **Cordas:** Ressonam em frequências harmônicas, que são múltiplos inteiros da fundamental ($f_1, 2f_1, 3f_1, 4f_1 \dots$).
  * **Membranas Circulares (Tambores):** Modos de vibração são governados por Funções de Bessel da primeira espécie ($J_n(kr)$) e **não são harmônicos**. Os modos são denotados por $(n, m)$, representados por razões não-inteiras em relação ao modo fundamental $(0,1)$:
    * Modo $(0,1)$ (Fundamental): $1.000$
    * Modo $(1,1)$: $1.593 \times f_{0,1}$
    * Modo $(2,1)$: $2.135 \times f_{0,1}$
    * Modo $(0,2)$: $2.295 \times f_{0,1}$
* **Recomendação de Projeto:** A interface do Lúmen para análise de tambores não deve tentar associar notas musicais (C, D, E) ou cents fixos por escala temperada de forma simplificada. Deve exibir um gráfico espectral destacando os picos ressonantes medidos e identificando as razões dos modos de membrana em relação à frequência do pico principal.

---

## 5. Processamento FFT, Normalização e Interpolação

### Normalização da FFT
* Para uma FFT de tamanho $N$, a magnitude real de um componente de frequência é calculada por:
  $$\text{Magnitude}(k) = \frac{2 \cdot |X[k]|}{N}$$
* Para conversão em dBFS:
  $$\text{dBFS}(k) = 20 \cdot \log_{10}\left( \frac{\text{Magnitude}(k)}{\text{Magnitude}_{\text{max}}} \right)$$

### Interpolação Parabólica de Picos
* **Evidência:** Para superar a limitação do tamanho do *bin* da FFT sem aumentar o custo computacional com janelas excessivamente grandes, aplica-se a interpolação parabólica sobre os valores logarítmicos de magnitude dos três *bins* ao redor do pico ($k-1$, $k$, $k+1$):
  $$\delta = \frac{1}{2} \cdot \frac{\alpha - \gamma}{\alpha - 2\beta + \gamma}$$
  Onde $\alpha = 20\log_{10}|X[k-1]|$, $\beta = 20\log_{10}|X[k]|$, e $\gamma = 20\log_{10}|X[k+1]|$.
* **Frequência Refinada:** $f_{\text{estimada}} = (k + \delta) \cdot \frac{f_s}{N}$.

### Resposta de Baixa Frequência dos Microfones
* **Evidência:** A maioria dos microfones MEMS integrados em smartphones possui filtros passa-alta de hardware/firmware projetados para atenuação abaixo de $50\text{–}80\text{ Hz}$ (para cortar ruído de vento e manuseio). *Declaração Unverified:* Testes de sensibilidade a $20\text{ Hz}$ dependem exclusivamente da resposta do componente de hardware do dispositivo específico; o software não pode recuperar frequências atenuadas abaixo do piso de ruído elétrico.

---

## 6. Comparativo de Bibliotecas DSP para Kotlin/Android

| Critério | **JTransforms** | **TarsosDSP** | **KMath (Kotlin Official)** |
| :--- | :--- | :--- | :--- |
| **Licença** | BSD-2-Clause | GPLv3 / Comercial | Apache 2.0 |
| **Manutenção / Estado** | Estável (Legado Java, amplamente utilizado) | Baixa atividade recente | Ativo (Projeto JetBrains) |
| **Viabilidade Pure-Kotlin** | Requer Java/JVM Bytecode (ou reescrita) | Requer adaptação do I/O Java (`javax.sound`) | Nativamente Kotlin Multiplatform |
| **Foco de Recursos** | Apenas cálculos FFT de altíssima performance | Algoritmos de Áudio (YIN, MPM, Onset) | Operações matemáticas genéricas e matrizes |

* **Recomendação de Arquitetura:** Para manter o projeto Lúmen livre de restrições de licença copyleft (GPL) e maximizar a independência, recomenda-se a implementação em Kotlin Nativo puro dos algoritmos MPM/YIN e das rotinas de FFT (ou binding leve com a biblioteca `JTransforms` / `FFTW` via NDK em C++ caso performance seja crítica).

---

## 7. Matriz de Testes Reprodutíveis

Para validação técnica e regressão do motor DSP do Lúmen, os seguintes testes sintéticos e com sinais reais devem ser executados:

1. **Teste de Discriminação de Otava ($E_2$ vs $E_1$):**
   * *Sinal:* Senoide pura em $82.4069\text{ Hz}$ ($E_2$) versus senoide pura em $41.2034\text{ Hz}$ ($E_1$).
   * *Objetivo:* Garantir que a estimativa retorne exatamente a nota correspondente sem desvios de $\pm 1200\text{ cents}$.
2. **Entrada Dominante em Harmônicos (Baixo Elétrico / Violão):**
   * *Sinal:* Onda complexa com fundamental em $41.2\text{ Hz}$ atenuada a $-20\text{ dB}$ e segundo harmônico em $82.4\text{ Hz}$ a $0\text{ dB}$.
   * *Objetivo:* Verificar se a combinação MPM/Validação Espectral infere corretamente a fundamental em $41.2\text{ Hz}$.
3. **Sinal Polifônico / Acordes:**
   * *Sinal:* Acorde de Mi Maior ($E_2, B_2, E_3, G\#_3, B_3, E_4$).
   * *Objetivo:* O afinador monofônico não deve oscilar descontroladamente; a exibição do espectro FFT deve mostrar com precisão os picos individuais de cada nota do acorde.
4. **Sinal Inarmônico Amortecido (Tambor/Tom-tom):**
   * *Sinal:* Gravação de impacto de baqueta em pele de tambor com decaimento rápido ($<500\text{ ms}$) e frequências não-múltiplas.
   * *Objetivo:* Testar se o módulo de ressonância captura os modos ressonantes sem travar a estimativa em uma escala temperada musical.

---

## 8. Referências Primárias e Documentação

1. **Android Audio Source Documentation:**
   * URL: `https://developer.android.com/reference/android/media/MediaRecorder.AudioSource` (Acesso em Oct 2026)
2. **YIN Pitch Estimation Paper:**
   * De Cheveigné, A., & Kawahara, H. (2002). *YIN, a fundamental frequency estimator for speech and music*. The Journal of the Acoustical Society of America, 111(4), 1917-1930.
   * DOI: `10.1121/1.1458024`
3. **McLeod Pitch Method Paper:**
   * McLeod, P., & Wyvill, G. (2005). *A Smarter Way to Find Pitch*. Proceedings of the International Computer Music Conference (ICMC).
   * URL: `https://www.cs.otago.ac.nz/graphics/Geoff/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf`
4. **Acoustics of Drumhead Modes:**
   * Fletcher, N. H., & Rossing, T. D. (1998). *The Physics of Musical Instruments*. Springer Science & Business Media.
   * Rossing, T. D. (1992). *Acoustics of Drums*. Physics Today, 45(3), 40-47.