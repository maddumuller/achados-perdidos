package br.udesc.achados.ocorrencia.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaDevolucaoConfirmadaEvent(
        UUID ocorrenciaId,
        UUID itemId,
        Instant occurredAt
) {
    public OcorrenciaDevolucaoConfirmadaEvent(UUID ocorrenciaId, UUID itemId) {
        this(ocorrenciaId, itemId, Instant.now());
    }
}
