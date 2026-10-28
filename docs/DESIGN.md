# Design e procedência

Ativos originais, desenhados como paths: diapasão assimétrico com haste em L e onda central. `assets/lumen-logo.svg` é a matriz; `lumen-wordmark.svg` traz lettering principal em paths; `github-cover.svg` é capa gráfica, explicitamente identificada como arte, não screenshot. Textos secundários SVG usam sans-serif do visualizador. Nenhuma rasterização é necessária para a interface funcional.

Android usa VectorDrawable real, com viewport 108, separado de fundo e máscara adaptativa. Desenho principal dentro da região central segura, aproximadamente 27–81, com extremos do diapasão 29–80; stroke discreto. Versão monocromática em recurso v33, somente silhueta do diapasão. Os SVGs não foram renomeados para XML Android. Ícones sob máscaras redonda/quadrada e launcher real ainda precisam ser conferidos no aparelho.

Fundo #090B12, superfície #141925, texto #F4F7FF, secundário #A7B0C0, ciano #35E8FF, violeta #A78BFA, rosa #FF4FD8, verde #46F5AE e alerta #FFBC66. As 12 cores seguem a tabela do documento original. Cor de nota representa classe cromática; texto e posição da barra representam afinação. Não se alega reproduzir exatamente a paleta de uma galeria externa.

Layout em coluna rolável: marca/referência/configurações, modos, permissão/status, faixa cromática, nota, métricas, alvo, espectro, barra. Métricas em linhas largas favorecem fonte ampliada e números monoespaçados. Modo Espectro amplia o gráfico. Botões com toque mínimo de 48 dp; controles Material têm dimensão acessível. Scroll mantém conteúdo acessível em telas pequenas; a barra acompanha o conteúdo, não fica fixa na viewport. Pode exigir rolagem em aparelhos pequenos.

Texto acompanha cores, espectro tem descrição e lista textual de picos. Estados indicados por texto; status com live region moderada. Sem brilho forte, falsas ondas ou fake measurements. TalkBack, fonte 200%, contraste em dispositivos e controles rotulados precisam de teste manual; não foram declarados aprovados. Checklist em TESTING.

Na versão 0.2.0, `StableText` reserva a altura das variantes na largura/escala de fonte atuais; a lista de picos mantém linhas mesmo sem dados. Status curtos têm orientações em diálogo separado. Cor/opacidade transitam em 120 ms, sem animação de tamanho. Leitura retida é explicitamente histórica. O espectro distingue atual ciano e histórico violeta; eixo musical e tempos estão documentados em [PRESENTATION](PRESENTATION.md). Testes Compose de limites foram adicionados para 320 dp e fonte 100%/200%; a execução precisa de Android.

As duas imagens fornecidas foram usadas como direção estética/funcional, sem inclusão como tela do app ou cópia dos logos. Documentos e anexos originais foram preservados. Não há assets de terceiros incorporados aos vetores novos. A licença dos novos ativos permanece pendente junto ao restante do código original.
