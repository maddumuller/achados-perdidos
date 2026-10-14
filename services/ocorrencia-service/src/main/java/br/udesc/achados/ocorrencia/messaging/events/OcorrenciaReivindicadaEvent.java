package br.udesc.achados.ocorrencia.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaReivindicadaEvent(
        UUID ocorrenciaId,
        UUID itemId,
        UUID usuarioId,
        Instant occurredAt
) {
    public OcorrenciaReivindicadaEvent(UUID ocorrenciaId, UUID itemId, UUID usuarioId) {
        this(ocorrenciaId, itemId, usuarioId, Instant.now());
    }
}
