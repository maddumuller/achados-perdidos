package br.udesc.achados.ocorrencia.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemReservadoMessage(
        UUID itemId,
        UUID ocorrenciaId,
        Instant occurredAt
) {
}
