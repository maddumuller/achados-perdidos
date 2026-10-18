# Sistema de Achados e Perdidos (UDESC)

Sistema de Achados e Perdidos desenvolvido em arquitetura de microsserviços para a disciplina de DevOps.

---

##  Arquitetura dos Serviços

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

1. **[api-gateway](services/api-gateway/README.md)**: Porta única de entrada (8080), validação centralizada de JWT, rate limiting, circuit breaker, logs de auditoria e roteamento.
2. **[auth-service](services/auth-service/README.md)**: Cadastro, login, emissão de JWT, controle de papéis (`USER`/`ADMIN`), proteção de força bruta e publicação de `usuario.desativado`.
3. **[ocorrencia-service](services/ocorrencia-service/README.md)**: Gerenciamento do ciclo de vida das reivindicações de itens, máquina de estados, moderação do ADMIN e coordenação da Saga coreografada.
4. **[item-service](services/item-service/README.md)**: Catálogo dos itens achados e perdidos com persistência em PostgreSQL.

---

##  Como os Microsserviços Funcionam em Conjunto

A comunicação entre os serviços combina **comunicação síncrona (REST)** para operações de leitura imediata e validação rápida com **comunicação assíncrona orientada a eventos (RabbitMQ / Saga)** para garantir consistência eventual e desacoplamento operacional.

```
                      ┌────────────────────────────────────────┐
                      │          Cliente / Frontend            │
                      └──────────────────┬─────────────────────┘
                                         │ (1) Chamada HTTP
                                         ▼
                      ┌────────────────────────────────────────┐
                      │              API Gateway               │
                      │  • Validação de JWT                    │
                      │  • Rate Limiting & Circuit Breaker     │
                      │  • Injeção de X-User-Id / Roles        │
                      └────┬─────────────────┬──────────────┬──┘
                           │ (2)             │ (2)          │ (2)
                           ▼                 ▼              ▼
                 ┌──────────────┐   ┌─────────────┐   ┌────────────┐
                 │ Auth Service │   │ Item Service│   │ Ocorrência │
                 └──────┬───────┘   └──────▲──────┘   │  Service   │
                        │                  │          └─────┬──────┘
                        │                  │ (3) REST Síncrono│
                        │                  │     (ItemClient) │
                        │                  └──────────────────┘
                        │ (Evento)                     │ (Eventos Saga)
                        ▼                              ▼
             ┌─────────────────────────────────────────────────────────┐
             │                  RabbitMQ (achados.events)              │
             │                                                         │
             │  • usuario.desativado       • ocorrencia.reivindicada   │
             │  • item.reservado           • ocorrencia.cancelada      │
             │  • item.reserva-negada      • ocorrencia.devolucao-...  │
             └─────────────────────────────────────────────────────────┘
```

### 1. Fluxo de Autenticação e Propagação de Identidade (Edge Security)
1. **Login e Obtenção de Token**:
   - O cliente envia `POST /api/auth/login` para o Gateway (porta 8080).
   - O Gateway roteia a chamada para o `auth-service`, que valida credenciais e retorna um token JWT assinado.
2. **Requisições Protegidas**:
   - Em qualquer requisição subsequente (ex: criar ocorrência ou listar itens), o cliente envia `Authorization: Bearer <jwt>` para o Gateway.
   - O Gateway valida a assinatura e expiração do JWT de forma independente (sem consultar banco ou rede), extrai as *claims* do usuário e verifica permissões.
3. **Propagação Downstream Confiável**:
   - Ao encaminhar a chamada para `ocorrencia-service` ou `item-service`, o Gateway remove cabeçalhos externos inseguros e injeta `X-User-Id`, `X-User-Roles` e `X-Correlation-Id`.
   - Os serviços internos confiam nesses cabeçalhos e não precisam reinicializar contextos complexos de segurança nem revalidar senhas/tokens.

### 2. Fluxo Síncrono: Consulta e Pré-validação de Elegibilidade
- Antes de registrar uma nova reivindicação, o `ocorrencia-service` executa uma chamada HTTP síncrona direta via `RestClient` (`ItemClient`) para o `item-service` (`GET /api/itens/{id}`).
- **Benefício**: Garante que o item realmente existe, está em estado compatível (`DISPONIVEL`) e não pertence ao próprio usuário antes de criar registros desnecessários ou disparar mensagens assíncronas.
- Caso o `item-service` esteja instável, o `ocorrencia-service` trata a exceção e retorna `503 Service Unavailable` de forma graciosa.

