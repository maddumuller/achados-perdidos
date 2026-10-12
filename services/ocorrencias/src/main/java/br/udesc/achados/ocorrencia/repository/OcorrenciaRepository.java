package br.udesc.achados.ocorrencia.repository;

import br.udesc.achados.ocorrencia.domain.Ocorrencia;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID>, JpaSpecificationExecutor<Ocorrencia> {

    boolean existsByUsuarioIdAndItemIdAndStatusIn(UUID usuarioId, UUID itemId, Collection<StatusOcorrencia> statuses);

    Page<Ocorrencia> findByUsuarioId(UUID usuarioId, Pageable pageable);

    Page<Ocorrencia> findByUsuarioIdAndStatus(UUID usuarioId, StatusOcorrencia status, Pageable pageable);

    List<Ocorrencia> findByUsuarioIdAndStatusIn(UUID usuarioId, Collection<StatusOcorrencia> statuses);

    List<Ocorrencia> findByStatusAndCriadoEmBefore(StatusOcorrencia status, Instant instante);
}
