package br.udesc.achados.ocorrencia.web.dto;

import br.udesc.achados.ocorrencia.domain.Ocorrencia;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;

import java.time.Instant;
import java.util.UUID;

public record OcorrenciaResponse(
        UUID id,
        UUID itemId,
        String itemTitulo,
        String itemTipo,
        UUID usuarioId,
        String justificativa,
        StatusOcorrencia status,
        String observacaoAdmin,
        String motivoFinal,
        UUID analisadoPor,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static OcorrenciaResponse fromEntity(Ocorrencia o) {
        return new OcorrenciaResponse(
                o.getId(),
                o.getItemId(),
                o.getItemTitulo(),
                o.getItemTipo(),
                o.getUsuarioId(),
                o.getJustificativa(),
                o.getStatus(),
                o.getObservacaoAdmin(),
                o.getMotivoFinal(),
                o.getAnalisadoPor(),
                o.getCriadoEm(),
                o.getAtualizadoEm()
        );
    }
}
