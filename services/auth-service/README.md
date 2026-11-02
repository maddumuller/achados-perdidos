# Auth Service (`auth-service`) - Sistema de Achados e Perdidos (UDESC)

Microsserviço de autenticação, autorização e gerenciamento de usuários do Sistema de Achados e Perdidos.

---

## 🌐 Ambientes em Nuvem (Render - DEV e HOMOL)

| Ambiente | Branch | Swagger UI | URL Base (Render) | Health Check |
| :--- | :--- | :--- | :--- | :--- |
| **DEV** (Desenvolvimento) | `develop` | **Habilitado** (`/swagger-ui/index.html`) | `https://auth-service-dev-4ru3.onrender.com` | `/actuator/health` |
| **HOMOL** (Homologação) | `main` | **Desabilitado** (`404 Not Found`) | `https://auth-service-homol-1hvy.onrender.com` | `/actuator/health` |

---

## 🛠️ Tecnologias e Decisões

- **Java 21** e **Spring Boot 3.4.3**
- **Persistência**: Spring Data JPA + H2 embarcado em modo arquivo (`/data/authdb` no container / `./data/authdb` local)
- **Segurança**: Criptografia de senhas com **BCrypt** (`spring-security-crypto`) e emissão de tokens **JWT** (HS256 com `jjwt 0.12.6`)
- **Mensageria**: RabbitMQ (`achados.events` topic exchange) com publicação do evento `usuario.desativado` após o commit da transação
- **Proteção contra Força Bruta**: Bloqueio de conta por 15 minutos após 5 erros consecutivos de senha
- **Observabilidade**: Spring Boot Actuator + Micrometer Prometheus (`/actuator/prometheus`) integrado ao **Prometheus** e **Grafana**
- **Qualidade e Cobertura**: JaCoCo (mínimo de 50% de cobertura de linhas) + análise estática no **SonarCloud**
- **Versionamento**: Semantic Versioning (`1.0.0`)

---

## 📌 Regras de Negócio Implementadas

- **RN-A1: Cadastro**: Validação de nome (2-100), e-mail válido e único (armazenado em minúsculas), senha com no mínimo 8 caracteres (pelo menos 1 letra e 1 número). Papel padrão é sempre `USER`. Retorna `409 Conflict` se o e-mail já existir.
- **RN-A2: Login e Bloqueio**: Emissão de JWT (validade configurável, padrão 120 min). 5 falhas consecutivas bloqueiam a conta por 15 minutos (`423 Locked`). Credenciais incorretas retornam mensagem opaca `401 Unauthorized` sem indicar se o e-mail existe. Login correto zera o contador.
- **RN-A3: Usuário Inativo**: Contas desativadas não conseguem efetuar login (`403 Forbidden`).
- **RN-A4: Administração**: Exclusivo para usuários com papel `ADMIN`. Permite listagem paginada, alteração de papéis e ativação/desativação. Um administrador não pode desativar a si mesmo nem remover o próprio papel de ADMIN (`409 Conflict`). Desativação publica `usuario.desativado`.
- **RN-A5: Troca de Senha**: Exige senha atual correta, nova senha válida e diferente da atual (`204 No Content`).

---

## ⚙️ Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` / `SERVER_PORT` | `8081` | Porta HTTP do serviço (compatível com injeção de `PORT` do Render) |
| `SPRING_PROFILES_ACTIVE` | *(vazio)* | Perfil do ambiente (`dev` habilita o Swagger; `homol` desabilita) |
| `JWT_SECRET` | `troque-este-segredo-com-no-minimo-32-caracteres` | Chave secreta para assinatura dos tokens JWT |
| `JWT_EXPIRATION_MINUTES` | `120` | Tempo de expiração do JWT em minutos |
| `ADMIN_EMAIL` | `admin@achados.local` | E-mail do administrador padrão inicial |
| `ADMIN_PASSWORD` | `Admin@1234` | Senha do administrador padrão inicial |
| `DB_PATH` | `./data/authdb` | Caminho do arquivo de banco H2 |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |
| `RABBIT_HEALTH_ENABLED` | `false` | Habilita/desabilita verificação do RabbitMQ no `/actuator/health` |

