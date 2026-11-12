# CV Authentication Service

Microsserviço de **autenticação e gestão de usuários** do ClassVision. Ele autentica usuários, emite tokens **JWT** com os dados e o perfil (role) de cada um, e expõe rotas para o administrador gerenciar contas.

O front-end e os demais microsserviços usam o token emitido aqui para saber **quem é o usuário**, **a qual instituição ele pertence** e **o que ele pode fazer**.

Todo usuário pertence a uma **instituição**. As instituições e os seus administradores (`ADMIN_INSTITUICAO`) são cadastrados pelo administrador geral (`ADMINISTRADOR`). A partir daí, cada administrador de instituição gerencia os usuários **da própria instituição**.

> Data de referência deste documento: **2026-11-12**.

## Sumário

1. [Visão geral do fluxo](#1-visão-geral-do-fluxo)
2. [Convenções da API](#2-convenções-da-api)
3. [Perfis (roles) e permissões](#3-perfis-roles-e-permissões)
4. [Login](#4-login)
5. [Token JWT: conteúdo e como decodificar](#5-token-jwt-conteúdo-e-como-decodificar)
6. [Instituições](#6-instituições)
7. [Cadastro de usuários](#7-cadastro-de-usuários)
8. [Gestão de usuários](#8-gestão-de-usuários)
9. [Padrão de erros](#9-padrão-de-erros)
10. [Exemplo de fluxo completo (curl)](#10-exemplo-de-fluxo-completo-curl)
11. [Pontos de atenção e limitações conhecidas](#11-pontos-de-atenção-e-limitações-conhecidas)
12. [Como executar localmente](#12-como-executar-localmente)
13. [Configuração e variáveis de ambiente](#13-configuração-e-variáveis-de-ambiente)
14. [Banco de dados](#14-banco-de-dados)
15. [Conteúdo da pasta `docs`](#15-conteúdo-da-pasta-docs)

---

## 1. Visão geral do fluxo

```
┌──────────┐  1. POST /api/auth/login (email, senha)   ┌──────────────────────────┐
│  Front / │ ────────────────────────────────────────▶ │  cv-authentication-      │
│  cliente │ ◀──────────────────────────────────────── │  service                 │
└──────────┘  2. { token: "eyJ..." }                   └──────────────────────────┘
     │
     │ 3. Lê id, nome, email, role e instituicaoId direto do token (tela/menus)
     │
     │ 4. Authorization: Bearer eyJ...
     ▼
┌─────────────────────────┐
│ Demais microsserviços   │  5. Validam a assinatura do token com o mesmo segredo
│ (ou rotas deste serviço)│     e liberam/negam o acesso pela role
└─────────────────────────┘
```

1. O usuário faz login com e-mail e senha.
2. O serviço devolve um **token JWT** assinado (HS512), válido por 1 hora.
3. O front decodifica o token para obter `id`, `nome`, `email`, `role` e `instituicaoId` e controlar o que aparece na tela.
4. Todas as chamadas protegidas enviam o token no cabeçalho `Authorization: Bearer <token>`.
5. Os microsserviços **validam a assinatura** do token, usando o mesmo segredo, antes de confiar nos dados.

**Não existe autocadastro.** O fluxo de implantação de uma instituição é:

1. O `ADMINISTRADOR` geral cadastra a **instituição**.
2. O `ADMINISTRADOR` geral cadastra o **administrador da instituição** (`ADMIN_INSTITUICAO`).
3. O `ADMIN_INSTITUICAO` cadastra os demais usuários (coordenadores, professores...) da instituição dele.

---

## 2. Convenções da API

| Item | Valor |
|---|---|
| Base URL local | `http://localhost:8080` |
| Prefixo das rotas | `/api` |
| Padrão das rotas | `/api/{recurso}/{acao-recurso}/{id}`, ex.: `/api/instituicoes/cadastrar-instituicao`, `/api/usuarios/buscar-usuario/5` (lista completa na [seção 3](#matriz-de-permissões-deste-serviço)) |
| Formato | JSON (`Content-Type: application/json`) |
| Autenticação | `Authorization: Bearer <token>` |
| Data (`LocalDate`) | `dd/MM/yyyy`, ex.: `25/03/1990` |
| Data e hora nas respostas de usuário | `dd/MM/yyyy HH:mm:ss`, ex.: `12/11/2026 09:30:00` |
| Data e hora nas respostas de erro (`dataHora`) | ISO-8601, ex.: `2026-11-12T10:04:57.1077599` |
| Swagger (sempre ativo) | `http://localhost:8080/swagger-ui.html` (no Railway: `https://<dominio-da-api>/swagger-ui.html`) |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

> ⚠️ Datas em formato ISO (`2026-11-12`) **não são aceitas** no corpo das requisições e retornam `400`.

---

## 3. Perfis (roles) e permissões

### Resumo rápido

- Existe **um administrador geral** (`ADMINISTRADOR`) que enxerga e gerencia **todas** as instituições.
- Cada instituição tem um ou mais **administradores da instituição** (`ADMIN_INSTITUICAO`). Eles são cadastrados pelo administrador geral e gerenciam **apenas** a própria instituição.
- Todos os outros usuários enxergam **somente a própria instituição**. Dados de outra instituição simplesmente "não existem" para eles (`404`).
- Ninguém consegue dar a alguém, nem a si mesmo, um poder **maior que o seu**.

### Perfis existentes

Cada role tem um **nível de privilégio**. Quanto maior o número, maior o poder. As roles são criadas automaticamente pela aplicação ao iniciar (`DataInitializer`), que cria as que estiverem faltando no banco.

```
 Nível
  100  ADMINISTRADOR ............ administrador geral da plataforma (todas as instituições)
   80  ADMIN_INSTITUICAO ........ administrador de UMA instituição
   60  COORDENADOR .............. coordenador de curso
   40  PROFESSOR / SECRETARIO ... (PROFESSOR é a role padrão)
   20  ASSISTENTE ............... assistente de coordenação
```

| Role | Nível | Enxerga | Pode gerenciar usuários? |
|---|:-:|---|---|
| `ADMINISTRADOR` | 100 | **Todas** as instituições e todos os usuários | Sim, de qualquer instituição |
| `ADMIN_INSTITUICAO` | 80 | A própria instituição e os usuários dela | Sim, só da própria instituição e até o nível 80 |
| `COORDENADOR` | 60 | A própria instituição e os usuários dela | **Não**, apenas consulta |
| `PROFESSOR` | 40 | Apenas a própria instituição | Não |
| `SECRETARIO` | 40 | Apenas a própria instituição | Não |
| `ASSISTENTE` | 20 | Apenas a própria instituição | Não |

Os nomes são **sensíveis a maiúsculas/minúsculas**: envie sempre em maiúsculas (`COORDENADOR`, não `coordenador`).

### Como a API decide se você pode ou não

Em toda requisição, a API faz quatro verificações, **nesta ordem**. A primeira que falhar define a resposta:

| # | Pergunta | Se a resposta for "não" |
|:-:|---|---|
| 1 | Você enviou um token válido, não expirado, de um usuário **ativo**? | `401 Unauthorized` |
| 2 | A sua **role** pode usar esta rota? (ver [matriz](#matriz-de-permissões-deste-serviço)) | `403 Forbidden`: `Acesso negado para este recurso.` |
| 3 | O usuário ou a instituição que você quer acessar é **da sua instituição**? (o `ADMINISTRADOR` sempre passa) | `404 Not Found`: `Usuário não encontrado com ID: ...` |
| 4 | A ação respeita as **regras de nível** e de **ações sobre si mesmo** (abaixo)? | `403 Forbidden`, com o motivo em `descricao` |

> Na verificação 3, a API responde `404` em vez de `403` de propósito: assim ninguém descobre quais usuários ou instituições existem fora da própria instituição.

### O que cada role pode fazer

**`ADMINISTRADOR` (administrador geral)**
- Cadastra, edita, ativa e desativa **instituições**, inclusive o CNPJ.
- Cadastra usuários em **qualquer** instituição e com **qualquer** role, inclusive outro `ADMINISTRADOR`. É ele quem cria o primeiro `ADMIN_INSTITUICAO` de cada instituição.
- Lista, busca, edita, ativa, desativa e exclui **qualquer** usuário.
- É o único que **transfere** um usuário de uma instituição para outra.

**`ADMIN_INSTITUICAO` (administrador da instituição)**
- Cadastra usuários **na própria instituição**, com qualquer role **até `ADMIN_INSTITUICAO`**. Ele pode criar outros administradores da instituição, mas nunca um `ADMINISTRADOR`. O `instituicaoId` pode ser omitido, porque o usuário é sempre criado na instituição dele.
- Lista, busca, edita, ativa, desativa e exclui usuários **da própria instituição** com nível **igual ou menor** que o dele.
- Altera a role desses usuários, sempre até `ADMIN_INSTITUICAO`.
- Edita os dados da **própria** instituição (nome e telefone), mas **não** o CNPJ.
- **Não** cria, ativa nem desativa instituições, e **não** transfere usuários para outra instituição.

**`COORDENADOR`**
- **Lista e busca** os usuários da própria instituição.
- Consulta a própria instituição.
- **Não** cadastra, edita, ativa, desativa nem exclui ninguém.

**`PROFESSOR`, `SECRETARIO` e `ASSISTENTE`**
- Fazem login e consultam **a própria instituição**.
- **Não** acessam as rotas de usuários.

> Estas são as permissões **deste serviço** (usuários e instituições). O que cada role pode fazer nos **outros** microsserviços (turmas, cursos, auditorias...) é definido por eles, usando a `role` e o `instituicaoId` do token.

### Regras de nível: como a escalada de privilégio é impedida

1. **Só administradores mexem em roles.** Apenas `ADMINISTRADOR` e `ADMIN_INSTITUICAO` cadastram usuários ou alteram a role de alguém.
2. **Você só concede roles até o seu nível.** Um `ADMIN_INSTITUICAO` (80) pode dar `ADMIN_INSTITUICAO`, `COORDENADOR`, `PROFESSOR`, `SECRETARIO` ou `ASSISTENTE`, mas nunca `ADMINISTRADOR` (100).
3. **Você não gerencia quem está acima de você.** Editar, trocar a senha, ativar, desativar ou excluir só é possível para usuários de nível **igual ou menor** que o seu. Por isso, um `ADMIN_INSTITUICAO` da própria ClassVision não consegue mexer no administrador geral.
4. **Você não mexe na própria role nem no próprio acesso.** Ninguém altera a própria role, se desativa ou se exclui. Isso impede tanto a autopromoção quanto que um admin se tranque para fora por engano. Editar os **próprios dados** (nome, e-mail, senha) continua permitido.
5. **Mudar de instituição é só com o administrador geral.** Um `ADMIN_INSTITUICAO` não "empurra" usuários para outra instituição nem "puxa" usuários de outra para a dele.
6. **Desativado perde o acesso na hora.** Quando um usuário é desativado, o token que ele já tinha para de funcionar imediatamente neste serviço (`401`), sem esperar a expiração.

### Exemplos práticos

Cenário: instituições **A** e **B**. `AdminA` é `ADMIN_INSTITUICAO` de A, `CoordA` é `COORDENADOR` de A, `ProfA` é `PROFESSOR` de A, e `ProfB` é `PROFESSOR` de B.

| Quem | Tenta | Resultado | Por quê |
|---|---|:-:|---|
| `ADMINISTRADOR` | Cadastrar a instituição B | ✅ `201` | Só ele cria instituições |
| `ADMINISTRADOR` | Cadastrar `AdminA` como `ADMIN_INSTITUICAO` de A | ✅ `201` | Ele concede qualquer role, em qualquer instituição |
| `AdminA` | Cadastrar `CoordA` (sem informar `instituicaoId`) | ✅ `201` | Vai automaticamente para a instituição A |
| `AdminA` | Cadastrar alguém na instituição B | ❌ `403` | Só cadastra na própria instituição |
| `AdminA` | Cadastrar alguém como `ADMINISTRADOR` | ❌ `403` | Role acima do nível dele |
| `AdminA` | Promover `ProfA` a `ADMIN_INSTITUICAO` | ✅ `200` | Até o próprio nível é permitido |
| `AdminA` | Promover `ProfA` a `ADMINISTRADOR` | ❌ `403` | Role acima do nível dele |
| `AdminA` | Transferir `ProfA` para a instituição B | ❌ `403` | Só o administrador geral transfere |
| `AdminA` | Buscar, editar ou desativar `ProfB` | ❌ `404` | `ProfB` é de outra instituição |
| `AdminA` | Alterar a própria role para `COORDENADOR` | ❌ `403` | Ninguém altera a própria role |
| `AdminA` | Alterar o próprio nome ou senha | ✅ `200` | Os próprios dados podem ser editados |
| `AdminA` | Desativar ou excluir a si mesmo | ❌ `403` | Ninguém tira o próprio acesso |
| `AdminA` | Editar o nome da instituição A | ✅ `200` | Pode editar a própria instituição |
| `AdminA` | Alterar o CNPJ da instituição A | ❌ `403` | CNPJ só o administrador geral altera |
| `AdminA` | Desativar a instituição A | ❌ `403` | Só o administrador geral ativa/desativa instituições |
| `ADMIN_INSTITUICAO` da ClassVision | Editar ou desativar o administrador geral | ❌ `403` | Alvo com nível acima do dele |
| `CoordA` | Listar os usuários | ✅ `200` | Recebe só os usuários da instituição A |
| `CoordA` | Editar `ProfA` ou cadastrar alguém | ❌ `403` | Coordenador só consulta |
| `ProfA` | Listar os usuários | ❌ `403` | Professor não acessa as rotas de usuários |
| `ProfA` | Listar as instituições | ✅ `200` | Recebe só a instituição A |
| `CoordA` desativado | Usar o token que já tinha | ❌ `401` | Desativação vale na hora |

### Matriz de permissões deste serviço

| Método | Rota | ADMINISTRADOR | ADMIN_INSTITUICAO | COORDENADOR | Demais roles |
|---|---|:-:|:-:|:-:|:-:|
| `POST` | `/api/auth/login` (público) | ✅ | ✅ | ✅ | ✅ |
| `POST` | `/api/usuarios/cadastrar-usuario` | ✅ | ✅ ¹ | ❌ | ❌ |
| `GET` | `/api/usuarios/listar-usuarios` | ✅ todos | ✅ ² | ✅ ² | ❌ |
| `GET` | `/api/usuarios/buscar-usuario/{id}` | ✅ | ✅ ² | ✅ ² | ❌ |
| `GET` | `/api/usuarios/buscar-usuario-por-email/{email}` | ✅ | ✅ ² | ✅ ² | ❌ |
| `PUT` | `/api/usuarios/atualizar-usuario/{id}` | ✅ | ✅ ¹ ³ | ❌ | ❌ |
| `PATCH` | `/api/usuarios/desativar-usuario/{id}` | ✅ | ✅ ³ | ❌ | ❌ |
| `PATCH` | `/api/usuarios/ativar-usuario/{id}` | ✅ | ✅ ³ | ❌ | ❌ |
| `DELETE` | `/api/usuarios/excluir-usuario/{id}` | ✅ | ✅ ³ | ❌ | ❌ |
| `POST` | `/api/instituicoes/cadastrar-instituicao` | ✅ | ❌ | ❌ | ❌ |
| `GET` | `/api/instituicoes/listar-instituicoes` | ✅ todas | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ |
| `GET` | `/api/instituicoes/buscar-instituicao/{id}` | ✅ | ✅ ⁴ | ✅ ⁴ | ✅ ⁴ |
| `PUT` | `/api/instituicoes/atualizar-instituicao/{id}` | ✅ | ✅ ⁴ ⁵ | ❌ | ❌ |
| `PATCH` | `/api/instituicoes/ativar-instituicao/{id}` | ✅ | ❌ | ❌ | ❌ |
| `PATCH` | `/api/instituicoes/desativar-instituicao/{id}` | ✅ | ❌ | ❌ | ❌ |

¹ Somente na própria instituição e concedendo roles até `ADMIN_INSTITUICAO`.
² Somente usuários da própria instituição.
³ Somente usuários da própria instituição, de nível igual ou menor, e nunca desativar/excluir a si mesmo nem alterar a própria role.
⁴ Somente a própria instituição.
⁵ Sem alterar o CNPJ.

### Mensagens de erro de permissão

| Status | `descricao` | Quando acontece |
|:-:|---|---|
| `401` | `Acesso nao autorizado.` | Sem token, token inválido/expirado ou usuário desativado |
| `403` | `Acesso negado para este recurso.` | A sua role não pode usar a rota |
| `403` | `Você não pode conceder uma role acima do seu nível de acesso (ADMIN_INSTITUICAO).` | Cadastro ou edição com role acima do seu nível |
| `403` | `Você não pode gerenciar um usuário com nível de acesso acima do seu.` | Editar, ativar, desativar ou excluir alguém acima de você |
| `403` | `Você não pode alterar a sua própria role.` | Mudar a própria role |
| `403` | `Você não pode desativar o seu próprio usuário.` (ou `excluir`) | Desativar ou excluir a si mesmo |
| `403` | `Você só pode cadastrar usuários na sua própria instituição.` | `ADMIN_INSTITUICAO` informando outra instituição no cadastro |
| `403` | `Somente o administrador geral pode transferir usuários entre instituições.` | `ADMIN_INSTITUICAO` trocando a instituição de um usuário |
| `403` | `Somente o administrador geral pode alterar o CNPJ da instituição.` | `ADMIN_INSTITUICAO` alterando o CNPJ |
| `404` | `Usuário não encontrado com ID: ...` / `Instituição não encontrada com ID: ...` | O recurso não existe **ou** é de outra instituição |

### Perguntas frequentes

**Como uma nova instituição começa a usar o sistema?**
O `ADMINISTRADOR` cadastra a instituição (`POST /api/instituicoes/cadastrar-instituicao`) e, em seguida, o administrador dela (`POST /api/usuarios/cadastrar-usuario` com `"role": "ADMIN_INSTITUICAO"` e o `instituicaoId` da nova instituição). A partir daí, o `ADMIN_INSTITUICAO` cadastra o restante da equipe.

**Uma instituição pode ter mais de um `ADMIN_INSTITUICAO`?**
Sim. O `ADMINISTRADOR` ou um `ADMIN_INSTITUICAO` da própria instituição pode cadastrar ou promover outros.

**Um `ADMIN_INSTITUICAO` pode rebaixar ou desativar outro `ADMIN_INSTITUICAO` da mesma instituição?**
Sim. Eles têm o mesmo nível, e a regra bloqueia apenas quem está **acima**. Só não pode fazer isso consigo mesmo.

**O que acontece se a instituição ficar sem nenhum `ADMIN_INSTITUICAO`?**
O `ADMINISTRADOR` geral cadastra ou promove um novo. Ele sempre tem acesso a todas as instituições.

**O front deve esconder botões pela role?**
Sim, para uma boa experiência: use `role` e `instituicaoId` do token (ver [seção 5](#5-token-jwt-conteúdo-e-como-decodificar)). Mas quem **garante** a segurança é a API: mesmo que o botão apareça por engano, a requisição será recusada.

**Mudei a role de alguém. Quando a mudança vale?**
Neste serviço, **na hora**: as permissões são conferidas no banco a cada requisição. Já o **token** dessa pessoa continua com a role antiga até expirar (até 1 hora). Por isso o front e os outros microsserviços, que leem o token, só veem a nova role depois de um **novo login**.

### Usuário administrador padrão

Se ainda não existir, o serviço cria na inicialização o administrador e a instituição padrão à qual ele pertence:

| E-mail | Senha | Instituição |
|---|---|---|
| `admin@classvision.com` | `admin123456` | `ClassVision` (CNPJ provisório `00000000000000`) |

> 🔒 **Troque essa senha em qualquer ambiente que não seja local.**
>
> O CNPJ `00000000000000` da instituição padrão é provisório e **não passa na validação de CNPJ**. Para editar essa instituição via `PUT`, informe o CNPJ real. Depois da troca, nenhuma nova instituição padrão é criada nas próximas inicializações.

---

## 4. Login

`POST /api/auth/login` (público)

### Requisição

```json
{
  "email": "admin@classvision.com",
  "senha": "admin123456"
}
```

| Campo | Tipo | Obrigatório | Regras |
|---|---|:-:|---|
| `email` | string | ✅ | Não pode ser vazio; deve ser um e-mail válido |
| `senha` | string | ✅ | Não pode ser vazia |

### Resposta de sucesso: `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJpZCI6MSwibm9tZSI6...",
  "tipo": "Bearer",
  "expiresIn": 3600000
}
```

| Campo | Descrição |
|---|---|
| `token` | JWT a ser enviado nas próximas requisições |
| `tipo` | Sempre `Bearer`: use `Authorization: Bearer <token>` |
| `expiresIn` | Validade do token **em milissegundos** (`3600000` = 1 hora) |

A resposta traz **apenas o token**. Os dados do usuário (`id`, `nome`, `email`, `role`, `instituicaoId`) ficam **dentro do token** (veja a [seção 5](#5-token-jwt-conteúdo-e-como-decodificar)).

### Regras

- O e-mail é o identificador de login.
- Usuários **desativados** (`ativo = false`) não conseguem fazer login.
- Por segurança, e-mail inexistente, senha errada e usuário desativado retornam **a mesma mensagem**: o cliente não sabe qual dos três aconteceu.
- Não existe refresh token: quando o token expira, é preciso fazer login novamente.

### Erros

| Status | Quando | `descricao` |
|---|---|---|
| `400` | E-mail vazio/inválido ou senha vazia | Ex.: `O campo 'senha' não pode estar em branco.` |
| `400` | JSON malformado | `Corpo da requisicao invalido ou com campo em formato incorreto.` |
| `401` | Credenciais inválidas ou usuário desativado | `Email ou senha inválidos.` |

---

## 5. Token JWT: conteúdo e como decodificar

### Estrutura

Um JWT tem três partes separadas por ponto: `HEADER.PAYLOAD.ASSINATURA`.

- **Header:** `{"alg":"HS512"}`
- **Payload** (os dados):

```json
{
  "id": 1,
  "nome": "Administrador",
  "email": "admin@classvision.com",
  "role": "ADMINISTRADOR",
  "instituicaoId": 1,
  "roles": ["ROLE_ADMINISTRADOR"],
  "sub": "admin@classvision.com",
  "iat": 1794488290,
  "exp": 1794491890
}
```

| Claim | Tipo | Descrição | Uso recomendado |
|---|---|---|---|
| `id` | número | ID do usuário no banco | Identificar o usuário em outros serviços (chave estrangeira, auditoria) |
| `nome` | string | Nome do usuário | Exibir na interface |
| `email` | string | E-mail do usuário | Exibir / contato |
| `role` | string | Role **sem prefixo** (`ADMINISTRADOR`, `PROFESSOR`...) | Comparação direta no front e nos microsserviços |
| `instituicaoId` | número | ID da instituição do usuário | **Filtrar os dados pela instituição** nos microsserviços (um usuário só deve ver dados da própria instituição) |
| `roles` | lista de strings | Role **com prefixo** `ROLE_` | Formato esperado pelo Spring Security (`hasRole(...)`) |
| `sub` | string | *Subject*: e-mail do usuário | Padrão JWT; usado por este serviço para localizar o usuário |
| `iat` | número | Emitido em (Unix timestamp **em segundos**) | Informativo |
| `exp` | número | Expira em (Unix timestamp **em segundos**) | Verificar se o token ainda é válido |

### ⚠️ O token é legível, mas não é alterável

O payload do JWT é apenas codificado em **Base64URL, não é criptografado**. Qualquer pessoa com o token consegue ler o conteúdo (por exemplo, colando em [jwt.io](https://jwt.io)). Por isso:

- O token **nunca** carrega senha, CPF, data de nascimento ou outros dados sensíveis.
- A **assinatura** garante que ninguém consegue **alterar** os dados sem invalidar o token.
- Em produção, use sempre **HTTPS**. É ele que protege o token na rede.

### Decodificar vs. validar

| | Decodificar | Validar |
|---|---|---|
| O que faz | Só lê o payload | Confere a assinatura com o segredo e checa a expiração |
| Precisa do segredo? | Não | Sim (`JWT_SECRET`) |
| Onde usar | **Front-end**, só para montar a interface (nome, menus por role) | **Back-end / microsserviços**, sempre, antes de liberar qualquer dado ou ação |

> O front **nunca** recebe o segredo. Esconder um botão pela role melhora a experiência do usuário, mas a **segurança de verdade** fica no back-end, que valida o token em toda requisição.

### Front-end (JavaScript/TypeScript): decodificar

Sem bibliotecas, com suporte a acentos (UTF-8):

```js
function decodificarToken(token) {
  const payloadBase64Url = token.split('.')[1];
  const base64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/');
  const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

function tokenExpirado(token) {
  const { exp } = decodificarToken(token);
  return exp * 1000 <= Date.now(); // exp vem em segundos
}

// Uso
const { token } = await (await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, senha }),
})).json();

const usuario = decodificarToken(token);
// usuario.id, usuario.nome, usuario.email, usuario.role, usuario.instituicaoId

if (usuario.role === 'ADMINISTRADOR') {
  // exibir menu de administração
}
```

Alternativa com biblioteca: [`jwt-decode`](https://www.npmjs.com/package/jwt-decode) (`import { jwtDecode } from 'jwt-decode'; jwtDecode(token)`).

Enviando o token nas requisições:

```js
fetch('http://localhost:8080/api/usuarios/listar-usuarios', {
  headers: { Authorization: `Bearer ${token}` },
});
```

Boas práticas no front:
- Antes de chamar a API, verifique `tokenExpirado(token)`. Se expirou, redirecione para o login.
- Trate `401` como "sessão expirada/inválida" (volte para o login) e `403` como "sem permissão".
- Prefira guardar o token em memória ou `sessionStorage`. `localStorage` fica exposto em caso de XSS.

### Microsserviço Java com Spring Security (recomendado)

Dependência: no Spring Boot 4, `spring-boot-starter-security-oauth2-resource-server`; no Spring Boot 3, `spring-boot-starter-oauth2-resource-server`.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Usa a claim "roles" (já com prefixo ROLE_) como authorities
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

Lendo os dados do usuário no controller:

```java
@GetMapping("/minhas-turmas")
@PreAuthorize("hasAnyRole('PROFESSOR', 'COORDENADOR')")
public List<TurmaDTO> minhasTurmas(@AuthenticationPrincipal Jwt jwt) {
    Long usuarioId = ((Number) jwt.getClaim("id")).longValue();
    Long instituicaoId = ((Number) jwt.getClaim("instituicaoId")).longValue();
    String nome = jwt.getClaimAsString("nome");
    String role = jwt.getClaimAsString("role");
    // Sempre filtre pela instituição do token, nunca por um parâmetro enviado pelo cliente
    return turmaService.listarPorProfessor(instituicaoId, usuarioId);
}
```

### Microsserviço Java sem Spring Security (biblioteca jjwt 0.12+)

```java
SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)   // lança exceção se a assinatura for inválida ou o token estiver expirado
        .getPayload();

Long id = ((Number) claims.get("id")).longValue();
Long instituicaoId = ((Number) claims.get("instituicaoId")).longValue();
String nome = claims.get("nome", String.class);
String role = claims.get("role", String.class);
```

### Microsserviço Node.js (biblioteca `jsonwebtoken`)

```js
const jwt = require('jsonwebtoken');

function autenticar(req, res, next) {
  const [, token] = (req.headers.authorization || '').split(' ');
  try {
    req.usuario = jwt.verify(token, process.env.JWT_SECRET, { algorithms: ['HS512'] });
    next();
  } catch {
    res.status(401).json({ descricao: 'Token inválido ou expirado.' });
  }
}

const exigirRole = (...roles) => (req, res, next) =>
  roles.includes(req.usuario.role) ? next() : res.status(403).json({ descricao: 'Acesso negado.' });

app.get('/api/relatorios', autenticar, exigirRole('ADMINISTRADOR', 'COORDENADOR'), handler);
```

### Requisitos do segredo (`JWT_SECRET`)

- Todos os serviços que validam o token precisam do **mesmo** valor de `jwt.secret` usado aqui.
- Use um segredo com **no mínimo 64 caracteres**. A biblioteca escolhe o algoritmo pelo tamanho da chave (≥ 64 bytes → HS512). Um segredo menor muda o algoritmo para HS384 ou HS256 e quebra os serviços configurados para HS512.
- Nunca coloque o segredo no código do front-end nem em repositórios.

---

## 6. Instituições

Cada usuário pertence a **uma** instituição. As instituições são cadastradas, ativadas e desativadas apenas pelo `ADMINISTRADOR` geral. O `ADMIN_INSTITUICAO` pode atualizar os dados da própria instituição (exceto o CNPJ). Qualquer usuário autenticado consulta **a própria** instituição (por exemplo, para o front exibir o nome dela).

### Campos

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | número | Identificador (gerado pelo servidor) |
| `nome` | string | Nome da instituição |
| `cnpj` | string | 14 caracteres, **sem pontuação**. Aceita o CNPJ **numérico** e o novo **alfanumérico** (em vigor desde julho/2026). **Único** no sistema |
| `telefone` | string | DDD + número, **somente dígitos** (10 ou 11 dígitos) |
| `situacao` | string | `ATIVA` ou `INATIVA` |
| `criadoEm` / `atualizadoEm` | string | `dd/MM/yyyy HH:mm:ss`, preenchidos pelo servidor |

### Cadastrar: `POST /api/instituicoes/cadastrar-instituicao`

**Permissão:** `ADMINISTRADOR`.

```json
{
  "nome": "Instituto Federal Goiano - Campus Urutaí",
  "cnpj": "10.651.417/0005-00",
  "telefone": "(64) 3465-1900"
}
```

| Campo | Obrigatório | Regras |
|---|:-:|---|
| `nome` | ✅ | Não pode ser vazio |
| `cnpj` | ✅ | CNPJ válido (dígitos verificadores conferidos), com ou sem pontuação. Letras são convertidas para maiúsculas. |
| `telefone` | ✅ | DDD + número, com ou sem máscara: `(64) 3465-1900`, `64 99999-8888`, `6434651900` |

- O servidor **remove a pontuação** do CNPJ e a máscara do telefone antes de salvar. A verificação de CNPJ duplicado já usa o valor normalizado, então `10.651.417/0005-00` e `10651417000500` são tratados como o mesmo CNPJ.
- Toda instituição nasce `ATIVA`. A `situacao` não é enviada no corpo: ela só muda pelas rotas de ativar/desativar.

Resposta: `201 Created`

```json
{
  "id": 2,
  "nome": "Instituto Federal Goiano - Campus Urutaí",
  "cnpj": "10651417000500",
  "telefone": "6434651900",
  "situacao": "ATIVA",
  "criadoEm": "12/11/2026 10:43:28",
  "atualizadoEm": "12/11/2026 10:43:28"
}
```

### Listar: `GET /api/instituicoes/listar-instituicoes`

**Permissão:** qualquer usuário autenticado. O `ADMINISTRADOR` geral recebe **todas** as instituições (ativas e inativas). Os demais recebem uma lista com **apenas a própria** instituição. Não há paginação.

### Buscar por ID: `GET /api/instituicoes/buscar-instituicao/{id}`

**Permissão:** qualquer usuário autenticado, apenas para a **própria** instituição (o `ADMINISTRADOR` geral consulta qualquer uma). `200`, ou `404` (`Instituição não encontrada com ID: 99`), inclusive para instituições de outros usuários.

### Atualizar: `PUT /api/instituicoes/atualizar-instituicao/{id}`

**Permissão:** `ADMINISTRADOR` (qualquer instituição) e `ADMIN_INSTITUICAO` (somente a própria, **sem alterar o CNPJ**: envie o CNPJ atual). Mesmo corpo e mesmas validações do cadastro (`nome`, `cnpj` e `telefone` obrigatórios). A `situacao` não é alterada por aqui. Respostas: `200`, `400`, `403` (CNPJ alterado por quem não é `ADMINISTRADOR`), `404` ou `409`.

### Ativar / desativar: `PATCH /api/instituicoes/ativar-instituicao/{id}` e `PATCH /api/instituicoes/desativar-instituicao/{id}`

**Permissão:** `ADMINISTRADOR`. Altera a `situacao` para `ATIVA` ou `INATIVA`. Retorna `200` com a instituição.

Efeitos de uma instituição **`INATIVA`**:
- Novos usuários **não podem** ser cadastrados nela, e usuários **não podem ser transferidos** para ela (`400`).
- Usuários que **já pertencem** a ela continuam podendo fazer login e ser editados, mantendo a instituição atual.

> Não existe exclusão de instituição: use a desativação. Excluir quebraria o vínculo dos usuários e os dados dos outros microsserviços.

### Erros

| Status | Quando | Exemplo de `descricao` |
|---|---|---|
| `400` | Campo vazio | `O campo 'nome' não pode estar em branco.` |
| `400` | CNPJ inválido | `O campo 'cnpj' deve ser um CNPJ válido.` |
| `400` | Telefone fora do formato | `O campo 'telefone' deve conter DDD + número, ex.: (64) 3465-1900 ou 64999998888.` |
| `403` | Cadastro, ativação ou desativação por quem não é `ADMINISTRADOR` | `Acesso negado para este recurso.` |
| `403` | `ADMIN_INSTITUICAO` tentando alterar o CNPJ | `Somente o administrador geral pode alterar o CNPJ da instituição.` |
| `404` | Instituição inexistente | `Instituição não encontrada com ID: 99` |
| `409` | CNPJ já cadastrado | `CNPJ já existente na base de dados.` |

---

## 7. Cadastro de usuários

`POST /api/usuarios/cadastrar-usuario` (**`ADMINISTRADOR` ou `ADMIN_INSTITUICAO`**)

### Requisição

```http
POST /api/usuarios/cadastrar-usuario
Authorization: Bearer <token-do-administrador>
Content-Type: application/json
```

```json
{
  "nome": "Maria Souza",
  "cpf": "52998224725",
  "email": "maria.souza@classvision.com",
  "senha": "senhaForte123",
  "dataNascimento": "25/03/1990",
  "role": "COORDENADOR",
  "instituicaoId": 2
}
```

| Campo | Tipo | Obrigatório | Regras |
|---|---|:-:|---|
| `nome` | string | ✅ | Não pode ser vazio |
| `cpf` | string | ✅ | CPF **válido** (dígitos verificadores conferidos); **único** no sistema |
| `email` | string | ✅ | E-mail válido; **único** no sistema; será o login |
| `senha` | string | ✅ | Não pode ser vazia; armazenada com hash **BCrypt** (nunca é retornada) |
| `dataNascimento` | string | ❌ | Formato `dd/MM/yyyy` |
| `role` | string | ❌ | Uma das roles da [seção 3](#perfis-existentes), em maiúsculas. Se omitida → `PROFESSOR` |
| `instituicaoId` | número | ✅ / ❌ | ID de uma instituição **existente** e **`ATIVA`** ([seção 6](#6-instituições)). **Obrigatório para o `ADMINISTRADOR`**. Para o `ADMIN_INSTITUICAO` pode ser omitido: o usuário é sempre criado na instituição dele. |

### Regras de negócio

- Apenas `ADMINISTRADOR` e `ADMIN_INSTITUICAO` cadastram usuários. Não há autocadastro.
- O `ADMINISTRADOR` cadastra em qualquer instituição e com qualquer role. É ele quem cria o primeiro `ADMIN_INSTITUICAO` de cada instituição.
- O `ADMIN_INSTITUICAO` cadastra **somente na própria instituição** e com role **até `ADMIN_INSTITUICAO`** (nunca `ADMINISTRADOR`).
- Todo usuário precisa de uma instituição. Cadastre a instituição antes ([seção 6](#6-instituições)).
- E-mail e CPF não podem se repetir. A verificação é feita antes de salvar e retorna `409`.
- **Envie o CPF só com números** (`52998224725`). O formato com pontuação (`529.982.247-25`) também é aceito, mas é gravado como foi enviado, e a verificação de duplicidade compara o texto exato. O mesmo CPF em formatos diferentes passaria como dois CPFs distintos.
- Todo usuário nasce **ativo** (`ativo = true`).
- `criadoEm` e `atualizadoEm` são preenchidos automaticamente pelo servidor.
- A resposta de validação traz **um erro por vez** (o primeiro campo inválido encontrado).

### Resposta de sucesso: `201 Created`

```json
{
  "id": 2,
  "nome": "Maria Souza",
  "email": "maria.souza@classvision.com",
  "cpf": "52998224725",
  "dataNascimento": "25/03/1990",
  "role": "COORDENADOR",
  "instituicaoId": 2,
  "instituicaoNome": "Instituto Federal Goiano - Campus Urutaí",
  "criadoEm": "12/11/2026 10:04:57",
  "atualizadoEm": "12/11/2026 10:04:57",
  "ativo": true
}
```

O cadastro **não** faz login automático: o novo usuário deve chamar `/api/auth/login`.

### Erros

| Status | Quando | Exemplo de `descricao` |
|---|---|---|
| `400` | Campo obrigatório ausente/vazio | `O campo 'nome' não pode estar em branco.` |
| `400` | CPF inválido | `número do registro de contribuinte individual brasileiro (CPF) inválido` |
| `400` | E-mail com formato inválido | `O campo 'email' deve ser um endereço de e-mail válido.` |
| `400` | Data fora de `dd/MM/yyyy` ou JSON malformado | `Corpo da requisicao invalido ou com campo em formato incorreto.` |
| `400` | `instituicaoId` ausente | `O campo 'instituicaoId' é obrigatório.` |
| `400` | Instituição `INATIVA` | `A instituição informada está inativa e não pode receber usuários.` |
| `401` | Sem token / token inválido ou expirado | `Acesso nao autorizado.` |
| `403` | Token de usuário que não é `ADMINISTRADOR` nem `ADMIN_INSTITUICAO` | `Acesso negado para este recurso.` |
| `403` | `ADMIN_INSTITUICAO` cadastrando em outra instituição | `Você só pode cadastrar usuários na sua própria instituição.` |
| `403` | Role acima do nível de quem cadastra | `Você não pode conceder uma role acima do seu nível de acesso (ADMIN_INSTITUICAO).` |
| `404` | Role inexistente (ou em minúsculas) | `Role não encontrada: coordenador` |
| `404` | Instituição inexistente | `Instituição não encontrada com ID: 99` |
| `409` | E-mail já cadastrado | `Email já existente na base de dados.` |
| `409` | CPF já cadastrado | `CPF já existente na base de dados.` |

---

## 8. Gestão de usuários

Todas as rotas exigem `Authorization: Bearer <token>`. Os objetos de usuário retornados seguem o formato da [resposta de cadastro](#resposta-de-sucesso-201-created) (incluindo `instituicaoId` e `instituicaoNome`).

### Listar todos: `GET /api/usuarios/listar-usuarios`

**Permissão:** `ADMINISTRADOR` (todos os usuários), `ADMIN_INSTITUICAO` e `COORDENADOR` (somente os usuários da própria instituição). Retorna `200` com um array de usuários, incluindo os inativos. Não há paginação.

### Buscar por ID: `GET /api/usuarios/buscar-usuario/{id}`

**Permissão:** `ADMINISTRADOR`, `ADMIN_INSTITUICAO` e `COORDENADOR` (os dois últimos, só da própria instituição). `200` com o usuário, ou `404` (`Usuário não encontrado com ID: 99`), inclusive para usuários de outra instituição.

### Buscar por e-mail: `GET /api/usuarios/buscar-usuario-por-email/{email}`

**Permissão:** igual à busca por ID. Ex.: `GET /api/usuarios/buscar-usuario-por-email/maria.souza@classvision.com`. `200` ou `404`.

### Atualizar: `PUT /api/usuarios/atualizar-usuario/{id}`

**Permissão:** `ADMINISTRADOR` e `ADMIN_INSTITUICAO` (só usuários da própria instituição e de nível igual ou inferior ao seu). O `COORDENADOR` **não** edita usuários.

O corpo é **o mesmo do cadastro**, e a atualização é **completa** (não parcial):

- `nome`, `cpf`, `email` e `senha` são **obrigatórios**, com as mesmas validações do cadastro.
- A **senha é sempre substituída** pelo valor enviado. Para manter a senha atual, é preciso reenviá-la.
- `dataNascimento` é sobrescrita: se for omitida, fica vazia.
- `role` é opcional: se omitida, vazia ou igual à atual, **mantém** a role. Para alterar:
  - a nova role precisa estar **até o nível** de quem edita;
  - ninguém altera a **própria** role.
- `instituicaoId` é opcional: se omitido ou igual ao atual, **mantém** a instituição. **Somente o `ADMINISTRADOR`** transfere o usuário para outra instituição, que precisa estar `ATIVA`.
- E-mail e CPF continuam únicos (`409` se pertencerem a outro usuário).
- Qualquer `ADMINISTRADOR` ou `ADMIN_INSTITUICAO` pode editar os **próprios** dados (nome, senha...), sem mudar a própria role.

Respostas: `200` com o usuário atualizado, `400`, `403`, `404` ou `409`.

| `403` — quando | `descricao` |
|---|---|
| Alvo com nível acima do seu | `Você não pode gerenciar um usuário com nível de acesso acima do seu.` |
| Nova role acima do seu nível | `Você não pode conceder uma role acima do seu nível de acesso (ADMIN_INSTITUICAO).` |
| Alterar a própria role | `Você não pode alterar a sua própria role.` |
| Transferir de instituição sem ser `ADMINISTRADOR` | `Somente o administrador geral pode transferir usuários entre instituições.` |

> Se o **e-mail** de um usuário for alterado, os tokens já emitidos para ele deixam de funcionar (`401`), porque o `sub` do token aponta para o e-mail antigo. Ele precisa fazer login novamente.

### Desativar: `PATCH /api/usuarios/desativar-usuario/{id}`

**Permissão:** `ADMINISTRADOR` e `ADMIN_INSTITUICAO` (só usuários da própria instituição e de nível igual ou inferior; nunca a si mesmo). Marca `ativo = false` (*soft delete*): o registro continua no banco, o usuário **não consegue mais fazer login** e o token que ele já tinha **deixa de funcionar na hora**. Retorna `200` com o usuário.

### Ativar: `PATCH /api/usuarios/ativar-usuario/{id}`

**Permissão:** mesmas regras do desativar. Marca `ativo = true`. Retorna `200` com o usuário.

### Excluir: `DELETE /api/usuarios/excluir-usuario/{id}`

**Permissão:** mesmas regras do desativar. Remove o registro **definitivamente** do banco. Retorna `204 No Content`, `403` ou `404`.

> Prefira **desativar** a excluir: outros microsserviços podem guardar o `id` do usuário (turmas, registros, auditoria).

---

## 9. Padrão de erros

Todas as respostas de erro seguem o mesmo formato:

```json
{
  "descricao": "Email já existente na base de dados.",
  "statusCode": 409,
  "solucao": "Use outro valor para os dados unicos (como email ou CPF).",
  "dataHora": "2026-11-12T10:14:11.7330021"
}
```

| Campo | Descrição |
|---|---|
| `descricao` | O que aconteceu (pode ser exibido ao usuário) |
| `statusCode` | Código HTTP (igual ao status da resposta) |
| `solucao` | Sugestão de como resolver |
| `dataHora` | Momento do erro (ISO-8601) |

| Status | Significado |
|---|---|
| `400 Bad Request` | Dados inválidos, campo obrigatório ausente, JSON malformado, data em formato errado, instituição inativa |
| `401 Unauthorized` | Credenciais inválidas no login, ou token ausente/inválido/expirado nas rotas protegidas |
| `403 Forbidden` | Token válido, mas a role não tem permissão para a rota |
| `404 Not Found` | Usuário, instituição, role ou rota não encontrados |
| `409 Conflict` | E-mail, CPF ou CNPJ já cadastrados |
| `500 Internal Server Error` | Erro inesperado no servidor |

> Requisições sem token para rotas inexistentes retornam `401` (e não `404`), porque toda rota que não é pública exige autenticação.

---

## 10. Exemplo de fluxo completo (curl)

```bash
BASE=http://localhost:8080

# 1. Login do administrador
TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@classvision.com","senha":"admin123456"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2. Cadastrar uma instituição (anote o "id" retornado)
curl -X POST $BASE/api/instituicoes/cadastrar-instituicao \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Instituto Federal Goiano - Campus Urutaí","cnpj":"10651417000500","telefone":"(64) 3465-1900"}'

# 3. Cadastrar um coordenador nessa instituição
curl -X POST $BASE/api/usuarios/cadastrar-usuario \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Maria Souza","cpf":"52998224725","email":"maria@classvision.com","senha":"senha123","dataNascimento":"25/03/1990","role":"COORDENADOR","instituicaoId":2}'

# 4. Login do novo usuário
curl -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@classvision.com","senha":"senha123"}'

# 5. Listar usuários (ADMINISTRADOR vê todos; ADMIN_INSTITUICAO/COORDENADOR, só os da instituição)
curl $BASE/api/usuarios/listar-usuarios -H "Authorization: Bearer $TOKEN"

# 6. Desativar o usuário de id 2 (ADMINISTRADOR ou ADMIN_INSTITUICAO da mesma instituição)
curl -X PATCH $BASE/api/usuarios/desativar-usuario/2 -H "Authorization: Bearer $TOKEN"
```

No PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' -Body '{"email":"admin@classvision.com","senha":"admin123456"}'
Invoke-RestMethod -Uri http://localhost:8080/api/usuarios/listar-usuarios -Headers @{ Authorization = "Bearer $($login.token)" }
```

---

## 11. Pontos de atenção e limitações conhecidas

Comportamentos atuais que quem consome o serviço precisa conhecer:

| # | Comportamento | Impacto |
|---|---|---|
| 1 | Os dados do token (`nome`, `role`, `instituicaoId`...) são uma **foto do momento do login**. Neste serviço as permissões são conferidas no banco a cada requisição, mas **os outros microsserviços que só leem o token** continuam vendo a role/instituição antigas até ele expirar (até 1 hora). | O front deve pedir novo login após alterações de perfil. Ações sensíveis em outros serviços podem conferir o usuário aqui. |
| 2 | **Desativação imediata vale só neste serviço.** Aqui o token de um usuário desativado para de funcionar na hora. Outros microsserviços que apenas validam a assinatura do token continuam aceitando-o até expirar. | Mesmo cuidado do item 1. |
| 3 | **Não há configuração de CORS.** | Um front que roda no navegador em outro domínio/porta (ex.: `localhost:4200`) tem as chamadas **bloqueadas pelo navegador**. Chamadas servidor-a-servidor, Postman e curl funcionam. |
| 4 | Não há refresh token nem logout no servidor. | Ao expirar, novo login. "Logout" = o cliente descarta o token. |
| 5 | Não há regra de força mínima de senha. | Qualquer senha não vazia é aceita. |
| 6 | `GET /api/usuarios/listar-usuarios` não tem paginação. | Pode ficar pesado com muitos usuários. |
| 7 | Desativar uma instituição **não bloqueia** o login dos usuários dela. | Apenas impede novos cadastros/vínculos nela. Se a regra for bloquear o acesso, isso precisa ser implementado. |
| 8 | Nada impede que uma instituição fique **sem `ADMIN_INSTITUICAO`** (por exemplo, se o `ADMINISTRADOR` rebaixar ou desativar o único). | O `ADMINISTRADOR` geral pode sempre cadastrar ou promover um novo. |

---

## 12. Como executar localmente

### Pré-requisitos

- **Java 21 ou superior** (testado com JDK 25)
- Não é preciso instalar o Maven: o projeto usa o **Maven Wrapper** (`mvnw` / `mvnw.cmd`), que baixa a versão correta automaticamente.
- Arquivo **`.env`** na raiz do projeto com as credenciais do banco e o `JWT_SECRET` (ver [seção 13](#arquivo-env-execução-local)).

> ⚠️ A aplicação usa **sempre o MySQL do Railway**, inclusive ao rodar na sua máquina. Tudo o que você cadastrar ou excluir localmente acontece no banco **real e compartilhado** com os outros microsserviços.

### Comandos

Windows (PowerShell), na pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run        # sobe a API em http://localhost:8080
.\mvnw.cmd test                   # roda os testes (conectam no banco configurado no .env)
.\mvnw.cmd clean package          # gera o .jar em target\
```

Linux/macOS: use `./mvnw` no lugar de `.\mvnw.cmd`.

Quando aparecer `Started CvAuthenticationServiceApplication` no log, a API está pronta. Na primeira subida com o banco vazio, aguarde também a mensagem `Usuário administrador criado com sucesso!`.

### Testando pelo Swagger

O Swagger fica **sempre ativo**, inclusive no Railway:

- Local: `http://localhost:8080/swagger-ui.html`
- Railway: `https://<dominio-da-api>/swagger-ui.html`

1. Em `POST /api/auth/login`, clique em **Try it out** e envie as credenciais do administrador.
2. Copie o valor de `token` da resposta.
3. Clique em **Authorize** (cadeado no topo), cole **apenas o token**, sem o prefixo `Bearer`, e confirme.
4. As rotas protegidas passam a funcionar. O token fica salvo mesmo se você recarregar a página.

### Testando pelo Postman

Importe os arquivos de `docs/collections/postman/` (collection + environment). A collection tem as pastas **Autenticacao**, **Instituicoes** e **Usuarios**. Todas as requisições, exceto o login, usam automaticamente o token salvo.

Ordem sugerida: **Login** → **Cadastrar instituicao** → **Cadastrar usuario**. Cada uma salva o valor retornado nas variáveis usadas pelas seguintes.

| Variável | Onde fica | Valor inicial | Preenchida automaticamente por |
|---|---|---|---|
| `baseUrl` | environment | `http://localhost:8080` (troque pelo domínio do Railway para testar lá) | — |
| `apiPrefix` | environment | `/api` | — |
| `authToken` | collection | vazio | Login |
| `instituicaoId` | collection | `1` (ClassVision) | Cadastrar instituicao |
| `usuarioId` | collection | `2` | Cadastrar usuario |
| `usuarioEmail` | collection | `coord.teste@classvision.com` | Cadastrar usuario |

> Não crie `authToken`, `usuarioId` ou `instituicaoId` no environment. Variáveis do environment têm prioridade sobre as da collection e impediriam os scripts de atualizar os valores.

---

## 13. Configuração e variáveis de ambiente

Existe um único arquivo de configuração, `src/main/resources/application.properties`, sem perfis: o mesmo vale para a sua máquina e para o Railway. Os valores sensíveis vêm de variáveis de ambiente.

| Propriedade | Valor | Descrição |
|---|---|---|
| `spring.datasource.*` | `jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}` | MySQL do Railway |
| `jwt.secret` | `${JWT_SECRET}` | Segredo de assinatura dos tokens (ver [requisitos](#requisitos-do-segredo-jwt_secret)) |
| `jwt.expiration` | `3600000` | Validade do token em milissegundos (1 hora) |
| `server.port` | `${PORT:8080}` | Porta HTTP (o Railway informa a porta pela variável `PORT`) |

Qualquer propriedade pode ser sobrescrita por variável de ambiente (ex.: `JWT_EXPIRATION`) ou na linha de comando (`--jwt.expiration=7200000`).

### Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|:-:|---|
| `DB_HOST` | ✅ | Host do MySQL. De fora do Railway: o host público (`*.proxy.rlwy.net`) |
| `DB_PORT` | ✅ | Porta do MySQL |
| `DB_NAME` | ✅ | Nome do banco (no Railway: `railway`) |
| `DB_USER` | ✅ | Usuário do banco |
| `DB_PASSWORD` | ✅ | Senha do banco |
| `JWT_SECRET` | ✅ | Segredo dos tokens, **mínimo 64 caracteres**. Gere com `openssl rand -base64 64`. Deve ser o mesmo em todos os microsserviços que validam o token |

Se alguma variável faltar, a aplicação **não sobe** e informa qual placeholder não foi resolvido (ex.: `Could not resolve placeholder 'DB_HOST'`).

### Arquivo `.env` (execução local)

A aplicação lê automaticamente um arquivo `.env` na raiz do projeto, se ele existir (`spring.config.import=optional:file:.env[.properties]`):

1. Copie `.env.example` para `.env`.
2. Preencha os valores com as credenciais do MySQL do Railway (aba **Variables** do serviço MySQL) e um `JWT_SECRET`.
3. Rode `.\mvnw.cmd spring-boot:run`.

> 🔒 O `.env` está no `.gitignore` e **nunca deve ser commitado**. No Railway ele não é usado: as variáveis são cadastradas no próprio serviço.

### Deploy no Railway

1. No projeto do Railway, crie um serviço a partir do repositório GitHub. O Railway detecta o projeto Maven/Java e gera o `.jar` sozinho.
2. Em **Variables** do serviço da aplicação, cadastre as variáveis acima. Se a aplicação e o MySQL estiverem no **mesmo projeto** do Railway, use referências e a rede privada, sem copiar a senha:
   ```
   DB_HOST=${{MySQL.MYSQLHOST}}
   DB_PORT=${{MySQL.MYSQLPORT}}
   DB_NAME=${{MySQL.MYSQLDATABASE}}
   DB_USER=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   JWT_SECRET=<segredo com 64+ caracteres>
   ```
3. Em **Settings → Networking**, gere um domínio público para a API.
4. Se o banco estiver vazio, a aplicação cria na primeira subida as roles, a instituição padrão `ClassVision` e o usuário `admin@classvision.com`. **Troque a senha do admin imediatamente.**

### Boas práticas para segredos

- Não versione valores reais de segredo (JWT, senha de banco, chaves de API). Use o `.env` local e as variáveis do Railway.
- Se uma senha vazar (por exemplo, colada em um chat ou commitada), **gere uma nova** no Railway e atualize o `.env`.
- Se compartilhar o zip de `docs/secret`, remova ou mascare os segredos antes.

---

## 14. Banco de dados

O serviço usa **MySQL 8 no Railway**.

- `spring.jpa.hibernate.ddl-auto=none`: a aplicação **não cria nem altera** tabelas. A estrutura é criada pelos scripts de `docs/database/ddl/`.
- O banco `railway` é **compartilhado** com outros microsserviços do ClassVision (tabelas `cv04_cursos`, `cv05_turmas`, `cv07_auditorias`...). Este serviço usa apenas `cv01_usuarios`, `cv02_roles` e `cv03_instituicoes`.
- Outras tabelas referenciam `cv01_usuarios` e `cv03_instituicoes` por chave estrangeira. Por isso, prefira **desativar** a excluir usuários: excluir um usuário que tem registros em outras tabelas falha.

### Tabelas

| Tabela | Conteúdo |
|---|---|
| `cv02_roles` | Roles (`id`, `nome` único, `descricao`) |
| `cv01_usuarios` | Usuários (`id`, `nome`, `cpf` único, `email` único, `senha` em BCrypt, `data_nascimento`, `ativo`, `criado_em`, `atualizado_em`, `bigint_role_id` → `cv02_roles.id`, `bigint_instituicao_id` → `cv03_instituicoes.id`) |
| `cv03_instituicoes` | Instituições (`id`, `nome`, `cnpj` único, `telefone`, `situacao` (`ATIVA`/`INATIVA`), `criado_em`, `atualizado_em`) |

Os nomes ficam em **minúsculas**: no Linux (Railway), o MySQL diferencia maiúsculas e minúsculas nos nomes de tabela, e o Hibernate procura os nomes em minúsculas.

### Scripts

| Script | Quando usar |
|---|---|
| `docs/database/ddl/DDL-CV-Authentication-Service.sql` | Banco **novo**: cria as três tabelas e insere as roles padrão |
| `docs/database/ddl/migracoes/001-instituicoes.sql` | Banco criado com a versão **antiga** do DDL (sem instituições): cria `cv03_instituicoes`, a instituição padrão e vincula os usuários existentes a ela |
| `docs/database/ddl/migracoes/002-role-admin-instituicao.sql` | Cria a role `ADMIN_INSTITUICAO`. **Opcional**: a aplicação já cria as roles que faltarem ao iniciar (já aplicada no Railway) |

Para executar, use a aba **Data → Query** do serviço MySQL no Railway, ou qualquer cliente MySQL (DBeaver, MySQL Workbench) conectado com as credenciais do `.env`.

![Diagrama do banco de dados](docs/database/diagram/diagram.png)

> O diagrama acima ainda não mostra a tabela `cv03_instituicoes`.

---

## 15. Conteúdo da pasta `docs`

```
docs/
├── architecture/
│   ├── Arquitetura-CV-Authentication.drawio      # desenho editável
│   └── Arquitetura-CV-Authenticationpng.png      # imagem exportada
├── collections/postman/
│   ├── CV-Authentication-Service.postman_collection.json
│   └── CV-Authentication-Service.postman_environment.json
├── database/
│   ├── ddl/
│   │   ├── DDL-CV-Authentication-Service.sql     # MySQL 8 (Railway) - banco novo
│   │   └── migracoes/
│   │       ├── 001-instituicoes.sql              # para bancos já existentes
│   │       └── 002-role-admin-instituicao.sql
│   └── diagram/diagram.png                       # ERD
└── secret/
    └── Variáveis de Ambiente - CV Authentication.zip
```

### Arquitetura da aplicação

![Arquitetura da aplicação](docs/architecture/Arquitetura-CV-Authenticationpng.png)
