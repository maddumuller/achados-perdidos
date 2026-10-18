package br.udesc.achados.ocorrencia.web.dto;

import br.udesc.achados.ocorrencia.domain.OcorrenciaHistorico;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;

import java.time.Instant;

public record OcorrenciaHistoricoResponse(
        Long id,
        StatusOcorrencia de,
        StatusOcorrencia para,
        String motivo,
        String autor,
        Instant criadoEm
) {
    public static OcorrenciaHistoricoResponse fromEntity(OcorrenciaHistorico h) {
        return new OcorrenciaHistoricoResponse(
                h.getId(),
                h.getDe(),
                h.getPara(),
                h.getMotivo(),
                h.getAutor(),
                h.getCriadoEm()
        );
    }
}