---

## 🚀 Endpoints

### Públicos
- `POST /api/auth/register` - Cadastro de usuário comum (`201 Created`)
- `POST /api/auth/login` - Autenticação e emissão do JWT (`200 OK`)

### Autenticados (`Authorization: Bearer <token>`)
- `GET /api/auth/me` - Dados do usuário logado (`200 OK`)
- `PUT /api/auth/me/password` - Alteração da própria senha (`204 No Content`)

### Administrativos (`ADMIN` logado)
- `GET /api/auth/users?page=0&size=20` - Lista paginada de usuários
- `PATCH /api/auth/users/{id}/role` - Altera papel (`USER` / `ADMIN`)
- `PATCH /api/auth/users/{id}/status` - Ativa ou desativa usuário

### Observabilidade e Documentação (Actuator & Swagger)
- `GET /actuator/health` - Status de saúde da aplicação
- `GET /actuator/prometheus` - Métricas no formato OpenMetrics/Prometheus
- **Ambiente DEV (`SPRING_PROFILES_ACTIVE=dev`)**: Swagger habilitado em `/swagger-ui/index.html` e especificação em `/v3/api-docs`
- **Ambiente HOMOL (`SPRING_PROFILES_ACTIVE=homol`)**: Swagger e `api-docs` desabilitados

---

## 🔄 Pipeline CI/CD Dedicado e Reutilizável

O serviço possui um pipeline exclusivo em [`.github/workflows/auth-service.yml`](../../.github/workflows/auth-service.yml), que também é invocado de forma modular (`workflow_call`) pelo pipeline geral do grupo ([`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)):

1. **CI (`build-test-sonar`)**:
   - Compilação com JDK 21 (Temurin), execução dos testes automatizados e verificação de cobertura mínima com JaCoCo (`./mvnw clean verify`).
   - Análise de qualidade e cobertura de código com **SonarCloud** (`./mvnw sonar:sonar`).
2. **Containerização e Versionamento (`publish-image`)**:
   - Extrai automaticamente a versão semântica (`1.0.0`) do [`pom.xml`](pom.xml).
   - Constrói a imagem Docker a partir do [`Dockerfile`](Dockerfile) e publica no **GitHub Container Registry (`ghcr.io`)** com as tags `:1.0.0` / `:1.0.0-dev`, `:homol` / `:dev`, `:latest` e `:sha-<commit>`.
3. **CD (`deploy-dev` e `deploy-homol`)**:
   - Push na branch `develop` → Aciona automaticamente o Deploy Hook do ambiente **DEV** no Render (`RENDER_DEPLOY_HOOK_DEV`).
   - Push na branch `main` → Aciona automaticamente o Deploy Hook do ambiente **HOMOL** no Render (`RENDER_DEPLOY_HOOK_HOMOL`).

### Secrets Utilizados no GitHub Actions (`Settings > Secrets and variables > Actions`)

| Secret | Descrição |
| :--- | :--- |
| `GITHUB_TOKEN` | Automático do GitHub — utilizado para publicar a imagem no `ghcr.io` |
| `SONAR_TOKEN` | Token gerado no SonarCloud para envio da análise estática |
| `SONAR_ORGANIZATION` | *(Opcional)* Organização no SonarCloud (padrão: `maddumuller`) |
| `SONAR_PROJECT_KEY` | *(Opcional)* Project Key no SonarCloud (padrão: `maddumuller_auth-service`) |
| `RENDER_DEPLOY_HOOK_DEV` | URL do Deploy Hook do serviço `auth-service-dev` no Render |
| `RENDER_DEPLOY_HOOK_HOMOL` | URL do Deploy Hook do serviço `auth-service-homol` no Render |

---

## 🐳 Executando Localmente

### Testes e Build:
```bash
./mvnw clean test
./mvnw clean verify
```

### Docker:
```bash
docker build -t auth-service .
```
