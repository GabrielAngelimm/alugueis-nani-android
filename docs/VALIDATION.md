# Validação da revisão

Verificação local concluída em **1 de outubro de 2026**, antes da publicação. Os comandos abaixo descrevem o resultado observado; a matriz não representa cobertura de todos os aparelhos ou de todos os extratos bancários.

## Ambiente

- Windows / PowerShell.
- Gradle Wrapper 9.1.0, AGP 9.0.1, Kotlin 2.2.10 e toolchain Java/Kotlin 17.
- JVM do Gradle local: JDK 21; CI configurado com JDK 17.
- Compile/target SDK 35, min SDK 26, Build Tools 36.0.0.
- Testes Android locais: emulador `emulator-5554`, API 37.
- Workflow remoto: Ubuntu com emulador isolado API 35.

## Resultados locais

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest` | **35 testes**, sem falhas, erros ou testes ignorados |
| `assembleDebug` | APK compilado |
| `assembleRelease` | APK release compilado, sem assinatura de distribuição |
| `assembleDebugAndroidTest` | APK instrumentado compilado |
| Testes instrumentados | **45 testes**, todos passaram |
| `lintDebug` | Sem erros; **27 avisos** mantidos visíveis |
| Compilação a partir do índice Git em pasta limpa | Passou com cache de build desativado: 134 tarefas executadas |
| Consulta de vulnerabilidades OSV | 150 coordenadas Maven do runtime verificadas; nenhum alerta retornado após a correção do Bouncy Castle |
| Arquivos destinados à publicação | Verificador de caminhos e credenciais conhecido passou; nomes privados antigos ausentes |
| Capturas públicas | Três imagens de testes com banco isolado, inspecionadas visualmente |
| Preservação no emulador | Registros das três tabelas de produção e preferências efetivas iguais antes/depois dos testes |

### Compilação e lint

```powershell
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease --max-workers=2 --stacktrace
./gradlew.bat lintDebug --no-configuration-cache --max-workers=1
```

A cópia formada pelos arquivos do índice Git também foi compilada sem `local.properties`, dados operacionais ou arquivos gerados preexistentes, fornecendo apenas `ANDROID_HOME` e os JDKs do ambiente:

```powershell
./gradlew.bat clean testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease --no-build-cache --max-workers=2 --stacktrace
```

No ambiente Windows/OneDrive ocorreu falha intermitente no empacotamento e um bloqueio de arquivos durante limpeza. Depois de encerrar o daemon e limitar os workers, a compilação completa limpa passou em 2 min 30 s. O workflow também limita os workers e preserva stacktraces para diagnóstico.

O lint foi executado depois de gerar as duas variantes. Quando lint e geração de Hilt de release foram sobrepostos, o analisador apresentou uma falha interna de resolução de arquivo gerado. A execução sequencial passou sem desabilitar verificações. Os avisos atuais são 25 avisos de SDK/ferramentas/dependências, um trust manager em código transitivo bcpkix não utilizado pela aplicação e um qualificador de recurso redundante. Veja [REVIEW.md](REVIEW.md) para a avaliação.

### Testes Android

Para limitar a execução local ao emulador escolhido e preservar a instalação existente, os APKs foram instalados com `-r` e os testes acionados diretamente pelo AndroidJUnitRunner:

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell pm grant com.rentalvalidator.app android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell am instrument -w -r -e reviewFolder portfolio-review-run com.rentalvalidator.app.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado final do runner: `OK (45 tests)`. O código de encerramento da instrumentação, isoladamente, não identifica aprovação; o resultado e eventuais falhas também foram conferidos. A primeira execução revelou um erro do novo leitor de schemas de teste, que exigia o campo opcional `indices`; ele foi corrigido e a suíte completa foi repetida.

Na primeira execução remota, 43 dos 45 testes Android passaram. Dois testes de interface assumiam um viewport maior que o Pixel 2 do CI: o rodapé do histórico estava fora da primeira tela e o switch de multa precisava de rolagem antes do toque. Os testes passaram a rolar até esses controles, mantendo as asserções de visibilidade e de mudança de estado. Os dois cenários foram reproduzidos no emulador local com resolução 1080×1920 e densidade 420 dpi. A interface de produção foi preservada.

## Cenários cobertos

- Regras de multa/juros, normalização de nomes, matching sem nomes vazios, parsing de moeda, CPF, telefone e extratos.
- DatePicker em UTC com fusos São Paulo, Honolulu e Tóquio; histórico na passagem de ano.
- Atualização do morador sem perda do histórico; ocupação observável; renomeação de unidades, bloqueio de exclusão ocupada e pagamento da mesma competência.
- Migração dos schemas 1, 2 e 3 até o schema 4 com preservação de moradores, unidade descoberta e pagamento.
- Round-trip do backup completo com documentos, configurações e lembretes; arquivos ausentes; corrupção, entradas inválidas e cancelamento após substituir o banco.
- JSON legado, descoberta de unidade, IDs duplicados e competência inválida sem substituir dados válidos.
- Cópia/remoção de documentos e proteção do diretório; PDF corrompido e PDF AES-256 com senha de abertura vazia.
- Cálculo, persistência, entrega e navegação de lembretes; registros de preferências inválidos e versões desconhecidas.
- Telas claras/escuras, texto ampliado, formulários, filtros, navegação, contato, documentos, conferência e mudanças rápidas de competência.

Os testes não usam extratos, documentos ou moradores reais. A cópia de segurança do emulador e os logs completos ficaram privados e não fazem parte do Git. O APK normal foi mantido instalado; nenhuma base de demonstração foi inserida no aplicativo.

## Limites da evidência

Não houve testes em todos os níveis de API, aparelhos físicos, PDF com OCR, todos os bancos, queda abrupta de energia/processo ou distribuição assinada. A ausência de alertas no OSV não é uma garantia de segurança absoluta. Os gráficos continuam sendo estimativas derivadas do aluguel atual e os backups não são criptografados.

O status do workflow depois da publicação é consultável em [GitHub Actions](https://github.com/GabrielAngelimm/alugueis-nani-android/actions/workflows/android.yml). Ele revalida a revisão a partir dos arquivos públicos e disponibiliza relatórios dos testes e do lint.
