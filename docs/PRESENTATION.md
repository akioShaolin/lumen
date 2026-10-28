# Apresentação estável e espectro musical — 0.2.0

## Referência e escopo

O vídeo fornecido em 27/10/2026 tem aproximadamente 3 min 30 s. Foram inspecionados quadros distribuídos pelo início e por trechos de 35–200 s: notas, alvo manual C4, alternância de confiança, gráfico de 20 Hz–5 kHz e ruído elevado no desenho. O texto antigo já dizia dB relativos ao máximo do quadro; a mudança implementa outro processamento visual, não apenas troca o rótulo. Não foi fornecido um novo APK junto ao texto/vídeo desta solicitação, portanto não foi possível comparar binários. O vídeo é referência de comportamento; não prova exatidão acústica.

Nenhum processamento desta página altera PCM, RMS dBFS, a seleção MPM ou os picos medidos. O app continua offline, sem anúncios e sem permissão de Internet.

## Leitura e status

`PitchPresentation` recebe o pitch DSP e tempo monotônico em milissegundos. Exige confiança ≥0,90, ausência de ambiguidade e de saturação. Uma nota diferente deve persistir por 300 ms; evidência é zerada ao trocar o candidato, receber entrada inválida ou ter intervalo entre quadros maior que 180 ms. A fronteira usa histerese de 68 cents em torno da nota aceita. Assim, a nota aceita pode permanecer perto da fronteira enquanto os cents mostram o desvio real.

Dentro da nota aceita, o filtro opera em `ln(Hz)`, com `α = 1 − exp(−Δt/140 ms)`. A frequência exibida e os cents usam o mesmo resultado filtrado. O alvo automático é a frequência nominal da nota aceita, na referência Lá4 atual. O alvo manual é independente. Uma troca confirmada inicializa diretamente a nova frequência, sem percorrer notas intermediárias nem dobrar/dividir o pitch. As proteções de oitava do DSP permanecem em uso.

Durante troca de candidato ou baixa confiança, o último valor é marcado **Leitura retida**, fica menos opaco e perde imediatamente a orientação mecânica/Afinado. A retenção termina após 500 ms desde a última leitura aceita. Ausência de novos quadros por mais de 180 ms também deixa de ser ao vivo. Um watchdog de 50 ms limpa os valores mesmo se a captura parar de produzir callbacks. O último referencial musical pode permanecer para manter o eixo fixo; isso não é uma medição atual.

`StatusPresentation` exige 180 ms de persistência para status comuns. Saturação e erro de captura são imediatos. As mensagens visíveis são curtas; detalhes ficam em **Sobre o sinal**, sem trocar linhas durante a captura.

`StableText` mede a maior altura das variantes com a largura, estilo e escala de fonte atuais. Nota, leituras, alvo, orientação e lista espectral mantêm espaço para valores válidos e traços. Linhas dos picos têm quantidade fixa por modo. Não há rolagem automática nem animação de altura; cores/opacidade usam 120 ms. A tela continua rolável. Fonte monoespaçada e precisão constante evitam saltos de largura; duas casas em Hz não representam garantia de exatidão física.

## Eixo horizontal

Modo musical: `f_ref` é o alvo manual ou a frequência nominal da nota aceita. Exibe de `f_ref × 2^(−1/12)` (margem inferior de um semitom) até `f_ref × 2^(21/12)`, limitado ao Nyquist real. **13ª maior = +21 semitons ≈ 3,364 × f_ref**, não 13º harmônico. Posição: `ln(f/f_min) / ln(f_max/f_min)`. Marcas 1, 3M, 5J, 8J, 10M e 13M usam suas posições musicais verdadeiras. Marcas intermediárias podem ser omitidas quando faltaria espaço com fonte ampliada; extremos e valores em Hz continuam disponíveis.

O eixo não acompanha o pitch ruidoso de cada quadro. Antes da primeira confirmação, aguarda uma referência; em silêncio, conserva a última. Para acordes, escolha um alvo manual por nota ou Hz. **Faixa completa** mostra 20 Hz até Nyquist. Bateria usa sempre a faixa completa, mantendo sua interpretação experimental.

## Eixo vertical, ruído e história

`SpectrumDisplay` guarda amplitudes em suas frequências absolutas. A referência vertical ataca imediatamente um máximo maior e libera com constante de 1000 ms, apenas em quadros acima do limiar RMS configurado. Na liberação há piso de `gate + 12 dB`. Silêncio não rebaixa a referência para transformar ruído em pico de 0 dB. Há epsilon de amplitude `10⁻¹²`, sanitização de valores não finitos e limite visual de −60 a 0 dB.

**Realce dos picos** estima o piso amplo pela mediana de até 512 bins amostrados uniformemente. Aplica somente ao desenho a atenuação suave `clamp(0,75 × (piso + 9 − nível_do_bin), 0, 18) dB`. Não classifica um pico persistente como musical: hum elétrico também pode persistir. O traço é uma visualização limiarizada relativa à referência suavizada, não dBFS nem valores brutos de cada pico. Não há compressão adicional de coordenadas. A lista numérica permanece na FFT original, explicitamente relativa ao máximo daquele quadro, sem atenuação visual.

Traço atual ciano: ataque de 35 ms e liberação de 280 ms, calculados com tempo decorrido. Histórico violeta: retém por 1000 ms e depois decai 16 dB/s. Somente sinal real renova a retenção; o próprio traço em liberação não a renova. Silêncio deixa o traço decair. Ao reduzir bins para pixels, conserva-se o máximo de cada coluna, em vez de diluir picos estreitos pela média. Nenhum pico é inventado nem deslocado para uma nota.

Trocar referência só muda a projeção dos bins absolutos; não relabela frequências antigas. Mudanças de tamanho/resolução FFT em sinal ativo reiniciam o histórico; uma troca de janela durante silêncio deixa o traço anterior decair. Reiniciar microfone, trocar rota/taxa ou modo limpa a apresentação e o congelamento. **Congelar** guarda espectro, lista, eixo e referência juntos; não congela a captura. **Limpar picos retidos** também libera o congelamento.

## Verificação e limites

Testes JVM exercitam os filtros de produção com relógio controlado: fronteiras, candidatos descontínuos, troca real, E1/E2, retenção/expiração, coerência Hz/cents/alvo, ruído, parcial fraco, tempo de retenção/liberação em duas taxas e projeção dos bins. Testes Compose em `app/src/androidTest` verificam limites dos componentes reais durante alternância de textos/leituras em 320 dp e escalas 100%/200%, além de acesso por rolagem. Compilar esses testes não equivale a executá-los; consulte o resultado da sessão em [TESTING](TESTING.md).

Os limiares são heurísticos, ajustáveis em parâmetros nomeados do código. Processamento lento, janelas graves de até 743 ms e contenção de CPU podem aumentar a aquisição ou interromper a evidência contínua. Polifonia e fundamental ausente continuam ambíguas. O vídeo não é um teste automatizado de microfone, e nenhum resultado novo em aparelho físico foi presumido.
