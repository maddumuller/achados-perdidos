# Sistema de Achados e Perdidos (UDESC)

Sistema de Achados e Perdidos desenvolvido em arquitetura de microsserviços para a disciplina de DevOps.

---

## 🏛️ Arquitetura dos Serviços

```
                        [ Cliente / Frontend ]
                                  │
                                  ▼
                         [ API Gateway ] (Porta 8080)
                         (C1: JWT, Roteamento, CB, Rate Limit)
                                  │
         ┌────────────────────────┼────────────────────────┐
         ▼                        ▼                        ▼
  [ Auth Service ]       [ Ocorrência Service ]     [ Item Service ]
  (C2: Porta 8081)        (C4: Porta 8082)          (C6: Porta 8081)
     (H2 DB)                   (H2 DB)                 (Postgres)
         │                        │                        │
         └─────────────┬──────────┴────────────────────────┘
                       ▼
                 [ RabbitMQ ]
                 (Exchange: achados.events)
```

1. **[api-gateway](file:///services/api-gateway/README.md)**: Porta única de entrada (8080), validação centralizada de JWT, rate limiting, circuit breaker, logs de auditoria e roteamento.
2. **[auth-service (item-auth)](file:///services/item-auth/README.md)**: Cadastro, login, emissão de JWT, controle de papéis (`USER`/`ADMIN`), proteção de força bruta e publicação de `usuario.desativado`.
3. **[ocorrencia-service](file:///services/ocorrencias/README.md)**: Gerenciamento do ciclo de vida das reivindicações de itens, máquina de estados, moderação do ADMIN e coordenação da Saga coreografada.
4. **[item-service](file:///services/item-service/README.md)**: Catálogo dos itens achados e perdidos com persistência em PostgreSQL.

---

## 🚀 Como Executar com Docker Compose

Suba todo o ecossistema com um único comando:

```bash
docker compose up -d --build
```

### Serviços e Portas Expostas:

- **API Gateway**: `http://localhost:8080` (Acesso principal para todas as rotas de API)
- **RabbitMQ Dashboard**: `http://localhost:15672` (Credenciais: `guest` / `guest`)
- **Postgres (Item DB)**: `localhost:5433`

---

## 🔐 Credenciais Padrão do Seed

- **ADMIN Inicial**:
  - E-mail: `admin@achados.local`
  - Senha: `Admin@1234`
- **Gateway Admin Key**:
  - Header: `X-Admin-Key: admin-dev-key`
