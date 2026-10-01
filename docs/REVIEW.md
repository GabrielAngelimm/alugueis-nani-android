# Revisão técnica para publicação

A revisão examinou código de produção e testes, modelos, DAOs, migrações, repositórios, ViewModels, navegação, telas e componentes Compose, processamento de extratos, backups, lembretes, manifesto, recursos, dependências, Gradle e material de apresentação. As mudanças foram limitadas a problemas reproduzíveis, proteção dos dados e preparação do repositório. Os resultados dos comandos estão em [VALIDATION.md](VALIDATION.md).

## Correções implementadas

| Área | Problema identificado | Resultado |
| --- | --- | --- |
| Moradores | `REPLACE` podia apagar um registro e, por cascata, seu histórico de pagamentos | `Upsert` mantém o vínculo; teste de preservação do histórico |
| Unidades | Contagem de ocupação dependia de consultas separadas e não acompanhava toda mudança de moradores | Consulta observável única para unidades e contagens |
| Unidades | Renomear unidade e atualizar texto legado eram operações separadas; exclusão verificava ocupação antes da transação | Renomeação e exclusão protegidas por transações no DAO |
| Pagamentos | Consulta seguida de inserção/atualização podia concorrer e perder a identidade do registro | Gravação transacional preservando o ID da competência |
| Backup antigo | Parsing e IO estavam no ViewModel; registros duplicados podiam sobrescrever dados silenciosamente | Serviço de dados com limites e validação; duplicidades abortam a transação |
| Backup completo | Cancelamento podia interromper o rollback depois de substituir o banco | Substituição e recuperação protegidas com `NonCancellable`; cenário testado |
| Documentos | Cópia na interface, nomes derivados de IDs importados e exclusão sem restringir diretório | Serviço em IO com nomes UUID, cópia temporária, limites e remoção restrita |
| Datas | O DatePicker fornece meia-noite UTC; converter para o fuso local podia voltar um dia | Conversão específica em UTC, testada em três fusos |
| Histórico | Recorte somente por número do mês misturava anos na passagem de dezembro para janeiro | Recorte por ano/mês com dados do ano anterior |
| Conferência | Nome normalizado vazio correspondia a qualquer descrição | Nomes/aliases vazios são descartados antes do matching |
| Formulários | Algumas telas confirmavam gravação antes do resultado e perdiam erros de persistência | Sucesso após gravação; canais de erro apresentados pela interface |
| Unidades | Editar os dados podia descartar a referência de fotografia existente | Referência preservada ao editar |
| Lembretes e preferências | Tipo incorreto ou campos inválidos de configurações podiam falhar; cancelamento era tratado como erro comum | Validação dos registros e propagação correta de cancelamento |
| PDF | Falha de leitura podia retornar lista vazia e o documento não tinha fechamento garantido | `use` fecha recursos; erro de leitura explícito |
| Moeda | `NumberFormat` compartilhado não é seguro entre threads | Formatação sincronizada |
| Dependências | PDFBox trazia Bouncy Castle 1.72 com alertas no OSV | Constraints mantêm os três módulos em 1.86; leitura de PDF AES testada |
| Build | Runner instrumentado não estava declarado e faltava o arquivo referenciado de regras ProGuard | Runner configurado, schemas disponíveis nos testes e arquivo de regras presente |
| Recursos | XMLs padrão de backup não eram referenciados pelo manifesto | Recursos sem efeito removidos; comportamento de backup do Android preservado |
| Privacidade | Código de migração e fixtures antigas continham nomes privados; raiz tinha evidências e dados operacionais | Fallback público `Geral`, fixtures fictícias e exclusões no Git; dados locais preservados |

A alteração dos nomes fixos da migração foi autorizada pelo autor. Bancos já instalados mantêm os nomes cadastrados. Nos schemas antigos, as unidades continuam sendo descobertas a partir dos registros de moradores.

## Segurança e dependências

O manifesto não pede Internet nem leitura ampla do armazenamento. FileProvider tem exportação desativada e diretórios limitados; receivers também não são exportados. Não há credenciais de backend ou configuração de serviços externos necessária para executar o projeto.

A consulta ao OSV para 150 coordenadas Maven do runtime de debug identificou alertas em Bouncy Castle 1.72. Depois da atualização, a mesma consulta não retornou alertas para as coordenadas verificadas. Isso descreve o resultado daquela base de vulnerabilidades, sem assegurar que todas as bibliotecas estejam livres de falhas. A atualização é limitada aos módulos existentes do PDFBox; não houve uma atualização indiscriminada do restante do catálogo. [Notas oficiais do Bouncy Castle 1.86](https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-86/).

