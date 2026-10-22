# Contribuindo

Use JDK 17, SDK 35 e o Wrapper incluído. Rode `./gradlew -PdspOnly :dsp:verifyDsp` (no Windows: `.\gradlew.bat -PdspOnly :dsp:verifyDsp`) e build/lint Android. Não altere critérios de precisão para encobrir falhas. Casos de oitava precisam testar E2 e E1 real; mudanças na análise devem preservar o espectro independente de acordes e o modo inarmônico de bateria.

Textos e documentação principal em português brasileiro; identificadores em inglês. Separe captura, DSP e interface. Não adicione rede/analytics/áudio persistido sem discussão de produto e privacidade. Não versione chaves, local.properties, ferramentas locais ou dados pessoais de gravações.

Para corrigir DSP, forneça sinal reprodutível, taxa real, janela, Hz esperado/medido, cents e teste de regressão que exercite produção. Diferencie resultados sintéticos de hardware. Arquivos de referência anexados são entradas do projeto, não ativos para copiar na interface.

Licença do projeto pendente do titular; obtenha sua decisão antes de redistribuir ou aceitar contribuições com termos incompatíveis. Não inventar identidade de copyright.
