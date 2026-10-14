package br.udesc.achados.ocorrencia.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaCanceladaEvent(
        UUID ocorrenciaId,
        UUID itemId,
        String motivo,
        Instant occurredAt
) {
    public OcorrenciaCanceladaEvent(UUID ocorrenciaId, UUID itemId, String motivo) {
        this(ocorrenciaId, itemId, motivo, Instant.now());
    }
}