### 3. Fluxo Assíncrono: Saga Coreografada de Reivindicação de Itens
Para garantir a consistência das transações distribuídas sem bloqueios globais (2PC), o sistema adota uma **Saga Coreografada** via RabbitMQ (Topic Exchange `achados.events`):

#### Cenário de Sucesso (Reivindicação e Aprovação):
1. **Abertura da Ocorrência**: O usuário abre uma reivindicação no `ocorrencia-service`. Uma ocorrência é criada no banco com status `AGUARDANDO_RESERVA`.
2. **Publicação do Evento**: Ao commitar a transação, o `ocorrencia-service` publica `ocorrencia.reivindicada` com o ID da ocorrência e do item.
3. **Reserva do Item**: O `item-service` consome o evento, atualiza o item para `RESERVADO` e emite o evento `item.reservado`.
4. **Confirmação da Reserva**: O `ocorrencia-service` consome `item.reservado` e avança o status da ocorrência para `EM_ANALISE`.
5. **Moderação e Devolução**:
   - Um administrador avalia e aprova a reivindicação (`APROVADA`).
   - Após a entrega física, o administrador confirma a entrega (`POST /api/ocorrencias/{id}/devolucao`), transicionando a ocorrência para `DEVOLVIDA`.
   - O serviço publica `ocorrencia.devolucao-confirmada`, fazendo com que o `item-service` atualize permanentemente o status do item para `DEVOLVIDO`.

#### Cenário de Compensação (Rejeição, Cancelamento ou Timeout):
- **Rejeição por Admin ou Cancelamento pelo Usuário**:
  - Se o administrador rejeitar a solicitação (`REJEITADA`) ou o usuário cancelar (`CANCELADA`), o `ocorrencia-service` transiciona o estado e publica o evento de compensação `ocorrencia.cancelada`.
- **Timeout da Reserva**:
  - Caso o `item-service` não responda no tempo hábil, o agendador `ReservaTimeoutJob` cancela automaticamente ocorrências em `AGUARDANDO_RESERVA` há mais de 5 minutos e publica `ocorrencia.cancelada`.
- **Efeito Compensatório**:
  - Ao receber `ocorrencia.cancelada`, o `item-service` restaura o status do item de volta para `DISPONIVEL`, liberando o pertence para novos interessados.

### 4. Fluxo de Bloqueio em Cascata (Segurança Distribuída)
1. Um administrador desativa uma conta no `auth-service` (`PATCH /api/auth/users/{id}/status`).
2. O `auth-service` persiste a alteração e publica o evento `usuario.desativado`.
3. O `ocorrencia-service` escuta o evento através da fila dedicada `ocorrencia-service.usuario`.
4. Todas as ocorrências em andamento vinculadas ao usuário desativado são imediatamente canceladas (`CANCELADA`).
5. As compensações correspondentes (`ocorrencia.cancelada`) são publicadas para liberar imediatamente todos os itens que estavam temporariamente retidos pelo usuário bloqueado.

---

##  Como Executar com Docker Compose

Suba todo o ecossistema com um único comando:

```bash
docker compose up -d --build
```

### Serviços e Portas Expostas:

- **API Gateway**: `http://localhost:8080` (Acesso principal para todas as rotas de API)
- **RabbitMQ Dashboard**: `http://localhost:15672` (Credenciais: `guest` / `guest`)
- **Postgres (Item DB)**: `localhost:5433`

---

##  Credenciais Padrão do Seed

- **ADMIN Inicial**:
  - E-mail: `admin@achados.local`
  - Senha: `Admin@1234`
- **Gateway Admin Key**:
  - Header: `X-Admin-Key: admin-dev-key`

---

## ⚙️ Integração Contínua (CI - GitHub Actions)

O pipeline de CI está configurado em [`.github/workflows/ci.yml`](.github/workflows/ci.yml) e é executado automaticamente em:
- **Pushes** nas branches `main` e `develop`
- **Pull Requests** direcionados a `main` e `develop`
- **Disparo manual** (*workflow_dispatch*) na aba Actions do GitHub

### Etapas do Pipeline:
1. **Build & Test (Matrix)**: Execução paralela para cada um dos 4 microsserviços (`api-gateway`, `item-auth`, `item-service`, `ocorrencias`):
   - Configuração do **JDK 21** (Eclipse Temurin) com cache inteligente do Maven.
   - Concessão de permissões de execução para o Maven Wrapper (`mvnw`).
   - Compilação do código, resolução de dependências e execução de todos os testes unitários e de integração (`mvn clean verify`).
2. **Docker Compose Build**:
   - Disparado após a aprovação de todos os testes.
   - Valida a integridade sintática e constrói as imagens Docker de todos os serviços declarados no [`compose.yaml`](compose.yaml).
