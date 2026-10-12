# Ocorrência Service (ocorrencia-service) - Sistema de Achados e Perdidos (UDESC)

Microsserviço de gestão de reivindicações e ocorrências de itens achados e perdidos (C4 da arquitetura). Coordena o processo de reivindicação com o `item-service` através de chamadas síncronas (`RestClient`) e coreografia de eventos da Saga com RabbitMQ.

---

## 🛠️ Tecnologias e Decisões

- **Java 21** e **Spring Boot 3.4.3**
- **Persistência**: Spring Data JPA + H2 embarcado em modo arquivo (`/data/ocorrenciadb` no container / `./data/ocorrenciadb` local)
- **Controle de Concorrência**: Bloqueio otimista via `@Version` na entidade `Ocorrencia`
- **Mensageria**: RabbitMQ (`achados.events` topic exchange) com publicação assíncrona após o commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`)
- **Chamada Síncrona**: `RestClient` para consulta prévia de item no `item-service` com timeout configurado
- **Jobs Agendados**: `@Scheduled` para cancelamento de reservas expiradas (timeout da reserva)

---

## 📌 Regras de Negócio e Saga

- **RN-O1: Abertura**: Exige `itemId` e `justificativa` (20 a 1000 caracteres). Valida previamente via REST no `item-service` se o item existe, está com status `ABERTO`, e não pertence ao próprio usuário que está reivindicando.
- **RN-O2: Uma ocorrência ativa**: Impede que o mesmo usuário tenha mais de uma ocorrência ativa (`AGUARDANDO_RESERVA`, `EM_ANALISE`, `APROVADA`) para o mesmo item (`409 Conflict`).
- **RN-O3: Máquina de Estados**:
  - `AGUARDANDO_RESERVA` -> `EM_ANALISE` (via evento `item.reservado`), `CANCELADA`
  - `EM_ANALISE` -> `APROVADA` (ADMIN), `REJEITADA` (ADMIN), `CANCELADA` (dono/ADMIN)
  - `APROVADA` -> `DEVOLVIDA` (ADMIN), `CANCELADA` (dono/ADMIN)
  - `DEVOLVIDA`, `REJEITADA`, `CANCELADA`: estados finais.
  - Toda transição gera registro em `ocorrencia_historico`.
- **RN-O4: Moderação pelo Administrador**: Apenas administradores podem aprovar, rejeitar (exige motivo min 5 caracteres) e confirmar devolução de um item.
- **RN-O5: Saga e Compensação**:
  - Abertura publica `ocorrencia.reivindicada`.
  - Confirmação de devolução publica `ocorrencia.devolucao-confirmada`.
  - Rejeição, cancelamento ou timeout publicam `ocorrencia.cancelada` para liberar o item de volta para `ABERTO` no `item-service`.
  - Tratamento de reserva tardia: se `item.reservado` chegar para ocorrência já cancelada, republica `ocorrencia.cancelada`.
- **RN-O6: Timeout da Reserva**: Ocorrências em `AGUARDANDO_RESERVA` por mais de 5 minutos (configurável) são canceladas automaticamente pelo `ReservaTimeoutJob` com motivo "Sem resposta do item-service".
- **RN-O7: Visibilidade**: Usuário comum visualiza apenas as próprias reivindicações. Administrador visualiza todas com suporte a paginação e filtros por `status` e `itemId`.
- **RN-O8: Usuário Desativado**: Ao receber evento `usuario.desativado`, cancela todas as ocorrências ativas do respectivo usuário e publica os cancelamentos de compensação.

---

## ⚙️ Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `8082` | Porta do serviço |
| `DB_PATH` | `./data/ocorrenciadb` | Caminho do arquivo de banco H2 |
| `ITEM_SERVICE_URL` | `http://localhost:8083` | URL do item-service para chamadas síncronas |
| `OCORRENCIA_RESERVA_TIMEOUT_MINUTES` | `5` | Minutos até expiração de uma reserva sem resposta |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |

---

## 🚀 Endpoints (`/api/ocorrencias`)

Todas as requisições exigem os headers propagados pelo API Gateway (`X-User-Id`, `X-User-Roles`, `X-Correlation-Id`).

### Usuário Autenticado
- `POST /api/ocorrencias` - Abre uma nova ocorrência / reivindicação (`201 Created`)
- `GET /api/ocorrencias/meus?page=0&size=20&status={STATUS}` - Lista as próprias ocorrências
- `GET /api/ocorrencias/{id}` - Detalhes da ocorrência (dono ou ADMIN)
- `GET /api/ocorrencias/{id}/historico` - Histórico de transições (dono ou ADMIN)
- `POST /api/ocorrencias/{id}/cancelar` - Cancela a ocorrência (dono ou ADMIN)

### Administrativos (Exige `ADMIN`)
- `GET /api/ocorrencias?page=0&size=20&status={STATUS}&itemId={UUID}` - Lista todas com filtros
- `POST /api/ocorrencias/{id}/aprovar` - Aprova a reivindicação (`EM_ANALISE -> APROVADA`)
- `POST /api/ocorrencias/{id}/rejeitar` - Rejeita a reivindicação (`EM_ANALISE -> REJEITADA`)
- `POST /api/ocorrencias/{id}/devolucao` - Confirma entrega do item (`APROVADA -> DEVOLVIDA`)

---

## 📦 Eventos RabbitMQ

- **Exchange**: `achados.events` (topic)
- **Publicados**:
  - `ocorrencia.reivindicada`
  - `ocorrencia.devolucao-confirmada`
  - `ocorrencia.cancelada`
- **Consumidos**:
  - `item.reservado` (fila `ocorrencia-service.item`)
  - `item.reserva-negada` (fila `ocorrencia-service.item`)
  - `usuario.desativado` (fila `ocorrencia-service.usuario`)
