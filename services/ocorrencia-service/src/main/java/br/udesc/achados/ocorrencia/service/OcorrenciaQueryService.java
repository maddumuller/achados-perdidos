package br.udesc.achados.ocorrencia.service;

import br.udesc.achados.ocorrencia.domain.Ocorrencia;
import br.udesc.achados.ocorrencia.domain.OcorrenciaHistorico;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;
import br.udesc.achados.ocorrencia.exception.ForbiddenException;
import br.udesc.achados.ocorrencia.exception.NotFoundException;
import br.udesc.achados.ocorrencia.repository.OcorrenciaHistoricoRepository;
import br.udesc.achados.ocorrencia.repository.OcorrenciaRepository;
import br.udesc.achados.ocorrencia.web.CurrentUser;
import br.udesc.achados.ocorrencia.web.dto.OcorrenciaHistoricoResponse;
import br.udesc.achados.ocorrencia.web.dto.OcorrenciaResponse;
import br.udesc.achados.ocorrencia.web.dto.PageResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OcorrenciaQueryService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaHistoricoRepository historicoRepository;

    public OcorrenciaQueryService(
            OcorrenciaRepository ocorrenciaRepository,
            OcorrenciaHistoricoRepository historicoRepository
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.historicoRepository = historicoRepository;
    }

    public OcorrenciaResponse buscarPorId(UUID id, CurrentUser user) {
        Ocorrencia ocorrencia = buscarOuFalhar(id);
        garantirAcesso(ocorrencia, user);
        return OcorrenciaResponse.fromEntity(ocorrencia);
    }

    public PageResponse<OcorrenciaResponse> listarMeus(CurrentUser user, StatusOcorrencia status, Pageable pageable) {
        Page<Ocorrencia> page;
        if (status != null) {
            page = ocorrenciaRepository.findByUsuarioIdAndStatus(user.id(), status, pageable);
        } else {
            page = ocorrenciaRepository.findByUsuarioId(user.id(), pageable);
        }
        return PageResponse.from(page.map(OcorrenciaResponse::fromEntity));
    }

    public PageResponse<OcorrenciaResponse> listarAdmin(CurrentUser user, StatusOcorrencia status, UUID itemId, Pageable pageable) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Acesso restrito a administradores");
        }

        Specification<Ocorrencia> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (itemId != null) {
                predicates.add(cb.equal(root.get("itemId"), itemId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ocorrencia> page = ocorrenciaRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(OcorrenciaResponse::fromEntity));
    }

    public List<OcorrenciaHistoricoResponse> buscarHistorico(UUID id, CurrentUser user) {
        Ocorrencia ocorrencia = buscarOuFalhar(id);
        garantirAcesso(ocorrencia, user);

        List<OcorrenciaHistorico> historico = historicoRepository.findByOcorrenciaIdOrderByCriadoEmAsc(id);
        return historico.stream()
                .map(OcorrenciaHistoricoResponse::fromEntity)
                .toList();
    }

    private void garantirAcesso(Ocorrencia ocorrencia, CurrentUser user) {
        if (!user.isAdmin() && !ocorrencia.getUsuarioId().equals(user.id())) {
            throw new ForbiddenException("Você não tem permissão para visualizar esta ocorrência");
        }
    }

    private Ocorrencia buscarOuFalhar(UUID id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ocorrência não encontrada com o id " + id));
    }
}
