# Documentação Técnica: Módulo de Autenticação - ThreatlensApplication

**Projeto:** ThreatlensApplication  
**Módulo:** Autenticação e Autorização  
**Versão:** 1.0  
**Data:** Maio de 2026  
**Escopo:** Documentação de TCC (Trabalho de Conclusão de Curso)

---

## Sumário

1. [Introdução](#introdução)
2. [Problema Resolvido](#problema-resolvido)
3. [Estratégia de Autenticação](#estratégia-de-autenticação)
4. [Arquitetura Geral](#arquitetura-geral)
5. [Fluxos de Autenticação](#fluxos-de-autenticação)
6. [Tratamento de Senhas](#tratamento-de-senhas)
7. [Geração e Validação de Tokens](#geração-e-validação-de-tokens)
8. [Proteção de Rotas](#proteção-de-rotas)
9. [Descrição dos Componentes](#descrição-dos-componentes)
10. [Bibliotecas Utilizadas](#bibliotecas-utilizadas)
11. [Decisões Arquiteturais](#decisões-arquiteturais)
12. [Segurança e Boas Práticas](#segurança-e-boas-práticas)
13. [Conclusão](#conclusão)

---

## Introdução

O módulo de autenticação da ThreatlensApplication é responsável por gerenciar o controle de identidade de usuários, proteção de endpoints sensíveis e manutenção segura de sessões. Este documento apresenta a implementação técnica em nível de TCC, detalhando todas as decisões arquiteturais, fluxos operacionais e mecanismos de segurança empregados.

A aplicação implementa um sistema moderno e seguro de autenticação baseado em **JSON Web Tokens (JWT)** combinado com **Refresh Tokens** persistidos em banco de dados, garantindo equilíbrio entre segurança, performance e escalabilidade.

---

## Problema Resolvido

### 1.1 Desafios de Autenticação em Aplicações Web Modernas

As aplicações web contemporâneas enfrentam diversos desafios relacionados à autenticação e autorização:

- **Identificação Confiável**: Necessidade de verificar a identidade do usuário de forma segura
- **Proteção de Endpoints**: Garantir que apenas usuários autenticados acessem recursos sensíveis
- **Gerenciamento de Sessão**: Manter rastreamento de usuários autenticados sem armazenar estado no servidor
- **Segurança de Credenciais**: Proteger senhas contra vazamento, phishing e ataques de força bruta
- **Recuperação de Conta**: Mecanismos para recuperação segura de acesso perdido
- **Tokens com Validade Limitada**: Reduzir risco de tokens comprometidos através de expiração
- **Escalabilidade**: Suportar múltiplas instâncias de servidor sem dependência de estado compartilhado

### 1.2 Solução Proposta

O sistema ThreatlensApplication resolve estes desafios através de:

1. **Autenticação por Email e Senha** com criptografia bcrypt
2. **Verificação de Email** obrigatória via código temporal (OTP de 4 dígitos)
3. **Tokens JWT** com expiração curta (1 minuto) para access token
4. **Refresh Tokens** persistidos e revogáveis com expiração longa (7 dias)
5. **Cookies HttpOnly** para armazenamento seguro de refresh tokens no cliente
6. **Filtro de Autenticação** que intercepta requisições e valida tokens
7. **Recuperação de Senha** via código temporal verificado por email

---

## Estratégia de Autenticação

### 2.1 Paradigma: Autenticação Stateless com JWT

A estratégia escolhida é **stateless** utilizando **JSON Web Tokens (JWT)**:

```
Paradigma: RESTful Stateless
├── Access Token (JWT curta duração)
├── Refresh Token (persistido em BD)
├── Cookies HttpOnly (transporte seguro)
└── Validação em cada requisição
```

#### Vantagens desta abordagem:

| Vantagem | Descrição |
|----------|-----------|
| **Escalabilidade Horizontal** | Múltiplos servidores sem sincronização de estado |
| **Segurança** | Tokens auto-contidos com assinatura criptográfica |
| **Performance** | Sem lookup de sessão a cada requisição |
| **Mobile-Friendly** | Headers HTTP nativamente suportados em apps móveis |
| **Revogação Controlada** | Refresh tokens podem ser revogados em banco de dados |

---

## Arquitetura Geral

### 3.1 Fluxo Geral

```
┌─────────────────────────────────────────────────────────────┐
│                Cliente (Browser/Mobile)                      │
├─────────────────────────────────────────────────────────────┤
│  Headers: Authorization: Bearer {accessToken}              │
│  Cookies: refreshToken={refreshToken}                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                    HTTP Requisição
                           │
┌──────────────────────────▼──────────────────────────────────┐
│               JwtAuthFilter (Spring Filter)                 │
├──────────────────────────────────────────────────────────────┤
│  1. Extrai token do header Authorization                   │
│  2. Valida assinatura e expiração                          │
│  3. Carrega UserDetails via UserDetailsService            │
│  4. Popula SecurityContext com autenticação               │
└──────────────────────────┬───────────────────────────────────┘
```

---

## Fluxos de Autenticação

### 4.1 Fluxo de Registro

1. Usuario envia email, username e senha
2. Valida duplicação de email
3. Criptografa senha com BCrypt
4. Persiste UserEntity com `isEmailVerified = false`
5. Gera código temporal de 4 dígitos (10 min validade)
6. Envia código por email
7. Retorna mensagem de sucesso

### 4.2 Fluxo de Verificação de Email

1. Usuario recebe código por email
2. Envia email, código e tipo (REGISTER)
3. Sistema busca VerificationCodeEntity
4. Valida expiração
5. Marca código como usado
6. Atualiza `user.isEmailVerified = true`
7. Gera Access Token (1 minuto)
8. Cria Refresh Token (7 dias) em BD
9. Retorna tokens + cookie com refresh token

### 4.3 Fluxo de Login

1. Usuario envia email e senha
2. AuthenticationManager autentica credenciais
3. Compara senha com passwordEncoder.matches()
4. Valida se email foi verificado
5. Gera Access Token (1 minuto)
6. Cria Refresh Token (7 dias) em BD
7. Retorna tokens + cookie

### 4.4 Fluxo de Requisição Autenticada

1. Cliente envia requisição com Authorization: Bearer {token}
2. JwtAuthFilter intercepta
3. Extrai token do header
4. JwtUtil valida assinatura e expiração
5. Extrai email do subject
6. UserDetailsServiceImpl carrega UserEntity
7. Cria autenticação e popula SecurityContext
8. Continua para handler

### 4.5 Fluxo de Refresh Token

1. Access Token expirado (1 minuto)
2. Cliente envia POST /auth/refresh com cookie
3. RefreshTokenService valida token (JWT + BD)
4. Revoga refresh token antigo (`isRevoked = true`)
5. Gera novo Access Token (1 minuto)
6. Cria novo Refresh Token (7 dias)
7. Retorna novos tokens

### 4.6 Fluxo de Logout

1. Usuario clica logout
2. Sistema revoga refresh token em BD (`isRevoked = true`)
3. Limpa cookie no cliente (Max-Age=0)
4. Access token antigo expira em 1 minuto naturalmente

---

## Tratamento de Senhas

### 5.1 Política de Armazenamento

Senhas nunca são armazenadas em texto plano. O sistema utiliza **BCrypt**:

```
Senha texto plano
     ↓
bcrypt.encode(password)
├─ Gera salt aleatório
├─ Aplica função Blowfish
├─ Iterações adaptativas
└─ Resultado: $2a$10$... (60 caracteres)
     ↓
Persiste em BD (nunca retorna para cliente)
```

### 5.2 Validação Durante Login

```java
passwordEncoder.matches(inputPassword, user.getPassword())
├─ Extrai salt do hash armazenado
├─ Aplica mesma função com novo input
└─ Compara resultado → true/false
```

### 5.3 Propriedades do BCrypt

| Propriedade | Valor | Impacto |
|-------------|-------|--------|
| Algoritmo | Blowfish | Projetado para senhas |
| Tamanho do Salt | 16 bytes | Impede rainbow tables |
| Iterações | 2^10 (padrão) | Aumenta tempo de ataque |
| Saída | 60 caracteres | Hash completo + metadados |

---

## Geração e Validação de Tokens

### 6.1 Estrutura JWT

```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQGV4YW1wbGUuY29tIiwiaWF0IjoxNzE2MDAwMDAwLCJleHAiOjE3MTYwMDA2MDB9.signature
```

**Header** (Base64Url):
```json
{"alg": "HS256", "typ": "JWT"}
```

**Payload** (Base64Url):
```json
{"sub": "usuario@example.com", "iat": 1716000000, "exp": 1716000600}
```

**Signature** (HMAC-SHA256):
```
HMACSHA256(base64(header) + "." + base64(payload), secret)
```

### 6.2 Access Token (1 minuto)

- **Duração**: 1 minuto
- **Propósito**: Autorizar requisições HTTP
- **Local**: Header Authorization
- **Revogação**: Impossível (depende de expiração)

### 6.3 Refresh Token (7 dias)

- **Duração**: 7 dias
- **Propósito**: Obter novos access tokens
- **Local**: Cookie HttpOnly
- **Revogação**: Possível (persistido em BD)
- **Armazenamento**: RefreshTokenEntity com campo `isRevoked`

### 6.4 Validações

```
Access Token (JwtAuthFilter):
├─ Assinatura HMAC-SHA256
├─ Expiração (exp > agora)
├─ Formato (3 partes com pontos)
└─ Encoding (Base64Url válido)

Refresh Token (RefreshTokenService):
├─ Todas as validações de Access Token
├─ Existe em RefreshTokenRepository
└─ isRevoked == false
```

---

## Proteção de Rotas

### 7.1 Rotas Públicas (Sem Autenticação)

```java
"/auth/register"
"/auth/verify"
"/auth/resend-code"
"/auth/login"
"/auth/forgot-password"
"/auth/refresh"
"/auth/logout"
```

### 7.2 Rotas Protegidas (Com Autenticação)

```
Todas as outras rotas requerem:
Authorization: Bearer {validAccessToken}
```

### 7.3 Configuração Spring Security

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(SecurityRoutes.PUBLIC).permitAll()
    .anyRequest().authenticated()
)
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

---

## Descrição dos Componentes

### 8.1 AuthController (`controller/AuthController.java`)

**Responsabilidade**: Receber requisições HTTP e delegar ao AuthService

**Endpoints**:
- POST `/auth/register` → MessageResponseDTO
- POST `/auth/verify` → AuthResponseDTO
- POST `/auth/login` → AuthResponseDTO
- POST `/auth/refresh` → AuthResponseDTO
- POST `/auth/logout` → MessageResponseDTO
- POST `/auth/forgot-password` → MessageResponseDTO
- POST `/auth/reset-password` → MessageResponseDTO (requer autenticação)
- POST `/auth/change-password` → MessageResponseDTO (requer autenticação)

### 8.2 AuthService (`service/AuthService.java`)

**Responsabilidade**: Orquestrar lógica de negócio de autenticação

**Métodos principais**:
- `register()` - Cadastra novo usuário
- `verifyCode()` - Verifica código de email
- `login()` - Autentica credenciais
- `refresh()` - Renova access token
- `logout()` - Revoga refresh token
- `resetPassword()` - Altera senha com verificação
- `changePassword()` - Altera senha sendo autenticado
- `forgotPassword()` - Inicia recuperação
- `resendCode()` - Reenvio de código

### 8.3 JwtAuthFilter (`filter/JwtAuthFilter.java`)

**Responsabilidade**: Filtro HTTP que valida JWT

**Fluxo**:
1. Extrai header Authorization
2. Verifica formato "Bearer {token}"
3. Valida token (assinatura + expiração)
4. Extrai email do subject
5. Carrega UserDetails
6. Popula SecurityContext
7. Continua pipeline

### 8.4 RefreshTokenService (`service/RefreshTokenService.java`)

**Responsabilidade**: Gerenciar refresh tokens

**Métodos**:
- `createRefreshToken()` - Cria novo token em BD
- `validate()` - Valida token (JWT + BD)
- `revoke()` - Marca como revogado

### 8.5 UserDetailsServiceImpl (`service/UserDetailsServiceImpl.java`)

**Responsabilidade**: Carregar UserDetails por email

**Implementa**: `UserDetailsService` do Spring Security

### 8.6 SecurityConfig (`config/SecurityConfig.java`)

**Responsabilidade**: Configurar Spring Security

**Configurações**:
- CSRF desabilitado
- SessionPolicy = STATELESS
- Rotas públicas vs protegidas
- JwtAuthFilter adicionado antes de UsernamePasswordAuthenticationFilter
- PasswordEncoder = BCryptPasswordEncoder
- AuthenticationManager bean

### 8.7 Entidades

#### UserEntity (`entity/UserEntity.java`)

Tabela: `users`

```
id: UUID (PK)
username: String
email: String (UNIQUE)
password: String (hash bcrypt 60 chars)
isEmailVerified: boolean
```

#### RefreshTokenEntity (`entity/RefreshTokenEntity.java`)

Tabela: `refresh_token`

```
id: UUID (PK)
token: String (UNIQUE, JWT)
email: String
expiresAt: LocalDateTime
isRevoked: boolean
```

#### VerificationCodeEntity (`entity/VerificationCodeEntity.java`)

Tabela: `verification_code`

```
id: UUID (PK)
email: String
codeType: CodeType (REGISTER, RESET_PASSWORD)
code: String (4 dígitos)
expiresAt: LocalDateTime
isUsed: boolean
```

### 8.8 DTOs

**RegisterRequestDTO**: email, username, password  
**LoginRequestDTO**: email, password  
**VerifyCodeRequestDTO**: email, code, codeType  
**AuthTokens**: accessToken, refreshToken, username, email  
**AuthResponseDTO**: accessToken, username, email  
**MessageResponseDTO**: message  

### 8.9 JwtUtil (`utils/JwtUtil.java`)

**Responsabilidade**: Geração e validação de JWT

**Métodos**:
- `generateAccessToken()` - Cria JWT 1 minuto
- `generateRefreshToken()` - Cria JWT 7 dias
- `extractEmail()` - Extrai subject
- `isValidToken()` - Valida assinatura + expiração

**Secret**: `jwt.secret` (application.properties)

### 8.10 CookieUtil (`utils/CookieUtil.java`)

**Responsabilidade**: Gerenciar cookies

**Propriedades**:
- `HttpOnly: true` - JS não acessa
- `Secure: false` (dev) / true (prod) - HTTPS
- `SameSite: Strict` - CSRF protection
- `Path: /auth/refresh` - Apenas para refresh
- `MaxAge: 7 dias` - Duração

---

## Bibliotecas Utilizadas

### 10.1 Spring Boot 4.0.5

**spring-boot-starter-security**
- Autenticação e autorização
- Filtros HTTP
- Gerenciador de autenticação

**spring-boot-starter-data-jpa**
- ORM com Hibernate
- Repositórios automáticos
- Transações

**spring-boot-starter-webmvc**
- Spring MVC para REST
- Roteamento
- Serialização JSON

**spring-boot-starter-validation**
- Validação de DTOs (@Valid)

**spring-boot-starter-mail**
- Envio de emails SMTP

### 10.2 JJWT 0.11.5 (JWT Library)

**Por quê?**
- ✓ Padrão de ouro em Java
- ✓ HS256 (HMAC-SHA256)
- ✓ Validação automática
- ✓ Tratamento robusto de erros
- ✓ Integração Jackson
- ✓ Bem mantido

### 10.3 PostgreSQL

- Persistência de usuários
- Refresh tokens
- Códigos de verificação

### 10.4 Lombok

**Anotações**:
- `@RequiredArgsConstructor` - Construtor com dependências
- `@Data` - Getters/setters
- `@Builder` - Builder pattern
- `@Getter` - Apenas getters

### 10.5 Jakarta Persistence (JPA)

- `@Entity` - Marca classe como entidade
- `@Table` - Nome da tabela
- `@Id` - Primary key
- `@Column` - Configuração de coluna
- `@GeneratedValue` - UUID automático

### 10.6 Java 21 (LTS)

- ✓ Suporte a records (DTOs)
- ✓ LTS até 2029
- ✓ Performance melhorada
- ✓ Segurança atualizada

---

## Decisões Arquiteturais

### 11.1 JWT + Refresh Tokens em BD (não JWT puro)

**Problema do JWT puro**:
```
├─ Impossível revogar token (stateless)
├─ Logout não é confiável
└─ Token roubado é válido até expiração
```

**Solução: Refresh Tokens em BD**:
```
├─ Revogação imediata via BD
├─ Logout é confiável
├─ Access token curto reduz risco
└─ Refresh token pode ser invalidado
```

### 11.2 Access Token Curto (1 minuto)

**Benefícios**:
- Se comprometido, válido por apenas 1 minuto
- Reduz janela de vulnerabilidade
- Força refresh frequente

**Trade-off**:
- Mais requisições ao servidor (refresh)
- Mas com validação rápida em BD

### 11.3 Email como Username

- Email é único por usuário
- Email é usado para recuperação
- Padrão moderno (Gmail, GitHub)
- No Spring Security: `loadUserByUsername(email)`

### 11.4 Código OTP 4 Dígitos (10 minutos)

- **Espaço**: 10.000 possibilidades
- **Expiração**: 10 minutos
- **Segurança**: Adequado com email (usuário controla tentativas)

### 11.5 Stateless (SessionPolicy.STATELESS)

```
├─ Sem JSESSIONID
├─ Sem HttpSession
├─ API RESTful pura
└─ Escalável horizontalmente
```

### 11.6 CSRF Desabilitado

**Razão**: API REST com JWT não sofre CSRF
```
├─ Token não é enviado automaticamente
├─ Cliente controla header Authorization
├─ SOP (Same-Origin Policy) protege
└─ Requer header explícito
```

### 11.7 Cookies HttpOnly + SameSite Strict

**HttpOnly = true**:
```
├─ JavaScript não acessa cookie
├─ Reduz risco de XSS
└─ Browser envia automaticamente
```

**SameSite = Strict**:
```
├─ Cookie não enviado em cross-site
├─ Mitiga CSRF
└─ Padrão de segurança
```

### 11.8 Access Token em Header (não em cookie)

**Benefícios**:
```
├─ Explícito: cliente controla
├─ Seguro: não afetado por CSRF
├─ Flexível: header customizável
└─ Mobile-friendly: APIs nativas
```

### 11.9 Refresh Token Única Vez

```
Quando usado:
├─ Token antigo revogado
├─ Novo token gerado
└─ Se token antigo usado novamente = token roubado

Benefício:
└─ Detecta comprometimento
```

### 11.10 @Transactional para Operações Críticas

```
Exemplo: verifyCode()
├─ Marca código como usado
├─ Atualiza user.isEmailVerified
└─ Se erro: rollback total (atomicidade)
```

---

## Segurança e Boas Práticas

### 12.1 Senhas BCrypt

✓ Criptografia irreversível  
✓ Salt único por senha  
✓ Iterações adaptativas  
✓ Resiste a rainbow tables e força bruta  

### 12.2 Validação de Input

```java
@Valid @RequestBody RegisterRequestDTO dto
├─ Email válido
├─ Comprimento de senha
├─ Campos obrigatórios
└─ Tipos de dados corretos
```

### 12.3 Expiração de Tokens

```
Access: 1 minuto
├─ Rápida revogação se comprometido
└─ Sem lookup a cada requisição

Refresh: 7 dias
├─ Persistido em BD
├─ Pode ser revogado
└─ Permite sessão longa
```

### 12.4 Revogação via BD

```
Logout imediato:
├─ isRevoked = true
├─ Requisições futuras com token falham
└─ Access token antigo expira em 1 min
```

### 12.5 Princípio da Menor Informação

```java
// ✓ Correto
"Se este e-mail estiver cadastrado, você receberá um código"

// ✗ Inseguro (revela qual email existe)
"Email não cadastrado"
```

### 12.6 HTTPS em Produção

```properties
# Dev:
secure=false

# Prod:
secure=true  # Requer HTTPS
```

**Sem HTTPS**:
```
├─ Cookie transmitido em plaintext
├─ Atacante intercepta (MITM)
└─ Token roubado facilmente
```

### 12.7 Secret Key Seguro

```properties
# ✗ Inseguro (fixo no código)
jwt.secret=b7T#9vP$2qL@8mKx5yN!wR&7cF*3h^J4

# ✓ Melhor (arquivo não versionado)
# .env ou application-prod.properties

# ✓ Melhor ainda (vault)
# AWS Secrets Manager, HashiCorp Vault
```

### 12.8 Logging Seguro

```java
// ✓ Correto
logger.info("Login attempt for: {}", email);

// ✗ Inseguro (expõe dados sensíveis)
logger.info("Login with password: {}", password);
logger.info("Token: {}", jwtToken);
```

### 12.9 Validação de Email Verificado

```java
if (!user.isEmailVerified()) {
    throw new RuntimeException("E-mail não verificado.");
}
```

**Razão**: Força verificação antes de login

### 12.10 Rate Limiting (Recomendado Futuro)

```
Implementar para:
├─ POST /auth/login (5 tentativas / 15 min)
├─ POST /auth/verify (10 tentativas / 10 min)
└─ Retorna 429 Too Many Requests
```

---

## Conclusão

### 13.1 Resumo

O módulo de autenticação implementa um sistema robusto, seguro e escalável baseado em JWT com refresh tokens persistidos. Oferece:

✓ Autenticação confiável  
✓ Proteção de dados (BCrypt)  
✓ Revogação de tokens (via BD)  
✓ Escalabilidade horizontal  
✓ API RESTful pura  
✓ Suporte a web e mobile  

### 13.2 Componentes Principais

```
AuthController → AuthService → Repositories → PostgreSQL
                    ↓
                RefreshTokenService
                UserDetailsServiceImpl
                    ↓
JwtAuthFilter (validação JWT)
                    ↓
SecurityConfig (policy HTTP)
```

### 13.3 Fluxos Suportados

| Operação | Tempo | Resultado |
|----------|-------|-----------|
| Registro | 2 passos | Usuário verificado |
| Login | 1 passo | Tokens gerados |
| Refresh | 1 passo | Novo access token |
| Logout | 1 passo | Token revogado |
| Reset Senha | 2 passos | Senha alterada |

### 13.4 Melhorias Futuras

1. Rate limiting para brute force
2. 2FA avançado (TOTP, backup codes)
3. OAuth2 (Google, GitHub)
4. Auditoria de login
5. Roles e permissions
6. Gerenciamento de sessões (múltiplos dispositivos)
7. Biometria em mobile

---

**Fim da Documentação - TCC**

*Documento completo sobre autenticação - ThreatlensApplication*

