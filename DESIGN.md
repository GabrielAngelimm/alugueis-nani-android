# Identidade visual

Esta documentação descreve a interface existente. A revisão de publicação preservou a composição, os fluxos e a identidade do aplicativo.

## Base visual

| Elemento | Direção |
| --- | --- |
| Cor principal | `#525DD8`, usada para ações e seleção |
| Fundo claro | `#F7F8FA` |
| Texto principal | `#111317` |
| Superfície escura | `#202631` |
| Tipografia | Manrope local na interface; Fraunces disponível nos recursos |
| Marca | Ícone que relaciona casa e a letra N, disponível em `brand/` |

O tema oferece esquemas claro e escuro e papéis semânticos para pagamentos, avisos e ações. Componentes compartilhados concentram superfícies, botões, formulários, seletores de mês e estados vazios. A navegação usa quatro áreas principais: Início, Locações, Receber e Documentos.

## Manutenção

Ao alterar uma tela, preserve os comportamentos de edição, exclusão, anexos, importação e navegação. Teste também texto ampliado, teclado, contraste, semântica de acessibilidade e estados de carregamento/erro. Parte dos textos ainda está diretamente no Kotlin; sua extração gradual é uma dívida documentada, sem uma alteração cosmética ampla nesta revisão.

As imagens do README são capturas reais da interface executada por testes com banco isolado e moradores fictícios. Não use capturas de operação, documentos ou extratos reais no material público.

## Fontes e ativos

As fontes são distribuídas localmente e acompanhadas de suas licenças OFL nos assets. Consulte [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Os arquivos da marca e scripts de geração em `brand/` fazem parte da apresentação do projeto; não são necessários para iniciar o aplicativo.
