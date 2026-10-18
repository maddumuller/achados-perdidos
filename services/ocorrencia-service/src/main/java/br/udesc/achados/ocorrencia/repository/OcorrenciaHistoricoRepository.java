package br.udesc.achados.ocorrencia.repository;

import br.udesc.achados.ocorrencia.domain.OcorrenciaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OcorrenciaHistoricoRepository extends JpaRepository<OcorrenciaHistorico, Long> {

    List<OcorrenciaHistorico> findByOcorrenciaIdOrderByCriadoEmAsc(UUID ocorrenciaId);
}
