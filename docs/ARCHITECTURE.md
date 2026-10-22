# Arquitetura

Fluxo unidirecional: ações Compose → ViewModel → configurações/controle de captura → estado imutável via StateFlow → Compose. Preferências locais privadas persistem Lá4, tolerância, limiar, enarmonia, alvo, preset e parâmetros de bateria. O modo de tela não é persistido. Uma instância de captura por ViewModel.

`CaptureEngine` usa PCM16 mono e tenta 48 kHz, depois 44,1 kHz. Confere a propriedade de suporte a UNPROCESSED antes de solicitá-lo; alternativas são VOICE_RECOGNITION e MIC. Verifica inicialização e usa `AudioRecord.sampleRate` real. Fonte solicitada, rota, taxa e perdas ficam visíveis; nada certifica que o fabricante removeu todo processamento.

Dois workers: captura bloqueante e análise. Dez blocos reutilizáveis de 1024 amostras, fila limitada a oito, ring de 32768. Se o consumidor atrasar, blocos são descartados e a perda é contada. Não existe fila crescente. Leituras parciais são acumuladas. Efeitos AGC/NS/AEC são desabilitados quando disponíveis e autorizados; falhas de efeitos opcionais não invalidam a entrada. Captura e buffers ficam fora da thread principal. Resultados e listas FFT alocam quantidades limitadas por quadro; não se afirma ausência total de alocações.

Análise a cada 4096 novas amostras (aproximadamente 85–93 ms), independente da leitura de 1024. Janelas de 8192 e 32768, escolhendo a longa para graves/ausência de pitch/bateria quando disponível. O estado publicado usa esse limite; o ritmo não depende da recomposição Compose. O estado não é um stream de áudio persistente.

Lifecycle ON_PAUSE cancela leitura, interrompe worker e libera microfone no finally. O controlador aguarda encerramento antes de reiniciar. Permissão é reavaliada em ON_RESUME; recusa tem caminho para configurações. Erro de captura produz estado explícito, sem números anteriores. Android 10+ sinaliza captura silenciada por disputa por meio de AudioRecordingCallback; mudança de rota invalida o histórico de análise. As APIs são implementadas, mas não exercitadas em Android nesta máquina. Não há foreground service.

`PitchDetector`, `SpectrumAnalyzer`, `Music`, `TargetTracker` e `DrumAnalyzer` são Kotlin/JVM puro. Os testes chamam exatamente essas classes. Não há cópia Python/JavaScript usada para validar uma implementação diferente.

`DrumAnalyzer` tem estados de espera, descarte de ataque, coleta de decaimento e resultado retido. O contexto Pele/parafuso versus Som global orienta a coleta humana, não aplica uma fórmula de membrana presumida. O alvo espectral selecionado é conservado entre golpes; o detector de cordas não decide a ressonância do tambor.
