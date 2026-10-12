package br.edu.udesc.gateway.web.dto;

import br.edu.udesc.gateway.domain.RequestLog;

import java.time.Instant;

public record RequestLogResponse(
        Long id,
        String correlationId,
        String method,
        String path,
        String routeName,
        int status,
        long durationMs,
        String clientIp,
        String userId,
        Instant createdAt
) {
    public static RequestLogResponse fromEntity(RequestLog entity) {
        return new RequestLogResponse(
                entity.getId(),
                entity.getCorrelationId(),
                entity.getMethod(),
                entity.getPath(),
                entity.getRouteName(),
                entity.getStatus(),
                entity.getDurationMs(),
                entity.getClientIp(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }
}