O lint também aponta um trust manager permissivo dentro de **bcpkix**, em código transitivo de rede. O aplicativo não usa esse caminho e não tem permissão de Internet. O aviso foi mantido visível; se uma integração de rede for acrescentada, esse caminho precisa de uma revisão específica. A documentação não confunde ausência de uso atual com segurança universal da biblioteca.

Backups e banco não têm criptografia adicional. As validações do ZIP protegem extração e integridade, mas não provam autoria. O backup automático do Android continua habilitado; uma política nova para excluir ou criptografar dados exige avaliar restauração, transferência entre aparelhos e recuperação dos anexos.

## Organização e legibilidade

As pastas e os contratos de repositório fornecem separação suficiente para a escala atual. Extratos e arquivos ficam fora da interface; regras de conferência podem ser testadas na JVM. Serviços de backup concentram formatos e validações. Separar tudo em módulos ou introduzir uma camada genérica de abstrações agora aumentaria custo sem resolver os defeitos encontrados.

Há mistura de nomes históricos em inglês com textos de interface em português, strings ainda diretamente no Kotlin, helpers de compatibilidade e parâmetros de componentes que deixaram de ter função. Renomear o package ou todos os tipos não foi necessário para apresentar o projeto e poderia afetar instalação, dados ou imports. A recomendação é remover helpers sem uso e extrair textos gradualmente quando o fluxo correspondente receber uma mudança funcional.

Componentes de formulários, superfícies, navegação, movimento e tema já são compartilhados. Algumas telas e testes de interface ainda são extensos; uma divisão futura deve acompanhar responsabilidades reais, com evidência de que melhora a manutenção, sem refazer o visual apenas para padronizar arquivos.

## Dívida restante e prioridades

| Prioridade | Evolução | Por que não foi feita nesta revisão |
| --- | --- | --- |
| Alta para contabilidade histórica | Persistir valor pago, data, origem e identificação de cada recebimento | Muda o produto, schema, backups e interpretação dos gráficos; exige decisão de domínio |
| Alta para conferência automatizada | Desambiguar matching e impedir atribuição do mesmo crédito a moradores diferentes | Substring/aliases fazem parte do comportamento atual; uma regra nova precisa de exemplos e critérios do responsável |
| Alta para distribuição real | Definir assinatura, política de backup/proteção de dados e atualizar target SDK conforme a distribuição | Publicar código de portfólio não define essas decisões nem fornece uma chave de produção |
| Média | Representar dinheiro por centavos/decimal | Requer estratégia de arredondamento e migração, não apenas troca de um tipo Kotlin |
| Média | Foreign key de `unitId` e consistência entre importações/mutações concorrentes | Precisa de migração de dados históricos e tratamento de referências inválidas |
| Média | Recuperação durável de restauração após morte abrupta do processo | Room, arquivos, DataStore e alarmes não formam uma transação única; rollback cooperativo não resolve todos os cenários |
| Média | Coleta segura de anexos órfãos e limite para leitura de extratos muito grandes | Remover arquivos antigos exige verificar todas as referências; PDF/CSV ainda podem consumir muita memória |
| Média | Melhorar diagnósticos de CSV e ampliar fixtures de bancos/layouts | Separador, cabeçalhos e descrições variam; não se deve prometer suporte universal sem amostras fictícias |
| Média | Atualização controlada de AGP/Kotlin/Compose e opções de Gradle | Há flags de compatibilidade/depreciações do AGP 9; modernização exige uma rodada própria de build e testes |
| Baixa | Localização completa, subdivisão de telas e retirada de wrappers remanescentes | Ganho de manutenção gradual; alterações cosméticas amplas ficaram fora do escopo |
| Baixa | Atualizar recortes de ano em sessão muito longa durante a virada do ano | Alguns flows inicializam o ano ao criar o ViewModel; normalmente são recriados ao reabrir o app |

## Apresentação pública

README, escopo, arquitetura, identidade, contribuição, política de segurança e avisos de terceiros foram revisados. O workflow valida código, histórico, builds, testes e lint; Dependabot acompanha Gradle e GitHub Actions. As ações são fixadas por SHA e recebem apenas leitura do repositório.

O `.gitignore` exclui chaves, configurações da máquina, cache, APKs, bancos, backups, extratos, capturas de operação, vídeos e notas privadas. O verificador local também analisa os blobs versionados e o histórico. A revisão manual complementa os padrões automáticos, principalmente para dados pessoais que não se parecem com tokens.

Não foi adicionado um banco de demonstração ao aplicativo nem alterado o package de instalação. As imagens públicas são produzidas por testes com dados fictícios. O código autoral permanece sem licença de reutilização até uma escolha explícita do autor.
