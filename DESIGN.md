# Identidade visual: Caderneta

A interface parte do objeto que o aplicativo substitui: a caderneta de aluguéis de quem administra uma república. Papel pautado, tinta de caneta, o verde dos livros-caixa, o carimbo de "pago" e o marca-texto do que ainda está em aberto. A referência é material, não decorativa: cada elemento visual codifica uma informação.

## Princípios

1. **Livro-caixa, não painel.** Listas são folhas pautadas: uma superfície por grupo, entradas separadas por linhas finas. Cartões individuais ficam para objetos que têm ações próprias (uma unidade, um resultado de conferência, um dossiê de documentos).
2. **Estado é forma, não só cor.** Pago é um carimbo de contorno duplo, levemente inclinado; pendente é uma faixa de marca-texto; em análise é um contorno tracejado; problema é um contorno sólido com fundo. A leitura sobrevive a daltonismo e a telas em escala de cinza.
3. **Tinta é ação.** O azul de caneta aparece em botões, seleção e links. Valores, nomes e estados usam tinta neutra ou o papel semântico correspondente.
4. **Um único destaque.** O anel do mês é o elemento memorável: uma fatia por estado (pago, a vencer, em atraso e, quando houver, em análise) a partir do topo, cada uma proporcional à quantidade de aluguéis naquele estado, com o percentual de pagos no centro e, ao lado, a mesma contagem em números. Anel, contadores e legendas são montados a partir de uma única lista, então cor, rótulo, quantidade e percentual não podem divergir. Os anéis aparecem só no Início, em Receber (Mensal e Conferir extrato) e em Docs. É o único movimento espontâneo da interface, uma varredura única no sentido horário. Os demais movimentos respondem a uma ação: folhas que abrem, linhas que expandem, o carimbo que "cai" quando um aluguel é marcado como pago, a lente da navegação que desliza até a seção escolhida.

## Cores

| Papel | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| Papel | `#EDF1E9` | `#10151D` | Fundo das telas |
| Folha | `#FBFCF9` | `#171E28` | Superfícies, folhas pautadas |
| Pauta | `#D3DBCF` | `#2B3442` | Linhas divisórias |
| Tinta | `#1B2330` | `#E8ECF1` | Texto principal |
| Caneta | `#2A43A6` | `#B0C0FF` (texto) / `#4460D6` (preenchimento) | Ações e seleção |
| Livro (pago) | `#2B6A4B` sobre `#DBEEE1` | `#8AD4AA` sobre `#173728` | Recebido, vigente |
| Marca-texto (pendente) | `#573700` sobre `#FBE8A8` | `#FBE3A5` sobre `#3A3013` | Em aberto, a vencer |
| Carimbo (atraso) | `#B0372D` sobre `#FAE2DD` | `#FF9E91` sobre `#4C1E19` | Vencido, erro |
| Placa | `#233886` | `#3A50A8` | Selo do aplicativo |
| Capa do inquilino | `#4864C2` a `#2B4290` | `#3A50AE` a `#1C2A66` | Topo do detalhe do inquilino, com texto `#F4F6FF` |
| Capa da unidade | `#4468A0` a `#26446F` | `#34568A` a `#172C4E` | Topo do detalhe da unidade, com texto `#F4F6FF`; a marca usa `#26446F` sobre `#DCE6F3` (escuro: `#C4D5EE` sobre `#1D2E4A`) |

As cores ficam em `presentation/theme/Color.kt`. Papéis que o Material 3 não define (estados de pagamento, placa, capas, marca da unidade, preenchimento de ação, trilho dos anéis e monogramas) são expostos por `NaniTheme.colors`. As telas escolhem um papel, nunca um valor hexadecimal.

## Tipografia

| Voz | Fonte | Uso |
| --- | --- | --- |
| Caderneta | Fraunces variável, eixo `SOFT` 70, `WONK` 0, tamanho óptico 72 (títulos) ou 20 (nomes e valores) | Títulos de tela e seção, nomes em destaque, valores monetários |
| Interface | Manrope variável com algarismos tabulares | Controles, rótulos, textos e listas |

A escala (`Type.kt`) usa corpo 15sp, legendas de 13sp e rótulos de 12–15sp, pensada para leitura confortável por pessoas mais velhas. Valores grandes usam `FittingText`, que reduz o corpo apenas o necessário para nunca cortar um número. Rótulos curtos (abas e navegação) são medidos para que nenhuma palavra seja quebrada em telas estreitas.

O tamanho do texto é fixo: a configuração de tamanho de fonte do sistema não altera o aplicativo, que sempre aparece com as proporções desenhadas. A trava fica na configuração da Activity (`MainActivity.attachBaseContext`), por isso vale também para painéis, diálogos e seletores. A configuração de tamanho de exibição continua ampliando a interface inteira por igual, o que preserva as proporções.

