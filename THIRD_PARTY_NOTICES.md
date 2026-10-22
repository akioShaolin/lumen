# Dependências, licenças e procedência

Nenhuma biblioteca DSP externa é distribuída: FFT/MPM/matemática e testes foram implementados em Kotlin neste projeto. Artigos definem os métodos; não houve cópia de código do TarsosDSP, FFTW ou JTransforms.

| Componente | Versão | Licença/fonte |
|---|---|---|
| Kotlin stdlib/plugins | 2.1.20 | Apache-2.0, https://github.com/JetBrains/kotlin |
| Android Gradle Plugin | 8.9.2 | Apache-2.0, https://android.googlesource.com/platform/tools/base/ |
| Gradle e Wrapper | 8.11.1 | Apache-2.0, https://github.com/gradle/gradle |
| Compose BOM | 2025.04.01 | AndroidX Apache-2.0, https://android.googlesource.com/platform/frameworks/support/ |
| Activity Compose | 1.10.1 | AndroidX Apache-2.0 |
| Lifecycle | 2.9.0 | AndroidX Apache-2.0 |
| Compose Material3/UI | BOM fixada | AndroidX Apache-2.0 |
| Coroutines transitivas | geridas por AndroidX | Apache-2.0, https://github.com/Kotlin/kotlinx.coroutines |

Wrapper baixado da tag oficial v8.11.1; SHA-256 do JAR: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`. Scripts oficiais preservam seus cabeçalhos de licença. Dependências transitivas serão resolvidas no primeiro build Android; o grafo Android completo ainda não foi produzido. Gere-o com `.\gradlew.bat :app:dependencies` e confira avisos antes de distribuir.

Ferramentas temporárias de teste em `.tools/` são ignoradas e não fazem parte do app: compilador embeddable/stdlib/script-runtime Kotlin 2.1.20; kotlin-reflect 1.6.10; trove4j 1.0.20200330; coroutines-core-jvm 1.8.0; annotations 13.0. Trove4j tem termos LGPL-2.1 (consulte https://github.com/JetBrains/intellij-deps-trove4j); annotations Apache-2.0 (https://github.com/JetBrains/java-annotations). Essas ferramentas não devem ser redistribuídas com o código/app. Repositório de download: Maven Central.

Ativos novos: `assets/*.svg` e VectorDrawable Android, autoria original desta implementação, sem importação de marcas alheias. Não há licença de projeto escolhida; MIT ou Apache-2.0 são recomendações, dependentes da decisão do titular. Fontes visuais fornecidas permanecem anexos com direitos não avaliados para redistribuição.
