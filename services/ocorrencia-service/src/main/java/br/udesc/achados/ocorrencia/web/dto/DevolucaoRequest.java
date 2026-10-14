package br.udesc.achados.ocorrencia.web.dto;

import jakarta.validation.constraints.Size;

public record DevolucaoRequest(
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
