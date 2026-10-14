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

1. **[api-gateway](services/api-gateway/README.md)**: Porta única de entrada (8080), validação centralizada de JWT, rate limiting, circuit breaker, logs de auditoria e roteamento.
2. **[auth-service](services/auth-service/README.md)**: Cadastro, login, emissão de JWT, controle de papéis (`USER`/`ADMIN`), proteção de força bruta e publicação de `usuario.desativado`.
3. **[ocorrencia-service](services/ocorrencia-service/README.md)**: Gerenciamento do ciclo de vida das reivindicações de itens, máquina de estados, moderação do ADMIN e coordenação da Saga coreografada.
4. **[item-service](services/item-service/README.md)**: Catálogo dos itens achados e perdidos com persistência em PostgreSQL.

---

## 🧩 Responsabilidades de Cada Microsserviço

Cada microsserviço foi concebido seguindo o princípio da responsabilidade única (*Single Responsibility Principle*) e isolamento de contexto de domínio:

### 1. 🚪 API Gateway (`api-gateway`)
- **Papel**: Ponto Único de Entrada (*Single Point of Entry*) e Protetor de Perímetro (*Reverse Proxy*).
- **Porta**: `8080` (única porta exposta diretamente aos clientes/frontend).
- **Banco de Dados**: H2 embarcado em modo arquivo (`gatewaydb`) para tabelas de rotas e auditoria.
- **Principais Responsabilidades**:
  - **Roteamento Dinâmico**: Roteia requisições em tempo de execução (`/api/auth/**`, `/api/itens/**`, `/api/ocorrencias/**`) casando pelo maior prefixo configurado.
  - **Autenticação & Autorização Centralizadas**: Intercepta requisições protegidas, valida assinaturas de tokens JWT (HS256) sem onerar o serviço de autenticação, e valida RBAC (permissões `USER` ou `ADMIN`).
  - **Propagação Segura de Identidade (Anti-Spoofing)**: Injeta cabeçalhos confiáveis para os microsserviços downstream:
    - `X-User-Id`: Identificador único do usuário autenticado.
    - `X-User-Roles`: Papéis e privilégios associados.
    - `X-Correlation-Id`: Identificador unificado para rastreamento distribuído de requisições.
    - `X-Forwarded-For`: Endereço IP original do cliente.
  - **Resiliência (Circuit Breaker)**: Utiliza Resilience4j por rota para isolar falhas temporárias ou indisponibilidade de serviços downstream.
  - **Rate Limiting**: Controle de taxa em memória por cliente e por rota (com janela de 1 minuto e header `Retry-After`), prevenindo abusos e negação de serviço.
  - **Auditoria & Administração**: Registra todas as chamadas em tabela `request_logs` e disponibiliza API administrativa interna para gerenciamento dinâmico de rotas via header `X-Admin-Key`.

### 2. 🛡️ Auth Service (`auth-service`)
- **Papel**: Gerenciamento de Identidade, Acesso e Usuários (IAM).
- **Porta Interna**: `8081` (no container `auth-service`).
- **Banco de Dados**: H2 embarcado em modo arquivo (`authdb`).
- **Principais Responsabilidades**:
  - **Cadastro & Credenciais**: Registro de novos usuários com validação estrita de dados e armazenamento seguro de senhas através de hash unidirecional **BCrypt**.
  - **Autenticação & Emissão de Tokens**: Validação de credenciais e geração de tokens JWT contendo ID do usuário, e-mail e papéis (`USER`/`ADMIN`).
  - **Proteção contra Ataques de Força Bruta**: Bloqueio automático de conta por 15 minutos (`423 Locked`) após 5 tentativas consecutivas de senha inválida.
  - **Autogestão de Conta**: Alteração de senha autenticada pelo próprio titular da conta.
  - **Administração de Usuários**: Listagem paginada, promoção/rebaixamento de papéis e ativação/desativação de contas (exclusivo para `ADMIN`).
  - **Mensageria**: Publicação do evento `usuario.desativado` na exchange `achados.events` do RabbitMQ ao suspender uma conta, disparando ações compensatórias pelo sistema.

### 3. 📦 Item Service (`item-service`)
- **Papel**: Catálogo Central e Inventário de Itens Achados e Perdidos.
- **Porta**: `8081` (exposta internamente e no host).
- **Banco de Dados**: PostgreSQL (`itens_db`), garantindo persistência relacional robusta.
- **Principais Responsabilidades**:
  - **Catálogo de Itens**: Cadastro, listagem geral, consulta por identificador e detalhamento de pertences (nome, descrição, categoria, localização de encontro e data de registro).
  - **Gestão de Disponibilidade**: Controle de status do item no inventário (`DISPONIVEL`, `RESERVADO`, `DEVOLVIDO`).
  - **Interface REST para Consulta**: Fornece endpoints síncronos consumidos tanto pelo Gateway quanto diretamente pelo `ocorrencia-service` para pré-validação antes da abertura de ocorrências.

### 4. 📋 Ocorrência Service (`ocorrencia-service`)
- **Papel**: Gestão do Ciclo de Vida de Reivindicações e Coordenação da Saga.
- **Porta Interna**: `8082` (no container `ocorrencia-service`).
- **Banco de Dados**: H2 embarcado em modo arquivo (`ocorrenciadb`).
- **Principais Responsabilidades**:
  - **Abertura de Reivindicações**: Registro de intenção de reivindicação de um item com justificativa detalhada e validação de duplicidade por usuário.
  - **Máquina de Estados Finita e Auditável**:
    - `AGUARDANDO_RESERVA` ➔ `EM_ANALISE` ➔ `APROVADA` / `REJEITADA` / `CANCELADA` ➔ `DEVOLVIDA`.
    - Controle de concorrência com bloqueio otimista (`@Version`).
    - Registro de cada mudança de estado na tabela de auditoria `ocorrencia_historico`.
  - **Moderação Administrativa**: Endpoints exclusivos para administradores aprovarem, rejeitarem (com motivo obrigatório) ou confirmarem a devolução física do item.
  - **Consulta com Restrição de Escopo**: Usuários comuns visualizam apenas suas próprias ocorrências; administradores visualizam todas com filtros por status e item.
  - **Coordenação da Saga Distribuída (RabbitMQ)**:
    - Pré-validação síncrona via `RestClient` no `item-service` para conferir existência e status do item.
    - Publicação de eventos de ciclo de vida (`ocorrencia.reivindicada`, `ocorrencia.devolucao-confirmada`, `ocorrencia.cancelada`).
    - Consumo de respostas de reserva (`item.reservado`, `item.reserva-negada`).
    - Cancelamento automático por timeout (`ReservaTimeoutJob` a cada 5 minutos para ocorrências pendentes em `AGUARDANDO_RESERVA`).
    - Reação ao evento `usuario.desativado`: cancela automaticamente todas as ocorrências ativas de usuários suspensos e compensa liberando os itens.

---

## 🔄 Como os Microsserviços Funcionam em Conjunto

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
