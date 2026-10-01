# Contribuindo

O projeto surgiu de uma necessidade real de gestão de uma pequena república. Alterações devem preservar os cadastros, o histórico de pagamentos e a compatibilidade dos backups existentes.

1. Abra uma issue descrevendo o problema, com passos para reproduzir e dados fictícios.
2. Crie uma branch com um nome descritivo, como `fix/backup-validation`.
3. Faça commits pequenos que expliquem o motivo da mudança.
4. Execute os comandos de validação do [README](README.md#validação).
5. Abra um pull request indicando comportamento anterior, resultado e testes realizados.

Não envie extratos reais, contratos, CPFs, telefones, bancos de dados, backups, chaves de assinatura ou capturas com informações pessoais. Os testes usam dados artificiais e bancos isolados.

Antes de alterar um schema Room, crie uma migração que preserve dados, mantenha as versões exportadas em `app/schemas` e amplie o teste de migrações. Evite atualizações de Kotlin, KSP, Hilt ou AGP isoladas sem verificar a compatibilidade do conjunto.

Consulte [arquitetura](docs/ARCHITECTURE.md), [design](DESIGN.md) e [prioridades técnicas](docs/REVIEW.md) antes de mudanças amplas.
