package br.edu.udesc.gateway.web.dto;

import br.edu.udesc.gateway.domain.RouteDefinition;

public record RouteResponse(
        Long id,
        String name,
        String pathPrefix,
        String targetUrl,
        boolean stripPrefix,
        boolean requiresAuth,
        String requiredRole,
        int rateLimitPerMinute,
        boolean enabled
) {
    public static RouteResponse fromEntity(RouteDefinition entity) {
        return new RouteResponse(
                entity.getId(),
                entity.getName(),
                entity.getPathPrefix(),
                entity.getTargetUrl(),
                entity.isStripPrefix(),
                entity.isRequiresAuth(),
                entity.getRequiredRole(),
                entity.getRateLimitPerMinute(),
                entity.isEnabled()
        );
    }
}
