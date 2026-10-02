# ThreatLens

ThreatLens é uma plataforma de **Cyber Threat Intelligence (CTI)**, desenvolvida como parte de uma pesquisa acadêmica na UFU. A aplicação centraliza a coleta e a visualização de postagens relevantes de fontes externas (hoje, Telegram), permitindo que analistas de segurança monitorem menções, avaliem o nível de relevância/risco de cada postagem e gerenciem usuários e contas administrativas.

Este repositório reúne as duas partes da aplicação:

| Diretório | Descrição | Stack |
|---|---|---|
| [`backend/`](backend/) | API REST: autenticação, posts, estatísticas, nuvem de palavras e administração | Java 21 + Spring Boot |
| [`frontend/`](frontend/) | Interface web dos analistas: dashboard, posts, alertas e painel de administração | React 18 + Vite |

Cada parte tem um README próprio com mais detalhes: [backend/README.md](backend/README.md) e [frontend/README.md](frontend/README.md).

## Sumário

- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura do Repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar o projeto localmente](#como-rodar-o-projeto-localmente)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Scripts Disponíveis](#scripts-disponíveis)
- [Autenticação](#autenticação)
- [Testes, Lint e Padrões de Código](#testes-lint-e-padrões-de-código)
- [Build e Deploy](#build-e-deploy)
- [Contribuindo](#contribuindo)

## Funcionalidades

- **Dashboard (Overview)** — visão geral com filtros por período (dia, semana, mês, ano, tudo ou intervalo customizado), proporção de posts analisados vs. relevantes, tabela dos posts mais relevantes, gráficos por fonte e por nível de relevância e nuvem de palavras.
- **Posts** — listagem paginada das postagens coletadas, com busca, filtros por fonte, nível de relevância, categoria e período, ordenação por data, score, id, fonte ou conteúdo, e detalhes de cada post em um modal.
- **Estatísticas** — endpoint agregado com total de posts, percentual de posts relevantes no período e distribuição por nível de relevância.
- **Nuvem de palavras** — termos mais frequentes nos posts, calculados via SQL, com stopwords em português e inglês.
- **Alertas** — criação de alertas personalizados por palavra-chave, fonte e frequência de verificação, além de visualização e exclusão.
- **Relatórios** — página reservada para exportação de relatórios de inteligência (em desenvolvimento).
- **Autenticação completa** — cadastro com verificação de código por e-mail (OTP), login com JWT + refresh token rotativo (cookies HttpOnly), "esqueci minha senha" com reset via código e troca de senha autenticada.
- **Minha Conta** — gerenciamento dos dados da própria conta.
- **Painel de Administração** — restrito a usuários com role `ADMIN`: listagem paginada de usuários com busca e filtro por role, alteração de role e exclusão de contas (ambas bloqueadas quando o alvo é o último administrador).
- **Bootstrap automático do primeiro administrador** — se nenhum `ADMIN` existir, o back-end cria um a partir de variáveis de ambiente na subida.
- **Rate limiting e auditoria** — limite de tentativas por IP nos endpoints sensíveis de autenticação e registro de eventos de autenticação (e-mail, IP e timestamp) em um logger dedicado.
- **Tema claro/escuro** e **notificações globais (toasts)** no front-end.

## Arquitetura

```
┌──────────────────────┐   /api/*    ┌──────────────────────┐
│  frontend (React)    │ ──────────▶ │  backend (Spring)    │
│  Vite dev :5173      │   proxy     │  API REST :8080      │
│  ou Nginx :80        │             └──────────┬───────────┘
└──────────────────────┘                        │
                                   ┌────────────┴────────────┐
                                   ▼                         ▼
                        PostgreSQL primário        PostgreSQL de posts
                        (usuários, tokens,         (`asgard`, somente
                         códigos — Flyway)          leitura)
```

O front-end faz todas as chamadas para `/api`. Em desenvolvimento, o proxy do Vite repassa essas requisições para `http://localhost:8080`, removendo o prefixo `/api`; em produção, o Nginx do container do front-end faz o mesmo papel. Isso evita problemas de CORS.

## Stack Tecnológica

**Back-end**

- **Java 21** e **Spring Boot 4.0.5** (`spring-boot-starter-webmvc`)
- **Spring Security** + **JJWT 0.11.5** + `BCryptPasswordEncoder` — autenticação e autorização
- **Spring Data JPA / Hibernate** — banco primário
- **NamedParameterJdbcTemplate** — acesso somente leitura ao banco de posts
- **PostgreSQL** (dois bancos) + **Flyway** (migrations do banco primário)
- **Spring Mail (SMTP)** — envio dos códigos OTP
- **Lombok**, **JUnit 5 / Mockito / AssertJ**, **H2** (testes) e **Maven** (wrapper `mvnw`)

**Front-end**

- **React 18** + **Vite**
- **React Router 7** — roteamento (`createBrowserRouter`)
- **Axios** — cliente HTTP com interceptors (refresh automático de token)
- **Chart.js** / **react-chartjs-2** — gráficos
- **D3.js** + **d3-cloud** — nuvem de palavras
- **Bootstrap 5**, **CSS Modules** e **React Icons**
- **ESLint**

## Estrutura do Repositório

```
threatlens/
├── backend/
│   └── threatlens/                # Projeto Spring Boot (pom.xml, mvnw, Dockerfile, docker-compose.yml)
│       └── src/
│           ├── main/java/com/backend/threatlens/
│           │   ├── bootstrap/     # AdminBootstrapRunner
│           │   ├── config/        # Segurança e datasources
│           │   ├── controller/    # AuthController, AdminController, PostsController
│           │   ├── dto/           # Contratos de entrada e saída da API
│           │   ├── entity/        # Entidades JPA
│           │   ├── enums/
│           │   ├── exception/     # Exceções + GlobalExceptionHandler
│           │   ├── filter/        # JwtAuthFilter, RateLimitFilter
│           │   ├── repository/
│           │   ├── security/
│           │   ├── service/
│           │   └── utils/
│           ├── main/resources/    # application*.properties e migrations Flyway
│           └── test/
└── frontend/
    ├── public/
    ├── src/
    │   ├── components/            # Componentes reutilizáveis
    │   ├── context/               # AuthContext
    │   ├── hooks/                 # usePosts, useStats, useUsers, useWordCloud, ...
    │   ├── layouts/
    │   ├── pages/                 # Home, Posts, Alerts, Admin, Login, Register, ...
    │   ├── services/              # Comunicação com a API
    │   └── router.jsx
    ├── Dockerfile                 # Build + Nginx
    ├── nginx.conf
    ├── vite.config.js             # Proxy de /api
    └── .env.example
```

## Pré-requisitos

- **JDK 21** ou superior (o Maven pode ser usado via wrapper `./mvnw` / `mvnw.cmd`)
- **Node.js 18** ou superior (recomendado LTS) e **npm**
- Dois bancos **PostgreSQL** acessíveis: um primário (`threatlens`) e um somente leitura com os posts já coletados (`asgard`)
- Uma conta **SMTP** para envio de e-mails (configurada para Gmail em desenvolvimento, mas qualquer servidor SMTP funciona)

## Como rodar o projeto localmente

### 1. Clone o repositório

```bash
git clone git@github.com:annaclararodrigues/threatlens.git
cd threatlens
```

### 2. Suba o back-end

Configure as variáveis de ambiente (veja [Variáveis de Ambiente](#variáveis-de-ambiente)):

```bash
export JWT_SECRET=uma-chave-bem-grande-e-secreta
export POSTS_DB_URL=jdbc:postgresql://localhost:5432/asgard
export POSTS_DB_USERNAME=asgard_reader
export POSTS_DB_PASSWORD=...
export MAIL_PASSWORD=...
```

O perfil `dev` espera o banco primário em `jdbc:postgresql://localhost:5432/threatlens`. O schema é criado automaticamente pelo Flyway na subida.

Se ainda não houver nenhum usuário `ADMIN`, defina também `ADMIN_EMAIL`, `ADMIN_USERNAME` e `ADMIN_PASSWORD` para que o primeiro administrador seja criado automaticamente.

```bash
cd backend/threatlens
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### 3. Suba o front-end

Em outro terminal, a partir da raiz do repositório:

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

A aplicação fica disponível em **http://localhost:5173**. Crie uma conta (ou faça login) e explore o dashboard.

> Se o back-end rodar em outra porta, ajuste o `target` do proxy em [frontend/vite.config.js](frontend/vite.config.js).

## Variáveis de Ambiente

**Back-end**

| Variável | Usada em | Descrição |
|---|---|---|
| `JWT_SECRET` | `jwt.secret` | Chave HMAC-SHA256 usada para assinar/validar os access e refresh tokens |
| `POSTS_DB_URL` / `POSTS_DB_USERNAME` / `POSTS_DB_PASSWORD` | `posts.datasource.*` | Conexão com o banco externo `asgard` (somente leitura) |
| `MAIL_PASSWORD` | `spring.mail.password` | Senha/App Password da conta SMTP usada para enviar os códigos OTP |
| `ADMIN_EMAIL` / `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin.bootstrap.*` | Credenciais do administrador inicial, criado no primeiro boot caso não exista nenhum `ADMIN` |

Outras propriedades relevantes (já configuradas em `application*.properties`):

- `jwt.expiration` — duração do access token em milissegundos (padrão: `300000`, 5 minutos)
- `cookie.secure` — atributo `Secure` dos cookies de sessão (`false` em dev, `true` em prod)
- `spring.datasource.*` — conexão com o banco primário (em produção, use as variáveis padrão do Spring, ex. `SPRING_DATASOURCE_URL`)

**Front-end**

| Variável | Descrição | Padrão (`.env.example`) |
|---|---|---|
| `VITE_API_BASE_URL` | Base URL usada pelo Axios para chamadas à API | `/api` |

## Scripts Disponíveis

**Back-end** (em `backend/threatlens/`)

| Comando | Descrição |
|---|---|
| `./mvnw spring-boot:run` | Sobe a aplicação em modo desenvolvimento |
| `./mvnw test` | Executa a suíte de testes (H2 em memória, sem infraestrutura externa) |
| `./mvnw clean package` | Gera `target/threatlens-0.0.1-SNAPSHOT.jar` |

**Front-end** (em `frontend/`)

| Comando | Descrição |
|---|---|
| `npm run dev` | Servidor de desenvolvimento do Vite com hot reload |
| `npm run build` | Build de produção na pasta `dist/` |
| `npm run preview` | Serve localmente a build de produção |
| `npm run lint` | Executa o ESLint em todo o projeto |

## Autenticação

A autenticação é baseada em JWT armazenado em cookies HTTP-only, gerenciados inteiramente pelo back-end:

- No cadastro (`POST /auth/register`), a senha é armazenada com hash BCrypt e um código OTP de 4 dígitos é enviado por e-mail.
- A confirmação do código (`POST /auth/verify`) é o momento em que os cookies de sessão (`accessToken` e `refreshToken`) são emitidos.
- O `accessToken` tem vida curta (5 minutos por padrão) e é validado a cada requisição pelo `JwtAuthFilter`, sem consultar o banco.
- O `refreshToken` vale 7 dias, fica persistido no banco para poder ser revogado e é rotacionado a cada uso de `POST /auth/refresh`.
- `POST /auth/logout` revoga o refresh token atual e limpa os cookies.
- No front-end, o Axios usa `withCredentials: true`, e um interceptor detecta respostas `401`/`403`, renova a sessão via `/auth/refresh` e reenfileira as requisições que falharam. Se o refresh falhar, o usuário é redirecionado para `/login`.
- As rotas do front-end são protegidas por `ProtectedRoute` (usuário autenticado) e `AdminRoute` (role `ADMIN`); no back-end, `/admin/**` exige `hasRole('ADMIN')`.

## Testes, Lint e Padrões de Código

Antes de abrir um PR, rode:

```bash
# back-end
cd backend/threatlens && ./mvnw test

# front-end
cd frontend && npm run lint
```

Os testes do back-end (JUnit 5 + Mockito + AssertJ) usam H2 em memória em modo de compatibilidade PostgreSQL, com o Flyway desabilitado, e não exigem credenciais reais. Testes de controller usam `MockMvc` em modo `standaloneSetup`.

## Build e Deploy

### Com Docker Compose (aplicação completa)

O [docker-compose.yml](backend/threatlens/docker-compose.yml) em `backend/threatlens/` sobe a aplicação inteira:

| Serviço | Imagem / build | Porta | Descrição |
|---|---|---|---|
| `db` | `postgres:16-alpine` | `5432` | Banco primário (`threatlens`), com volume persistente `threatlens_db_data` |
| `app` | [backend/threatlens/Dockerfile](backend/threatlens/Dockerfile) | `8080` | API Spring Boot (build com Maven + JRE 21), sobe após o `db` ficar saudável |
| `front` | [frontend/Dockerfile](frontend/Dockerfile) | `80` | Build do React servida por Nginx, que encaminha `/api/` para o serviço `app` |

O banco de posts (`asgard`) **não** sobe no compose: ele é externo e acessado via `POSTS_DB_URL`.

```bash
cd backend/threatlens
cp .env.example .env   # preencha JWT_SECRET, POSTS_DB_*, MAIL_PASSWORD e, opcionalmente, ADMIN_*
docker compose up -d --build
```

A aplicação fica disponível em **http://localhost** e a API em `http://localhost:8080`.

Variáveis específicas do compose (além das listadas em [Variáveis de Ambiente](#variáveis-de-ambiente)):

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do Postgres do container `db`, usadas também pela API | `postgres` / `postgres` |
| `FRONTEND_PATH` | Build context do serviço `front`, relativo a `backend/threatlens/` | `../../frontend` |

### Manualmente

**Back-end** — gere o `.jar` e execute com o perfil `prod`:

```bash
cd backend/threatlens
./mvnw clean package
SPRING_PROFILES_ACTIVE=prod java -jar target/threatlens-0.0.1-SNAPSHOT.jar
```

**Front-end** — `npm run build` gera os arquivos estáticos em `dist/`, que podem ser servidos por qualquer servidor estático, desde que `/api` seja encaminhado para a API (ou `VITE_API_BASE_URL` aponte para ela).

## Contribuindo

1. Crie uma branch a partir da `dev`: `git checkout -b feat/minha-feature`
2. Faça suas alterações e garanta que `./mvnw test` e `npm run lint` passam sem erros
3. Abra um Pull Request descrevendo a mudança

---

Projeto desenvolvido no contexto de pesquisa em Cybersecurity — UFU.
