# Escopo do produto

O Aluguéis Nani ajuda na organização cotidiana dos aluguéis de uma república. A pessoa responsável cadastra moradores e unidades, acompanha cada competência, confere extratos, guarda documentos e recebe lembretes no próprio aparelho.

## Fluxos existentes

| Destino | Responsabilidade |
| --- | --- |
| Início | Resumir pagamentos, ocupação e estimativas mensais |
| Locações | Criar, editar e excluir moradores; administrar unidades |
| Receber | Consultar competências e registrar estados de pagamento; conferir PDF/CSV |
| Docs | Anexar e abrir contratos e vistorias; registrar datas relacionadas |
| Configurações | Preferências, backups completos e importação/exportação JSON de versões antigas |

Os dados são locais. O aplicativo não envia mensagens, não processa pagamentos e não consulta contas bancárias.

## Regras preservadas

A conferência considera créditos positivos do mês selecionado e relaciona descrições aos nomes ou aliases normalizados. Um nome sem conteúdo normalizado não pode corresponder a todos os créditos. O resultado pode ser pago, parcial ou pendente, com a tolerância monetária existente de menos de R$ 1,00.

As regras opcionais de atraso continuam configuráveis. O vencimento se ajusta ao último dia quando o mês não possui o dia cadastrado. A identidade de um pagamento é a combinação de morador, ano e mês.

Os resultados calculados a partir de um extrato são diferentes dos estados de pagamento persistidos. A base não armazena comprovantes, datas efetivas ou valores efetivamente pagos. A mudança para uma contabilidade histórica completa exige uma evolução explícita do produto e do schema.

## Dados e portabilidade

O formato completo `.nani` inclui tabelas, preferências, lembretes e anexos disponíveis. A importação valida estrutura, referências e integridade dos arquivos antes de substituir os dados. Backups antigos em JSON continuam suportados, mas não embutem documentos.

A publicação usa somente fixtures fictícias. Os nomes de unidades instaladas são mantidos no banco de cada usuário; a lista privada que existia no código da migração foi retirada. O fallback público é `Geral`, e a migração recupera as unidades a partir dos registros antigos.

As limitações e prioridades futuras estão em [docs/REVIEW.md](docs/REVIEW.md).
