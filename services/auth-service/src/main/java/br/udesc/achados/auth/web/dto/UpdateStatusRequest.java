package br.udesc.achados.auth.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "O status ativo é obrigatório")
        Boolean ativo
) {
}