As fontes são locais e acompanhadas das licenças OFL em `assets/licenses`.

## Navegação

A barra é uma cápsula flutuante sobre o conteúdo, que corre por trás dela e se dissolve num degradê do papel. A sombra tem duas camadas tingidas com a tinta do tema, uma difusa e ampla e outra curta de contato; no tema escuro, uma borda de luz fina substitui a sombra que não apareceria. As listas reservam ao final o espaço da barra (`LocalNavigationClearance`), para que o último item nunca fique escondido. As seções são Início, Locações, Receber e Docs.

## Capa das páginas de detalhe

Inquilino e unidade abrem com um único cartão. O topo é uma capa azul, como a de uma caderneta de poupança, iluminada no canto superior direito e gravada com um guilloché, como as linhas de segurança de um documento. As duas capas são da mesma família, mas nunca se confundem. A do inquilino é azul-caderneta e leva anéis finos e concêntricos que irradiam da luz, como uma impressão digital, cada um com ondulações suaves que giram de leve de um anel para o outro. A da unidade puxa para o azul-aço e leva linhas paralelas, quase retas, que atravessam a luz, como o fundo pautado de uma escritura, cada uma com uma ondulação suave que corre um pouco adiante da linha de cima. Os traços nunca se cruzam e se dissolvem antes de chegar à marca, deixando limpa a área do texto. A gravura é derivada do nome, então cada inquilino e cada unidade tem a sua e a reencontra a cada visita. Sobre a capa ficam, numa só linha de leitura, a marca, o nome em Fraunces e o subtítulo (a unidade do inquilino ou o endereço da unidade). A marca é sempre um disco de 58dp num halo de luz de 3dp: o monograma do inquilino, ou o ícone do tipo de imóvel desenhado como um monograma, em tinta de aço sobre um disco claro. O nome usa o maior corpo que o mantém em até duas linhas (26, 23 ou 20sp) e só nomes muito longos chegam a três. Abaixo da capa, no mesmo cartão, ficam os dois fatos principais. De dia, o cartão tem uma sombra tingida com o azul da sua capa; à noite, a própria capa faz o destaque.

## Componentes

| Componente | Arquivo | Papel |
| --- | --- | --- |
| `NaniHeader`, `NaniTabs`, `NaniSection`, `NaniEditor` | `design/Layout.kt` | Cabeçalhos, controle segmentado, seções e editor em tela cheia |
| `LedgerSheet`, `LedgerSlice`, `LedgerRule`, `NaniActionRow`, `NaniFact` | `design/Ledger.kt` | Folhas pautadas e linhas de lista |
| `StatusMark` | `design/Status.kt` | Carimbo, marca-texto, tracejado e contorno |
| `StatusRing`, `RingLabel`, `StatTiles`, `RingLegend`, `dueStats`, `ringOf` | `design/Progress.kt` | Anéis por contagem do Início, de Receber e de Docs, com contadores e legendas da mesma lista; nos contadores do Início, cada estado ocupa uma coluna igual, com o número centralizado e sublinhado por um traço curto na cor do seu arco no anel (o traço fica no tom do trilho quando o estado está vazio), separadas por fios de mesma altura; a geometria (`ringMarks`) é testada em `RingMathTest` |
| `Monogram`, `UnitTile`, `UnitPlaque`, `ResidentsPile`, `NaniSheetTenantCard` | `design/Identity.kt` | Identidade de pessoas e unidades: o ícone do tipo de imóvel (`UnitTile`), num círculo de vidro redondo como os monogramas, marca cada unidade no card; no rodapé do card, até quatro retratos sobrepostos dos moradores e um "+N" discreto para os demais |
| `NaniDetailHero`, `NaniDetailCard`, `NaniDetailAction` | `design/Detail.kt` | Páginas de detalhe: capa com marca, nome e subtítulo; abaixo, os dois fatos principais sempre na mesma linha, separados por um fio |
| `engravingOf`, `ringRadius`, `courseY` | `design/Engraving.kt` | Gravura da capa derivada do nome: anéis para inquilinos, linhas paralelas para unidades; a geometria é testada em `EngravingTest` |
| `NaniSheet`, `NaniChoice` | `design/NaniSheet.kt` | Painéis inferiores e escolhas exclusivas |
| `NaniConfirmDialog` | `design/Dialogs.kt` | Confirmações com verbo de ação |
| `AppEmptyState`, `AppLoadingState`, `AppErrorState` | `design/States.kt` | Estados vazios, carregamento e erro |
| `PrimaryButton`, `SecondaryButton`, `TonalButton`, `DangerButton` | `components/Buttons.kt` | Hierarquia de ações |
| `NaniTextField`, `NaniDropdownField` | `components/Fields.kt` | Campos: caixa preenchida apoiada na pauta |
| `AppNavigation`, `AppNavigationRail` | `components/AppNavigation.kt` | Cápsula flutuante com lente deslizante; trilho lateral em telas largas |
| `GuidedFlow`, `StepPage`, `ReviewPage`, `FlowFooter`, `DueDayPicker`, `GlyphChoices`, `CountStepper`, `ReviewLine` | `ui/registration/` | Cadastro guiado: trilha de etapas, perguntas, escolhas visuais e revisão; as regras de cada etapa ficam em `RegistrationDrafts.kt` e são testadas em `RegistrationDraftsTest` |

