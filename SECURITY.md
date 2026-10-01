# Segurança e privacidade

O aplicativo armazena dados localmente e não possui backend, conta de usuário ou telemetria. WhatsApp, discador e leitor de PDF são abertos mediante ação do usuário. O manifesto não solicita acesso à Internet nem leitura ampla do armazenamento.

O banco Room e os anexos não implementam criptografia adicional. O arquivo `.nani` contém dados pessoais e documentos; hashes verificam integridade, sem criptografar ou autenticar o arquivo. Guarde backups em um local protegido. O backup automático do Android depende das configurações do aparelho; a cópia manual completa é o caminho portátil validado pelo projeto.

Para relatar uma vulnerabilidade, use a aba **Security → Report a vulnerability** do repositório. Não publique dados pessoais ou arquivos operacionais em issues. Descreva o problema com informações artificiais.

`scripts/check_publication.py` verifica os arquivos versionados e padrões de credenciais no histórico. Essa verificação complementa a revisão manual e o secret scanning do GitHub; não é uma garantia de detecção de todo tipo de segredo.

Chaves de produção, propriedades locais do SDK, caches, arquivos de operação e evidências privadas de desenvolvimento ficam fora do versionamento. Se uma credencial for exposta, revogue-a e remova-a também do histórico antes de continuar publicando.
