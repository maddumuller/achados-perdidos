package br.udesc.achados.ocorrencia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocorrencia")
public record OcorrenciaProperties(
        String itemServiceUrl,
        int reservaTimeoutMinutes
) {
}
