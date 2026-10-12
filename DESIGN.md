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
| Capa do inquilino (safira) | `#5B74DA` a `#243A8C` | `#4660C8` a `#172462` | Topo do detalhe do inquilino, com texto `#F4F6FF`; a luz puxa para o azul-claro e o reflexo de baixo, para o violeta |
| Capa da unidade (petróleo) | `#3F8597` a `#173F4F` | `#326E80` a `#0F2E3B` | Topo do detalhe da unidade, com texto `#F4F6FF`; a luz puxa para o água-marinha e o tom de baixo, para o verde-mar; a marca usa `#173F4F` sobre `#DDEEF1` (escuro: `#A9CFD7` sobre `#0C2530`) |

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

Inquilino e unidade abrem com um único cartão. O topo é uma capa que parece luz sobre um material, não impressão, como um cartão de metal na mão: o fundo escurece na diagonal, um tom vizinho mais claro se abre do canto superior direito como a fonte de luz, um segundo tom sobe suave por baixo, o lado esquerdo assenta no tom mais fundo para o nome sempre ler bem, e um grão quase invisível dá textura e evita faixas no degradê. A borda de cima pega a luz num fio claro que se apaga pelas laterais. As duas capas são da mesma família e nunca se confundem: a do inquilino é safira, com a luz em azul-pervinca e o tom de baixo puxando para o violeta; a da unidade é azul-petróleo, com a luz em água-marinha e o tom de baixo em verde-mar. Nada de linhas, formas ou marcas no fundo: a capa se distingue só pela luz e pela cor, e a marca do registro aparece uma vez, no disco ao lado do nome. Sobre a capa ficam, numa só linha de leitura, a marca, o nome em Fraunces e o subtítulo (a unidade do inquilino ou o endereço da unidade). A marca é sempre um disco de 58dp num anel de vidro de 3dp com um fio claro na borda: o monograma do inquilino, ou o ícone do tipo de imóvel desenhado como um monograma, em tinta petróleo sobre um disco claro. O nome usa o maior corpo que o mantém em até duas linhas (26, 23 ou 20sp) e só nomes muito longos chegam a três. Abaixo da capa, no mesmo cartão, ficam os dois fatos principais. De dia, o cartão tem uma sombra tingida com o fundo da sua capa; à noite, a própria capa faz o destaque.

## Componentes

