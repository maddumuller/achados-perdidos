package br.udesc.achados.ocorrencia.web.dto;

import jakarta.validation.constraints.Size;

public record CancelarRequest(
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        String motivo
) {
}
