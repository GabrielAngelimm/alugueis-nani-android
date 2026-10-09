# Validação da revisão

## Moradores no card da unidade (8 de outubro de 2026)

O cabeçalho do card passou a mostrar só o nome da unidade e o endereço; o tipo do imóvel e a marca de ocupação saíram. No rodapé, a fileira mostra até quatro moradores e conta os demais num "+N" discreto; as vagas livres tracejadas foram retiradas. Ao abrir o card, cada retrato da fileira se desloca até a linha do respectivo inquilino e os nomes entram em seguida; ao fechar, as linhas voltam a formar a fileira. A fileira também passou a ser lida pelo TalkBack ("Clara Nunes, Davi Rocha, ... e mais 2").

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest` | **44 testes**, sem falhas |
| Testes instrumentados | **50 testes**, todos passaram (1 novo: card com seis moradores mostra quatro retratos e "+2", sem tipo nem marca; abre com todos os nomes e fecha de volta à fileira) |
| `lintDebug` | Sem erros; os mesmos **27 avisos** anteriores |
| Inspeção visual | Quadros intermediários da abertura e do fechamento capturados com o relógio de teste pausado, nos dois temas |

Nos primeiros quadros, os retratos ficavam translúcidos no meio do trajeto, porque as duas cópias se misturavam; agora o retrato de destino fica sempre opaco. No fechamento, o card encolhia mais rápido que o último retrato subia e o cortava na borda; o recolhimento passou a usar a mesma mola dos retratos.

## Cards e detalhes das unidades (8 de outubro de 2026)

O card da unidade ganhou o ícone do tipo de imóvel no lugar das iniciais, a marca de ocupação ao lado do tipo e uma faixa com dois números alinhados: aluguéis por mês e ocupação. Os moradores ficam no rodapé, junto do botão que mostra a lista. Nos detalhes do inquilino e da unidade, os dois fatos principais ficam sempre na mesma linha, com um fio entre eles; quando um valor é largo, os dois diminuem juntos. O detalhe da unidade deixou de mostrar a marca de ocupação e o texto de vagas ou excesso, e o condomínio só aparece em "Sobre a unidade" quando foi informado.

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest` | **44 testes**, sem falhas |
| Testes instrumentados | **49 testes**, todos passaram (1 novo: valores largos numa coluna de 320dp continuam lado a lado, em uma linha, sem invadir o outro lado) |
| `lintDebug` | Sem erros; os mesmos **27 avisos** anteriores |
| Inspeção visual | Cards fechados e abertos nos dois temas, detalhes de unidade e inquilino, valores largos e escala de texto 1,3 revisados nas capturas dos testes |

Durante a revisão, um teste mostrou que texto ajustável não pode ficar dentro de uma linha com altura intrínseca: a tela de Locações fechava ao abrir. O divisor passou a ter altura fixa e o problema não se repetiu.

## Correção dos anéis (8 de outubro de 2026)

Os anéis do Início, de Receber (Mensal e Conferir extrato) mediam valores em reais e deixavam "a vencer" como trilho, enquanto os números ao lado contavam inquilinos; com aluguéis diferentes, as fatias não correspondiam às quantidades. Todos passaram a contar itens a partir de uma única lista que também gera contadores e legendas; o percentual central é a parcela de pagos. Docs já contava corretamente e passou a usar a mesma lista. Os anéis de ocupação foram retirados dos cards e dos detalhes das unidades.

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest` | **44 testes** (8 novos em `RingMathTest`: frações por contagem, fatias iguais desenhadas iguais, proporções, percentuais e contagem por estado) |
| Testes instrumentados | **48 testes**, todos passaram |
| Inspeção visual | Anéis, legendas e contadores conferidos entre si; fileira de moradores revisada ampliada nos dois temas |

## Navegação, anéis e tamanho de texto (8 de outubro de 2026)

A barra de navegação passou a ser uma cápsula flutuante; as réguas de progresso foram substituídas por anéis com contagens; a seção de documentos passou a se chamar Docs; e o texto do aplicativo deixou de acompanhar o tamanho de fonte do sistema. Também foi corrigido o texto preto no tema escuro do aplicativo real, causado pela troca do `Scaffold` por uma `Box` sem cor de conteúdo.

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest` | **36 testes**, sem falhas |
| Testes instrumentados (emulador API 37) | **48 testes**, todos passaram (2 novos: cor do texto do esqueleto real no tema escuro e trava do tamanho de fonte) |
| Trava de fonte | Com `font_scale` do sistema em 1,5, a Activity manteve escala 1,0 e idioma pt-BR; o emulador voltou a 1,0 depois |
| `lintDebug` | Sem erros; os mesmos **27 avisos** anteriores |
| Inspeção visual | Barra, anéis e telas em tema claro e escuro revisados nas capturas dos testes |

Os testes de interface montam as telas dentro de um `Scaffold` próprio, que define a cor do conteúdo; por isso o texto preto não aparecia neles. O novo teste usa o esqueleto real do aplicativo.

## Redesign da interface (7 de outubro de 2026)

A camada de apresentação foi reconstruída com o design system "Caderneta" ([DESIGN.md](../DESIGN.md)). Banco, repositórios, backups, regras de conferência, lembretes e integrações não foram alterados; o ViewModel mensal passou apenas a expor, para leitura, os estados do ano já carregados.

| Verificação | Resultado |
| --- | --- |
| `testDebugUnitTest --rerun-tasks` | **36 testes** (35 anteriores e 1 novo para o preenchimento de valores), sem falhas |
| `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest` | Compilados |
| Testes instrumentados (emulador API 37, 1280×2856) | **46 testes**, todos passaram (45 anteriores e 1 novo para a lista "Quem falta pagar") |
| `lintDebug` | Sem erros; os mesmos **27 avisos** de SDK, dependências e código transitivo da revisão anterior |
| Contraste | Pares de texto do tema claro e escuro acima de 4,5:1; linhas de campo acima de 3:1 |
| Inspeção visual | Capturas dos testes em tema claro, escuro e texto ampliado (1,3× e 1,6×) revisadas tela a tela |

Os testes de interface continuaram usando os mesmos rótulos e nomes acessíveis. Em uma pasta com caracteres não ASCII no caminho, o AGP no Windows compila com `android.overridePathCheck`, mas os testes JVM falham ao carregar as classes; a execução acima usou o projeto a partir de uma unidade mapeada com `subst` para um caminho ASCII.

## Publicação

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
