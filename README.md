# ThreatLens

ThreatLens é o back-end de uma plataforma de Cyber Threat Intelligence (CTI), desenvolvida como parte de uma pesquisa acadêmica na UFU. A aplicação centraliza a coleta e o fornecimento de postagens relevantes de fontes como Telegram, permitindo que um front-end de analistas de segurança monitore menções, avalie o nível de relevância/risco de cada postagem e gerencie usuários e contas administrativas.

Este repositório contém apenas o back-end (Java + Spring Boot). Ele expõe uma API REST consumida por um front-end separado (React), responsável por autenticação, coleta de posts, estatísticas e gerenciamento de usuários.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar o projeto localmente](#como-rodar-o-projeto-localmente)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Scripts Disponíveis](#scripts-disponíveis)
- [Autenticação](#autenticação)
- [Testes e Padrões de Código](#testes-e-padrões-de-código)
- [Build e Deploy](#build-e-deploy)
- [Contribuindo](#contribuindo)

## Funcionalidades

- **Autenticação completa** — cadastro com verificação de código por e-mail (OTP), login com JWT + refresh token rotativo (cookies HttpOnly), "esqueci minha senha" com reset via código, e troca de senha autenticada.
- **Posts** — listagem paginada de postagens coletadas de fontes externas (hoje, Telegram), com filtros por fonte, nível de relevância, categoria e período (dia, semana, mês, ano, tudo ou intervalo customizado), e ordenação por data, score, id, fonte ou conteúdo.
- **Estatísticas** — endpoint agregado com total de posts, percentual de posts relevantes no período e distribuição por nível de relevância, usado para alimentar dashboards.
- **Nuvem de palavras** — termos mais frequentes nos posts, calculados via SQL (sem sobrecarregar a aplicação), com stopwords em português e inglês.
- **Painel de Administração** — restrito a usuários com role `ADMIN`: listagem paginada de usuários com busca textual e filtro por role, alteração de role de um usuário e exclusão de contas (ambas bloqueadas quando o alvo é o último administrador restante).
- **Bootstrap automático do primeiro administrador** — se nenhum usuário `ADMIN` existir, a aplicação cria um a partir de variáveis de ambiente na subida, sem precisar de um endpoint público de criação de admin.
- **Rate limiting** — limite de tentativas por IP nos endpoints sensíveis de autenticação (login, cadastro, verificação de código).
- **Auditoria** — eventos de autenticação (login, logout, registro, troca de senha, renovação de sessão) registrados em um logger dedicado, com e-mail, IP e timestamp.

## Stack Tecnológica

- **Java 21** — linguagem/runtime
- **Spring Boot 4.0.5** (`spring-boot-starter-webmvc`) — framework
- **Spring Security** + **JJWT 0.11.5** + `BCryptPasswordEncoder` — autenticação e autorização
- **Spring Data JPA / Hibernate** — persistência do banco primário (usuários, tokens, códigos)
- **NamedParameterJdbcTemplate** — acesso somente leitura ao banco externo de posts (JDBC puro, sem JPA)
- **PostgreSQL** — dois bancos distintos (primário e externo)
- **Flyway** — migrations do banco primário
- **Spring Mail (SMTP)** — envio de códigos OTP por e-mail
- **Lombok** — redução de boilerplate (`@RequiredArgsConstructor`, `@Data`, `@Builder`)
- **JUnit 5 / Mockito / AssertJ** — testes
- **H2** — banco em memória usado nos testes (modo de compatibilidade PostgreSQL)
- **Maven** — build (via wrapper `mvnw` / `mvnw.cmd`)

## Estrutura do Projeto

```
threatlens/
├── src/
│   ├── main/
│   │   ├── java/com/backend/threatlens/
│   │   │   ├── bootstrap/      # AdminBootstrapRunner — cria o admin inicial no boot
│   │   │   ├── config/         # SecurityConfig, SecurityRoutes, datasources primário e de posts
│   │   │   ├── controller/     # AuthController, AdminController, PostsController
│   │   │   ├── dto/            # request/ e response/ — contratos de entrada e saída da API
│   │   │   ├── entity/         # Entidades JPA do banco primário (User, RefreshToken, VerificationCode)
│   │   │   ├── enums/          # Role, CodeType, PostSource, RelevanceLevel, SortBy, SortOrder, StatsPeriod
│   │   │   ├── exception/      # Exceções customizadas + GlobalExceptionHandler
│   │   │   ├── filter/         # JwtAuthFilter, RateLimitFilter
│   │   │   ├── repository/     # Repositórios JPA + repository/posts (fontes externas de posts)
│   │   │   ├── security/       # UserPrincipal
│   │   │   ├── service/        # AuthService, AdminService, PostsService, EmailService, ...
│   │   │   └── utils/          # JwtUtil, CookieUtil, StopWords
│   │   └── resources/
│   │       ├── db/migration/   # Migrations Flyway (V1, V2, ...)
│   │       └── application*.properties
│   └── test/                   # Testes unitários e de controller (JUnit 5 + Mockito + MockMvc)
├── pom.xml
└── mvnw / mvnw.cmd
```

## Pré-requisitos

- JDK 21 ou superior
- Maven (ou use o wrapper `./mvnw` / `mvnw.cmd` incluído — não requer instalação global)
- Duas instâncias/bancos PostgreSQL acessíveis: um para a aplicação (usuários/tokens) e outro somente leitura com os posts já coletados (`asgard`)
- Uma conta SMTP para envio de e-mails (o projeto foi configurado para Gmail em desenvolvimento, mas qualquer servidor SMTP funciona)

## Como rodar o projeto localmente

Siga o passo a passo abaixo para colocar o back-end no ar na sua máquina:

### 1. Clone o repositório

```bash
git clone git@github.com:annaclararodrigues/threatlens-backend.git
cd threatlens-backend/threatlens
```

### 2. Configure as variáveis de ambiente

```bash
export JWT_SECRET=uma-chave-bem-grande-e-secreta
export POSTS_DB_URL=jdbc:postgresql://localhost:5432/asgard
export POSTS_DB_USERNAME=asgard_reader
export POSTS_DB_PASSWORD=...
export MAIL_PASSWORD=...
```

Veja a seção [Variáveis de Ambiente](#variáveis-de-ambiente) para a lista completa.

### 3. Garanta que os bancos PostgreSQL estejam disponíveis

Por padrão, o perfil `dev` (`application-dev.properties`) espera o banco primário em `jdbc:postgresql://localhost:5432/threatlens`. O schema desse banco é criado automaticamente pelo Flyway na subida da aplicação — não é necessário rodar migrations manualmente.

### 4. Inicie a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe por padrão em `http://localhost:8080`.

### 5. (Opcional) Configure o admin inicial

Se ainda não existir nenhum usuário `ADMIN` no banco, defina também `ADMIN_EMAIL`, `ADMIN_USERNAME` e `ADMIN_PASSWORD` antes de subir a aplicação — o `AdminBootstrapRunner` cria o primeiro administrador automaticamente.

### 6. Acesse a API

Use um cliente HTTP (Postman, Insomnia, curl) ou o front-end do ThreatLens (repositório separado) apontando para `http://localhost:8080`. Crie uma conta via `POST /auth/register` e explore os demais endpoints.

## Variáveis de Ambiente

| Variável | Usada em | Descrição |
|---|---|---|
| `JWT_SECRET` | `jwt.secret` | Chave HMAC-SHA256 usada para assinar/validar os access e refresh tokens |
| `POSTS_DB_URL` / `POSTS_DB_USERNAME` / `POSTS_DB_PASSWORD` | `posts.datasource.*` | Conexão com o banco externo `asgard` (somente leitura) |
| `MAIL_PASSWORD` | `spring.mail.password` | Senha/App Password da conta SMTP usada para enviar os códigos OTP |
| `ADMIN_EMAIL` / `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin.bootstrap.*` | Credenciais do administrador inicial, criado automaticamente no primeiro boot caso ainda não exista nenhum usuário `ADMIN` |

Outras propriedades relevantes (já configuradas em `application.properties` / `application-dev.properties` / `application-prod.properties`, sem exigir variável de ambiente em dev):

- `jwt.expiration` — duração do access token em milissegundos (padrão: `300000`, 5 minutos)
- `cookie.secure` — atributo `Secure` dos cookies de sessão (`false` em dev, `true` em prod)
- `spring.datasource.*` — conexão com o banco primário (valores de dev já preenchidos; em produção, defina via variáveis de ambiente padrão do Spring, ex. `SPRING_DATASOURCE_URL`)

## Scripts Disponíveis

| Comando | Descrição |
|---|---|
| `./mvnw spring-boot:run` | Sobe a aplicação em modo desenvolvimento |
| `./mvnw test` | Executa a suíte de testes (H2 em memória, sem depender de infraestrutura externa) |
| `./mvnw clean package` | Gera o artefato executável em `target/threatlens-0.0.1-SNAPSHOT.jar` |
| `java -jar target/threatlens-0.0.1-SNAPSHOT.jar` | Executa o `.jar` já empacotado |

## Autenticação

A autenticação é baseada em JWT + cookies HTTP-only, gerenciados inteiramente pelo back-end:

- No cadastro (`POST /auth/register`), a senha é armazenada com hash BCrypt e um código OTP de 4 dígitos é enviado por e-mail.
- A confirmação do código (`POST /auth/verify`) é o momento em que os cookies de sessão (`accessToken` e `refreshToken`) são de fato emitidos — não no cadastro nem no login isoladamente.
- O `accessToken` tem vida curta (5 minutos por padrão) e é validado a cada requisição por um filtro (`JwtAuthFilter`) que lê as claims do próprio token, sem consultar o banco a cada request.
- O `refreshToken` tem vida longa (7 dias), fica persistido no banco para poder ser revogado, e é rotacionado a cada uso de `POST /auth/refresh` (o token antigo é revogado e um novo é emitido).
- `POST /auth/logout` revoga o refresh token atual e limpa os cookies — sempre retorna 200, mesmo sem uma sessão válida.
- Endpoints sensíveis (`/auth/login`, `/auth/register`, `/auth/verify`) têm limite de tentativas por IP (`RateLimitFilter`).
- O acesso a `/admin/**` é controlado por `@PreAuthorize("hasRole('ADMIN')")`.

## Testes e Padrões de Código

```bash
./mvnw test
```

Os testes (JUnit 5 + Mockito + AssertJ) usam H2 em memória, em modo de compatibilidade PostgreSQL, com o Flyway desabilitado — o schema de teste é criado via `spring.jpa.hibernate.ddl-auto=create-drop`. Nenhuma credencial real é necessária para rodar a suíte. Testes de controller usam `MockMvc` em modo `standaloneSetup`, isolados do contexto completo do Spring.

Antes de abrir um PR, rode:

```bash
./mvnw test
```

## Build e Deploy

Para gerar o artefato de produção:

```bash
./mvnw clean package
```

O `.jar` executável é gerado em `target/threatlens-0.0.1-SNAPSHOT.jar` (Lombok é excluído do artefato final via `spring-boot-maven-plugin`). Para rodar em produção, ative o perfil `prod` e configure as variáveis de ambiente necessárias:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/threatlens-0.0.1-SNAPSHOT.jar
```

## Contribuindo

1. Crie uma branch a partir da `dev`: `git checkout -b feat/minha-feature`
2. Faça suas alterações e garanta que `./mvnw test` passa sem erros
3. Abra um Pull Request descrevendo a mudança

Projeto desenvolvido no contexto de pesquisa em Cybersecurity — UFU.
