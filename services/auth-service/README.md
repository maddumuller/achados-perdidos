# Auth Service (auth-service) - Sistema de Achados e Perdidos (UDESC)

Microsserviço de autenticação, autorização e gerenciamento de usuários do Sistema de Achados e Perdidos.

---

## 🛠️ Tecnologias e Decisões

- **Java 21** e **Spring Boot 3.4.3**
- **Persistência**: Spring Data JPA + H2 embarcado em modo arquivo (`/data/authdb` no container / `./data/authdb` local)
- **Segurança**: Criptografia de senhas com **BCrypt** (`spring-security-crypto`) e emissão de tokens **JWT** (HS256 com `jjwt 0.12.6`)
- **Mensageria**: RabbitMQ (`achados.events` topic exchange) com publicação do evento `usuario.desativado` após o commit da transação
- **Proteção contra Força Bruta**: Bloqueio de conta por 15 minutos após 5 erros consecutivos de senha

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
| `SERVER_PORT` | `8081` | Porta do serviço |
| `JWT_SECRET` | `troque-este-segredo-com-no-minimo-32-caracteres` | Chave secreta para assinatura dos tokens JWT |
| `JWT_EXPIRATION_MINUTES` | `120` | Tempo de expiração do JWT em minutos |
| `ADMIN_EMAIL` | `admin@achados.local` | E-mail do administrador padrão inicial |
| `ADMIN_PASSWORD` | `Admin@1234` | Senha do administrador padrão inicial |
| `DB_PATH` | `./data/authdb` | Caminho do arquivo de banco H2 |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |

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

---

## 🐳 Executando

### Testes e Build:
```bash
./mvnw clean test
./mvnw clean package
```

### Docker:
```bash
docker build -t auth-service .
```
