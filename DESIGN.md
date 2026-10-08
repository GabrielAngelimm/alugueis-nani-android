# Identidade visual: Caderneta

A interface parte do objeto que o aplicativo substitui: a caderneta de aluguéis de quem administra uma república. Papel pautado, tinta de caneta, o verde dos livros-caixa, o carimbo de "pago" e o marca-texto do que ainda está em aberto. A referência é material, não decorativa: cada elemento visual codifica uma informação.

## Princípios

1. **Livro-caixa, não painel.** Listas são folhas pautadas: uma superfície por grupo, entradas separadas por linhas finas. Cartões individuais ficam para objetos que têm ações próprias (uma unidade, um resultado de conferência, um dossiê de documentos).
2. **Estado é forma, não só cor.** Pago é um carimbo de contorno duplo, levemente inclinado; pendente é uma faixa de marca-texto; em análise é um contorno tracejado; problema é um contorno sólido com fundo. A leitura sobrevive a daltonismo e a telas em escala de cinza.
3. **Tinta é ação.** O azul de caneta aparece em botões, seleção e links. Valores, nomes e estados usam tinta neutra ou o papel semântico correspondente.
4. **Um único destaque.** A régua do mês (cada inquilino como um segmento proporcional ao aluguel, preenchido quando recebido) é o elemento memorável e o único movimento espontâneo da interface, revelado uma vez da esquerda para a direita. Os demais movimentos respondem a uma ação: folhas que abrem, linhas que expandem, o carimbo que "cai" quando um aluguel é marcado como pago.

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
| Placa | `#233886` | `#3A50A8` | Identidade das unidades |

As cores ficam em `presentation/theme/Color.kt`. Papéis que o Material 3 não define (estados de pagamento, placa, preenchimento de ação, régua e monogramas) são expostos por `NaniTheme.colors`. As telas escolhem um papel, nunca um valor hexadecimal.

## Tipografia

| Voz | Fonte | Uso |
| --- | --- | --- |
| Caderneta | Fraunces variável, eixo `SOFT` 70, `WONK` 0, tamanho óptico 72 (títulos) ou 20 (nomes e valores) | Títulos de tela e seção, nomes em destaque, valores monetários |
| Interface | Manrope variável com algarismos tabulares | Controles, rótulos, textos e listas |

A escala (`Type.kt`) usa corpo 15sp, legendas de 13sp e rótulos de 12–15sp, pensada para leitura confortável por pessoas mais velhas. Valores grandes usam `FittingText`, que reduz o corpo apenas o necessário para nunca cortar um número. Rótulos curtos (abas e navegação) são medidos para que nenhuma palavra seja quebrada; com texto ampliado, "Documentos" passa a "Docs" antes de qualquer redução.

As fontes são locais e acompanhadas das licenças OFL em `assets/licenses`.

## Componentes

| Componente | Arquivo | Papel |
| --- | --- | --- |
| `NaniHeader`, `NaniTabs`, `NaniSection`, `NaniEditor` | `design/Layout.kt` | Cabeçalhos, controle segmentado, seções e editor em tela cheia |
| `LedgerSheet`, `LedgerSlice`, `LedgerRule`, `NaniActionRow`, `NaniFact` | `design/Ledger.kt` | Folhas pautadas e linhas de lista |
| `StatusMark` | `design/Status.kt` | Carimbo, marca-texto, tracejado e contorno |
| `Regua`, `ReguaLegend` | `design/Regua.kt` | Régua do mês, de contratos e de capacidade |
| `Monogram`, `UnitPlaque`, `NaniSheetTenantCard` | `design/Identity.kt` | Identidade de pessoas e unidades |
| `NaniDetailHero`, `NaniDetailCard`, `NaniDetailAction` | `design/Detail.kt` | Páginas de detalhe |
| `NaniSheet`, `NaniChoice` | `design/NaniSheet.kt` | Painéis inferiores e escolhas exclusivas |
| `NaniConfirmDialog` | `design/Dialogs.kt` | Confirmações com verbo de ação |
| `AppEmptyState`, `AppLoadingState`, `AppErrorState` | `design/States.kt` | Estados vazios, carregamento e erro |
| `PrimaryButton`, `SecondaryButton`, `TonalButton`, `DangerButton` | `components/Buttons.kt` | Hierarquia de ações |
| `NaniTextField`, `NaniDropdownField` | `components/Fields.kt` | Campos: caixa preenchida apoiada na pauta |
| `AppNavigation`, `AppNavigationRail` | `components/AppNavigation.kt` | Navegação com marcador de aba de caderno |

Raios seguem a hierarquia: marcas 6dp, controles 14dp, folhas 20dp, painéis e diálogos 28dp. O espaçamento usa múltiplos de 4dp, margem lateral de 20dp e 32dp entre seções.

## Acessibilidade e adaptação

- Rótulos visíveis dos campos são também o nome acessível; ícones de ação têm descrição própria.
- Alvos de toque mínimos de 48dp; linhas de lista inteiras são clicáveis.
- Estados não dependem só de cor; contraste de texto atende WCAG AA nos dois temas.
- Fatos lado a lado empilham quando o texto ampliado não cabe; valores nunca são cortados.
- Janelas a partir de 600dp usam trilho lateral; o conteúdo fica limitado a 960dp.
- Componentes do sistema (como o seletor de datas) seguem pt-BR, como o restante do aplicativo.

## Movimento

`AppMotion` concentra durações e curvas. Seções trocam por esmaecimento; páginas abertas a partir de outra deslizam no sentido de leitura. Expansões usam 240ms com desaceleração firme. Animações respeitam a escala de animação do sistema.

## Manutenção

Ao alterar uma tela, preserve os comportamentos de edição, exclusão, anexos, importação e navegação, e os rótulos usados como nomes acessíveis (os testes de interface os utilizam). Teste texto ampliado, tema escuro, teclado, semântica de acessibilidade e estados de carregamento e erro.

As imagens do README são capturas reais da interface executada pelos testes instrumentados com banco isolado e moradores fictícios. Não use capturas de operação, documentos ou extratos reais no material público.
