# Avisos de terceiros

As bibliotecas e fontes utilizadas mantêm suas próprias licenças. As dependências são resolvidas pelo Gradle; suas fontes e avisos também podem ser consultados nos projetos originais. As versões diretas estão no catálogo `gradle/libs.versions.toml`.

| Componente | Licença / origem |
| --- | --- |
| Kotlin, coroutines e serialization | [Apache 2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) |
| AndroidX: Compose, Material 3, Lifecycle, Navigation, Room, DataStore e Core | [Apache 2.0](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt) |
| Dagger / Hilt | [Apache 2.0](https://github.com/google/dagger/blob/master/LICENSE.txt) |
| PDFBox Android | [Apache 2.0](https://github.com/TomRoush/PdfBox-Android/blob/master/LICENSE) |
| kotlin-csv | [Apache 2.0](https://github.com/doyaaaaaken/kotlin-csv/blob/master/LICENSE) |
| Bouncy Castle: bcprov, bcpkix e bcutil (dependências do PDFBox) | [Licença permissiva do Bouncy Castle](https://www.bouncycastle.org/licence.html) |
| Manrope | SIL Open Font License 1.1; [aviso incluído](app/src/main/assets/licenses/Manrope-OFL.txt) |
| Fraunces | SIL Open Font License 1.1; [aviso incluído](app/src/main/assets/licenses/Fraunces-OFL.txt) |

Os textos [Apache 2.0](app/src/main/assets/licenses/Apache-2.0.txt) e [Bouncy Castle](app/src/main/assets/licenses/Bouncy-Castle.txt) também estão nos assets do aplicativo. Essa lista destaca os componentes principais e não substitui os avisos dos artefatos transitivos.

JUnit, ferramentas Android/Gradle, testes AndroidX e ações do GitHub têm seus próprios avisos nos respectivos projetos. Pillow e resvg-py são dependências opcionais do script de geração de ícones em `brand/`; não são dependências do APK.

A marca e o código autoral do projeto são distintos das licenças de terceiros. Consulte a seção de licença do README para o estado atual de autorização de reutilização.
