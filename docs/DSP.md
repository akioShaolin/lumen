# DSP: definições e limites

## Nível e preparação

PCM16 normalizado por 32768. RMS digital calculado sobre amostras originais, `20 log10(max(sqrt(mean(x²)), 10⁻⁶))`, piso −120 dBFS. Uma senoide de amplitude 0,5 produz aproximadamente −9,031 dBFS RMS. Saturação é marcada a partir de módulo 0,999; ela bloqueia orientação de afinação.

Para periodicidade/FFT é subtraída a média da janela; não há passa-altas agressivo. Esta operação remove DC por quadro e não é um filtro LTI com resposta fixa: equivale à projeção que elimina a componente constante daquela janela. Pode alterar sinais de frequência muito baixa e janelas incompletas; não corta explicitamente 20 Hz. O nível RMS ainda inclui DC; um sinal constante não vira nota.

Gate inicial −65 dBFS, ajustável de −85 a −35. O piso adaptativo inicia em −85 e só se move para observações próximas do piso; gate efetivo é o maior entre o configurado e piso+10 dB. É conservador, não um estimador universal de ruído ambiente. Sensibilidade altera limiar, não resposta do microfone. Bloco recente abaixo do limiar elimina pitch antigo antes de drenar a janela longa.

## Periodicidade e oitavas

MPM/NSDF: autocorrelação linear calculada por FFT de tamanho 2N, com zero padding que evita correlação circular; denominador usa energia das duas faixas sobrepostas. Custo O(N log N), sem busca O(N²) em janela longa. Picos locais entre atrasos correspondentes a 20–2000 Hz; seleciona o primeiro acima de 0,80 e de 97% do melhor pico. Refina atraso por parábola de três amostras.

O período mais curto com suporte evita selecionar um período duplicado para E2. Uma proposta f/2 só pode substituir f se houver pico de periodicidade melhor, atraso compatível e energia em harmônicos ímpares da proposta, não só pares. O candidato 2f é considerado implicitamente pelos picos de menor atraso e pelo corte relativo; não é obtido por duplicar arbitrariamente todo grave. Fundamental fraca, combinação de 2º e 3º harmônicos e fundamental ausente entram nos testes de E1 e E2. Um espectro composto só por 2f e 4f é indistinguível de outro com fundamental 2f: retorna a periodicidade observável, sem inventar f.

FFT independente mede energia não explicada pela grade harmônica. Energia significativa fora dela, ausência da fundamental, periodicidade <0,90 ou saturação reduzem confiança e bloqueiam instrução. Esses limiares são heurísticos, não probabilidade calibrada nem garantia contra toda polifonia. Alvo manual não altera o pitch medido. Nenhuma oitava é forçada pelo preset. O DSP entrega a estimativa bruta; uma camada separada suaviza apenas a apresentação e confirma trocas por tempo. `PitchStability` exige confirmação em outro quadro após salto maior que 600 cents, mantendo Hz medido e reduzindo confiança temporariamente; silêncio/rota nova zeram o histórico. Essa validação temporal não muda a seleção espectral dos candidatos.

Alvo automático acompanha a nota aceita por `PitchPresentation`: confiança mínima 0,90, histerese de 68 cents e confirmação de troca por 300 ms. Alvo manual conserva Hz. `m = 69 + 12 log2(f/A4)`, `f_target = A4 2^((m_target−69)/12)`, `cents = 1200 log2(f/f_target)`. A4=MIDI69; E1=28; E2=40. Cents não são reduzidos módulo 100. Barra limita posição, mantém número real e indica erro fora da escala. Perdas breves retêm valores por até 500 ms, identificados como históricos e sem orientação; depois, valores dependentes são traços.

## Janelas e espectro

8192 amostras: 171/186 ms em 48/44,1 kHz. 32768: 683/743 ms. Hop 4096: sobreposição 50%/87,5%. Graves podem aparecer inicialmente com janela curta até existir histórico longo. Latência inclui hardware, preenchimento e CPU; taxa da UI não elimina esse atraso.

FFT radix-2 original em Kotlin, janela Hann periódica `0,5−0,5 cos(2πn/N)`. Amplitude unilateral `2|X[k]|/sum(window)` para bins internos; DC e Nyquist usam fator 1. É amplitude digital de pico por bin, não RMS global. Picos são máximos locais maiores que 2,5% do máximo; parábola de log magnitude refina Hz. A amplitude relatada continua sendo a do bin, sujeita a scalloping. A lista numérica usa dB relativos ao máximo do quadro original. O desenho usa referência suavizada com gate e limiar visual suave opcional, −60..0 dB. Retenção de 1000 ms e decaimento de 16 dB/s usam tempo real. Eixo musical com referência estável até +21 semitons; faixa completa disponível e obrigatória na bateria. Fórmulas e limites em [PRESENTATION](PRESENTATION.md).

Espaçamento: fs/N (1,465 Hz com 32768 a 48 kHz). Interpolação permite estimar a frequência de uma senoide isolada abaixo desse espaçamento; não separa duas frequências arbitrariamente próximas. Zero padding da autocorrelação não aumenta resolução física. Notas dos picos são referências próximas calculadas com Lá4 escolhido; frequência do pico não depende da seleção de pitch. Acorde C4+E4+G4 deve apresentar três máximos medidos, sem rótulos de fundamental/terça/quinta automáticos.

## Bateria experimental

Golpe: subida >9 dB e nível >−48 dBFS; ignora 20–150 ms iniciais, padrão 50 ms. Coleta 32768 amostras pós-início. Compara picos do segmento completo com as duas metades, conservando frequências presentes nas duas dentro de duas larguras de bin de 16384. Isso mede persistência aproximada, não identifica modos físicos de Bessel. Lista até oito ressonâncias de 20–2500 Hz, retém resultado, alvo escolhido por Hz e golpe anterior selecionado. Sem seleção, nenhuma sequência é forçada a seguir o maior pico.

Graves <40 Hz ou ausência de picos persistentes sinalizam baixa confiança. Não há nota universal de tambor nem garantia para prato/ruído de caixa. A referência de cents/nota não é afirmação de fundamental musical. Sensibilidade a 20 Hz só pode ser demonstrada com equipamento e caracterização apropriados.

Fontes e decisões: [DECISIONS](DECISIONS.md). Critérios e evidência: [TESTING](TESTING.md).
