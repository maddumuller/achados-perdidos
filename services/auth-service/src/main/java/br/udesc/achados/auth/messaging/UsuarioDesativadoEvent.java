package br.udesc.achados.auth.messaging;

import java.time.Instant;
import java.util.UUID;

public record UsuarioDesativadoEvent(
        UUID usuarioId,
        Instant occurredAt
) {
    public UsuarioDesativadoEvent(UUID usuarioId) {
        this(usuarioId, Instant.now());
    }
}
