# API Gateway - Sistema de Achados e Perdidos (UDESC)

Porta de entrada única para o sistema distribuído de Achados e Perdidos. Roteia requisições REST para os microsserviços (`item-service`, `auth-service`, `ocorrencia-service`), centraliza segurança, resiliência e auditoria.

---

## 🛠️ Tecnologias e Decisões de Arquitetura

- **Java 21** e **Spring Boot 3.4.3** (Stack Servlet Spring MVC)
- **Proxy HTTP**: `RestClient` com `JdkClientHttpRequestFactory` (suporte a GET, POST, PUT, DELETE, PATCH)
- **Persistência**: Spring Data JPA + H2 embarcado em modo arquivo (`/data/gatewaydb` no container / `./data/gatewaydb` local)
- **Segurança**: Validação de JWT (HS256 com `jjwt 0.12.6`), extração de roles e usuário sem acoplamento ao Spring Security
- **Resiliência**: Resilience4j Circuit Breaker por rota
- **Rate Limiting**: Controle de taxa em memória por cliente (`userId` ou IP) e rota, com janela fixa de 1 minuto e cabeçalho `Retry-After`
- **Auditoria**: Persistência de logs de requisição (`RequestLog`) para todas as chamadas roteadas
- **Administração**: Endpoints de gestão de rotas protegidos via cabeçalho `X-Admin-Key`

---

## 📌 Regras de Negócio Implementadas

1. **RN1 - Roteamento Dinâmico**: Casamento de `/api/**` com rotas ativas por maior prefixo (`RouteDefinition`). Se não encontrar, retorna `404 Not Found`.
2. **RN2 - Autenticação & Autorização**: Rotas com `requiresAuth = true` exigem `Authorization: Bearer <jwt>` válido. Falha no token retorna `401 Unauthorized`. Se a rota exigir `requiredRole` e o usuário não possuir, retorna `403 Forbidden`.
3. **RN3 - Rate Limit**: Limite por minuto por cliente e rota. Ao exceder, retorna `429 Too Many Requests` com header `Retry-After`.
4. **RN4 - Resiliência (Circuit Breaker)**: Cada rota possui um circuit breaker. Falhas de conexão ou timeout retornam `502 Bad Gateway` ou `503 Service Unavailable`. Respostas 4xx/5xx vindas do microsserviço de destino passam direto ao cliente.
5. **RN5 - Auditoria**: Todas as requisições geram registro na tabela `request_logs` com status, duração, correlation ID, cliente e rota.
6. **RN6 - Administração**: CRUD de rotas em `/gateway/admin/routes` e consulta de logs em `/gateway/admin/logs`, protegidos por `X-Admin-Key`.

---

## ⚙️ Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `8080` | Porta do Gateway |
| `JWT_SECRET` | `troque-este-segredo-com-no-minimo-32-caracteres` | Chave secreta para validação dos tokens JWT |
| `ADMIN_KEY` | `admin-dev-key` | Chave exigida no header `X-Admin-Key` para rotas de admin |
| `AUTH_SERVICE_URL` | `http://localhost:8082` | URL do serviço de autenticação |
| `OCORRENCIA_SERVICE_URL` | `http://localhost:8083` | URL do serviço de ocorrências |
| `ITEM_SERVICE_URL` | `http://localhost:8081` | URL do serviço de itens |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/gatewaydb` | URL de conexão do H2 |

---

## 🚀 Endpoints

### 1. Proxy (Consumido pelo Frontend / Clientes)
- Qualquer método em `/api/**`, direcionado para o serviço mapeado.
- Header injetado para downstream:
  - `X-Correlation-Id`: Identificador de correlação (propaga ou gera UUID)
  - `X-Forwarded-For`: IP do cliente
  - `X-User-Id`: ID do usuário autenticado (anti-spoofing)
  - `X-User-Roles`: Roles do usuário autenticado

### 2. Rotas Administrativas (`X-Admin-Key: <ADMIN_KEY>`)
- `GET /gateway/admin/routes` - Lista rotas cadastradas
- `POST /gateway/admin/routes` - Cadastra nova rota (201 Created)
- `GET /gateway/admin/routes/{id}` - Detalha uma rota
- `PUT /gateway/admin/routes/{id}` - Atualiza rota existente
- `DELETE /gateway/admin/routes/{id}` -  Remove rota (204 No Content)
- `GET /gateway/admin/logs?page=0&size=20` - Consulta logs de auditoria paginados

### 3. Saúde do Gateway
- `GET /actuator/health` - Status do gateway (público)

---

## 🐳 Executando com Docker

### Build local do JAR:
```bash
./mvnw clean package
```

### Executar testes:
```bash
./mvnw test
```

### Subir com Docker Compose na raiz do projeto:
```bash
docker compose up --build
```
