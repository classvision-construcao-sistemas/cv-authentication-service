# Changelog

Data de referencia: **2026-09-05**

### Alteracoes
- Padronizacao das respostas de erro da API com os campos `descricao`, `statusCode`, `solucao` e `dataHora`.
- Cricao de tratamento global de excecoes para respostas mais consistentes.
- Ajuste das regras de seguranca para manter leituras autenticadas e acoes administrativas restritas ao perfil `ADMINISTRADOR`.
- Atualizacao da documentacao do projeto com explicacao da aplicacao, da pasta `docs` e do uso de secrets.
- Inclusao do diagrama do banco no `README.md`.
- Migracao do banco H2 para uso em arquivo para facilitar analise com DBeaver.
- Ajuste do modelo de usuarios e perfis para eliminar a tabela de junção e manter apenas `CV01_USUARIOS` e `CV02_ROLES`.

### Documentacao
- `README.md` atualizado com informacoes de uso, estrutura da pasta `docs` e boas praticas de secrets.
- Colecao Postman H2 ajustada para os fluxos atuais da API.

### Observacoes
- O projeto segue voltado para desenvolvimento e testes com H2.
- Para ambiente local com DBeaver, e necessario evitar acesso concorrente ao arquivo do banco ou usar modo server do H2.

