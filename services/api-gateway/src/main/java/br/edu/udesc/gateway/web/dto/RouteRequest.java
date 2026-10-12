package br.edu.udesc.gateway.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RouteRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O pathPrefix é obrigatório")
        String pathPrefix,

        @NotBlank(message = "O targetUrl é obrigatório")
        String targetUrl,

        boolean stripPrefix,

        boolean requiresAuth,

        String requiredRole,

        @Min(value = 1, message = "O rateLimitPerMinute deve ser no mínimo 1")
        int rateLimitPerMinute,

        boolean enabled
) {
}
