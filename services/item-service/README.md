# Item Service - Sistema de Achados e Perdidos (UDESC)

Microsserviço responsável pelo catálogo e gerenciamento de itens achados e perdidos (C6 da arquitetura).

---

## 🛠️ Tecnologias e Decisões

- **Java 21** e **Spring Boot**
- **Persistência**: Spring Data JPA com **PostgreSQL**
- **Build**: Maven com Dockerfile multi-stage

---

## ⚙️ Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `8081` | Porta do serviço |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/itens_db` | URL de conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | - | Senha do banco |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Estratégia de DDL Hibernate |

---

## 🚀 Endpoints (`/itens`)

- `POST /itens` - Cadastra um novo item (`201 Created`)
- `GET /itens` - Lista todos os itens cadastrados (`200 OK`)
- `GET /itens/{id}` - Busca item por ID (`200 OK`)
- `PATCH /itens/{id}/status` - Atualiza o status do item (`200 OK`)
