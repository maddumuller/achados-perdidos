package br.udesc.achados.ocorrencia.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOcorrenciaRequest(
        @NotNull(message = "O ID do item é obrigatório")
        UUID itemId,

        @NotBlank(message = "A justificativa é obrigatória")
        @Size(min = 20, max = 1000, message = "A justificativa deve ter entre 20 e 1000 caracteres")
        String justificativa
) {
}