Raios seguem a hierarquia: marcas 6dp, controles 14dp, folhas 20dp, painéis e diálogos 28dp. O espaçamento usa múltiplos de 4dp, margem lateral de 20dp e 32dp entre seções.

## Acessibilidade e adaptação

- Rótulos visíveis dos campos são também o nome acessível; ícones de ação têm descrição própria.
- Alvos de toque mínimos de 48dp; linhas de lista inteiras são clicáveis.
- Estados não dependem só de cor; contraste de texto atende WCAG AA nos dois temas.
- Os dois fatos do topo dos detalhes ficam sempre na mesma linha. Se um valor não cabe na sua metade, os dois diminuem na mesma proporção, para continuarem nivelados e inteiros. Valores nunca são cortados.
- O texto não acompanha o tamanho de fonte do sistema. A escala base já foi definida com corpo de 15sp para leitura confortável; quem precisar de tudo maior pode usar o tamanho de exibição do Android, que amplia a interface inteira.
- Janelas a partir de 600dp usam trilho lateral; o conteúdo fica limitado a 960dp.
- Componentes do sistema (como o seletor de datas) seguem pt-BR, como o restante do aplicativo.

## Movimento

`AppMotion` concentra durações e curvas. As seções compartilham um eixo horizontal: ir para uma seção à direita na barra desliza a página da direita, e voltar desliza da esquerda. Na barra, a lente da seção ativa estica a caminho do destino (a borda da frente com mola mais rígida que a de trás) e o ícone ganha preenchimento com um pequeno salto. Páginas abertas a partir de outra deslizam no sentido de leitura. Expansões usam 240ms com desaceleração firme. No card da unidade, abrir a lista leva cada retrato da fileira até a linha do respectivo inquilino (`SharedTransitionLayout`, mola `AppMotion.Travel`), e os nomes entram em seguida, uma linha após a outra; fechar faz o caminho inverso, e o card recolhe na mesma mola para nunca cortar um retrato no trajeto. Animações respeitam a escala de animação do sistema.

## Cadastro guiado

O "+" de Locações abre uma escolha entre inquilino e unidade, cada um num cartão com uma faixa da capa azul. O cadastro segue uma pergunta por página, escrita como conversa ("Quanto e quando Marina paga?"), com perguntas próximas agrupadas: valor e vencimento, telefone e CPF, nome e endereço. Inquilino tem seis etapas (nome, unidade, aluguel, contato, extrato e revisão); unidade tem cinco (tipo, nome e endereço, capacidade, detalhes e revisão). Contato, extrato e detalhes são opcionais e marcados assim.

No topo, uma barra fina mostra o avanço e uma trilha de abas lista as etapas: as já respondidas trazem a resposta e podem ser reabertas, a atual fica em tinta e as próximas esperam em lápis. Avançar só valida a etapa da tela, com o erro junto ao campo e o foco nele; Voltar, inclusive o do sistema, recua uma pergunta sem perder nada. A revisão repete a capa da página de detalhe com o que será salvo e lista cada resposta, que abre a etapa para correção e volta direto à revisão. Fechar com respostas pede confirmação. Erros ao salvar aparecem na própria revisão, e a confirmação de uma unidade nova oferece cadastrar o primeiro inquilino nela.

Campos, validações e o registro gravado são os mesmos dos formulários completos, que continuam sendo usados para editar.

## Manutenção

Ao alterar uma tela, preserve os comportamentos de edição, exclusão, anexos, importação e navegação, e os rótulos usados como nomes acessíveis (os testes de interface os utilizam). Teste telas estreitas, tema escuro (no app real, não só no ambiente de teste), teclado, semântica de acessibilidade e estados de carregamento e erro.

As imagens do README são capturas reais da interface executada pelos testes instrumentados com banco isolado e moradores fictícios. Não use capturas de operação, documentos ou extratos reais no material público.