| Componente | Arquivo | Papel |
| --- | --- | --- |
| `NaniHeader`, `NaniTabs`, `NaniSection`, `NaniEditor` | `design/Layout.kt` | Cabeçalhos, controle segmentado, seções e editor em tela cheia, desenhado atrás das barras e do teclado: o conteúdo sobe os próprios botões (`imePadding`), sem o sistema empurrar a tela. A partir do Android 15, a janela cobre também as barras do sistema, para os botões do rodapé nunca ficarem cortados |
| `LedgerSheet`, `LedgerSlice`, `LedgerRule`, `NaniActionRow`, `NaniFact` | `design/Ledger.kt` | Folhas pautadas e linhas de lista |
| `StatusMark` | `design/Status.kt` | Carimbo, marca-texto, tracejado e contorno |
| `StatusRing`, `RingLabel`, `StatTiles`, `RingLegend`, `dueStats`, `ringOf` | `design/Progress.kt` | Anéis por contagem do Início, de Receber e de Docs, com contadores e legendas da mesma lista; nos contadores do Início, cada estado ocupa uma coluna igual, com o número centralizado e sublinhado por um traço curto na cor do seu arco no anel (o traço fica no tom do trilho quando o estado está vazio), separadas por fios de mesma altura; a geometria (`ringMarks`) é testada em `RingMathTest` |
| `Monogram`, `UnitTile`, `UnitPlaque`, `BankCoin`, `ResidentsPile`, `NaniSheetTenantCard` | `design/Identity.kt` | Identidade de pessoas, unidades e bancos (`BankCoin`: moeda redonda com o logotipo oficial do banco, como o ícone do app dele, que pode virar para o selo branco; os logos são marcas dos bancos, usados só para identificá-los, e ficam em `res/drawable-nodpi/bank_*.webp`, moedas de 160 px montadas a partir dos SVGs da coleção pública [Bancos-em-SVG](https://github.com/Tgentil/Bancos-em-SVG)): o ícone do tipo de imóvel (`UnitTile`), num círculo de vidro redondo como os monogramas, no petróleo da capa da unidade, marca cada unidade no card; no rodapé do card, até quatro retratos sobrepostos dos moradores e um "+N" discreto para os demais |
| `NaniDetailHero`, `NaniDetailCard`, `NaniDetailAction` | `design/Detail.kt` | Páginas de detalhe: capa com marca, nome e subtítulo; abaixo, os dois fatos principais sempre na mesma linha, separados por um fio |
| `engravedCover`, `coverRim` | `design/Cover.kt` | A mesma capa em qualquer tamanho (detalhe, revisão, faixas da escolha, cartões escolhidos, cartão do ano dos Pagamentos e, só com a luz, a prévia do cadastro): fundo, as duas luzes de cor, grão e o fio de luz da borda |
| `NaniSheet`, `NaniChoice` | `design/NaniSheet.kt` | Painéis inferiores e escolhas exclusivas |
| `NaniConfirmDialog` | `design/Dialogs.kt` | Confirmações com verbo de ação |
| `AppEmptyState`, `AppLoadingState`, `AppErrorState` | `design/States.kt` | Estados vazios, carregamento e erro |
| `PrimaryButton`, `SecondaryButton`, `TonalButton`, `DangerButton` | `components/Buttons.kt` | Hierarquia de ações |
| `NaniTextField`, `NaniDropdownField` | `components/Fields.kt` | Campos: caixa preenchida apoiada na pauta |
| `AppNavigation`, `AppNavigationRail` | `components/AppNavigation.kt` | Cápsula flutuante com lente deslizante; trilho lateral em telas largas |
| `GuidedFlow`, `FlowPreview`, `StepPage`, `ReviewPage`, `FlowFooter`, `DueDayPicker`, `CoverChoices`, `BankChoices`, `CountStepper`, `ReviewLine` | `ui/registration/` | Cadastro guiado: régua de etapas, prévia viva do registro, perguntas, escolhas visuais e revisão; as regras de cada etapa ficam em `RegistrationDrafts.kt` e são testadas em `RegistrationDraftsTest` |
| `TenantPaymentsContent`, `YearCard`, `MonthGlyph`, `MonthSlip`, `SlipShape` | `ui/tenant_payments/` | Pagamentos de um inquilino: cartão do ano com a capa gravada e a fileira de meses, e o cupom do mês, que desliza entre meses e anos; as regras (`monthState`, `monthRange`, `yearSummary`) ficam em `PaymentYear.kt` e são testadas em `PaymentYearTest` |

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

O "+" de Locações abre uma escolha entre inquilino e unidade, cada um num cartão com uma faixa da sua capa: safira para a pessoa, petróleo para o imóvel. O cadastro segue uma pergunta por página, escrita como conversa ("Quanto e quando Marina paga?"), com perguntas próximas agrupadas: valor e vencimento, telefone e CPF, nome e endereço. Cada página tem só a pergunta e o campo, sem subtítulo nem texto explicativo; o formato esperado aparece em cinza dentro do próprio campo ("0,00", "(00) 00000-0000"). Os campos do cadastro são caixas claras de cantos arredondados, com um fio que ganha a cor de ação no foco. Inquilino tem seis etapas (nome, unidade, aluguel, contato, extrato e revisão); unidade tem cinco (tipo, nome e endereço, capacidade, detalhes e revisão). Extrato e detalhes são opcionais e marcados assim. As escolhas seguem a linguagem das capas: tipo de imóvel e unidade são cartões compactos, dois por linha, com o ícone num disco ao lado do nome (o da unidade na letra da caderneta). Em repouso o cartão tem um degradê suave de petróleo e um fio do mesmo tom; o escolhido acende como uma pequena capa de unidade, com a luz, o grão e o fio de luz da página dela, e um visto no disco. Os nomes encolhem só o necessário para nenhuma palavra quebrar ao meio num celular estreito. Bancos são cartões três por linha, em papel um tom abaixo do branco, com a moeda do logotipo oficial sobre o nome; o escolhido se pinta na cor do próprio banco, como um cartão tirado da carteira, e a moeda vira o selo branco com o logo; "Nenhum" escurece. Todos os bancos têm o mesmo tamanho: o nome sempre reserva duas linhas, mesmo quando cabe em uma. Nenhum cartão muda de tamanho ao ser escolhido, então nada se mexe ao tocar.

No topo, uma régua única de etapas: paradas numeradas ligadas por um traço que se pinta até a etapa da tela, e embaixo o nome da etapa atual e "Etapa 3 de 6". As já respondidas levam um visto e podem ser reabertas; as próximas esperam em lápis. As respostas não aparecem na régua (ficam na revisão). Com o teclado aberto, a régua fica só com as paradas, para a pergunta e o campo caberem juntos; o editor desenha atrás do teclado e sobe só os botões, em vez de deixar o sistema empurrar a tela para cima (o mesmo vale para os formulários de edição). O que rola por baixo do topo some em degradê.

Abaixo da régua fica a prévia do registro, como o cartão que se desenha ao adicionar um cartão num app de pagamentos: começa como um cartão em branco tracejado a lápis, com os campos em cinza, e assim que o registro tem nome vira um cartão claro e discreto, no branco das folhas com um fio fino, onde só uma luz leve no tom da capa (safira para a pessoa, petróleo para o imóvel) acende o canto superior direito. Depois se preenche resposta a resposta: unidade, valor e vencimento do inquilino, com a moeda do banco presa ao monograma; endereço e capacidade da unidade. A cor do monograma segue o nome de quando a página virou, para não mudar a cada letra. A prévia fica neutra para não disputar atenção com a pergunta. Ela sai na revisão, onde a capa colorida completa assume, e em janelas baixas (celular pequeno com teclado aberto), para a pergunta manter o espaço.

Avançar só valida a etapa da tela, com o erro junto ao campo e o foco nele; Voltar, inclusive o do sistema, recua uma pergunta sem perder nada. A revisão repete a capa da página de detalhe com o que será salvo e lista cada resposta, que abre a etapa para correção e volta direto à revisão. Fechar com respostas pede confirmação. Erros ao salvar aparecem na própria revisão, e a confirmação de uma unidade nova oferece cadastrar o primeiro inquilino nela.

Campos, validações e o registro gravado são os mesmos dos formulários completos, que continuam sendo usados para editar, com uma diferença: no cadastro guiado o telefone é obrigatório (nome no WhatsApp e CPF continuam opcionais). A edição ainda aceita telefone vazio, para não travar registros antigos.

## Pagamentos do inquilino

A página abre com um único cartão do ano. O topo é a mesma capa gravada do inquilino, com "Recebido no ano" e o valor em destaque, quantos meses já vencidos foram pagos ("7 de 10 meses pagos até agora") e uma pílula com o que mais importa no ano: meses em atraso com o valor em aberto, pagamento em análise, meses sem registro ou "Tudo em dia". No canto, uma cápsula troca o ano. Abaixo da capa, no mesmo cartão, os doze meses formam uma fileira que desliza para os lados, como as abas de mês de uma fatura de cartão: cada mês leva uma marca pequena na forma do seu estado (selo cheio para pago, disco marca-texto para o que está aberto, anel tracejado para análise, anel sólido para atraso, anel vazio para o que ainda vai chegar e um traço onde não há registro). O mês escolhido fica em caneta e o atual leva um ponto. As pontas da fileira esmaecem enquanto houver meses além delas.

Sob o cartão, o mês escolhido aparece como o cupom de um carnê: mês, vencimento, a marca do estado e o aluguel acima de um picote; abaixo dele, uma frase curta com o que o mês pede ("Venceu há 2 dias.") e as ações. Os cupons deslizam de mês em mês e atravessam os anos; os vizinhos aparecem nas bordas para mostrar que dá para deslizar, e a fileira e o ano acompanham. Cada mês tem uma só ação forte, a que ele pede: "Marcar como pago" (ou "Confirmar pagamento" quando está em análise), que vibra, carimba o cupom e oferece "Desfazer". Meses futuros ou anteriores ao cadastro ainda podem ser pagos, com o botão discreto. "Mudar situação" abre o mesmo painel de antes, para qualquer mês ir para qualquer estado. Com telefone cadastrado, um mês aberto oferece "Cobrar" e um mês pago oferece "Confirmar no WhatsApp", com uma mensagem de recebimento. Os cupons lado a lado têm sempre a mesma altura.

Um mês passado sem registro aparece como "Sem registro", nunca como atraso, porque o app pode não ter sido usado naquele mês; só o mês atual, ou um mês gravado como pendente, entra em atraso. Os valores seguem o aluguel atual, já que um pagamento não guarda valor próprio, e uma nota no fim da página diz isso.

## Manutenção

Ao alterar uma tela, preserve os comportamentos de edição, exclusão, anexos, importação e navegação, e os rótulos usados como nomes acessíveis (os testes de interface os utilizam). Teste telas estreitas, tema escuro (no app real, não só no ambiente de teste), teclado, semântica de acessibilidade e estados de carregamento e erro.

As imagens do README são capturas reais da interface executada pelos testes instrumentados com banco isolado e moradores fictícios. Não use capturas de operação, documentos ou extratos reais no material público.
