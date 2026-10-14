package br.udesc.achados.ocorrencia.web;

import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;
import br.udesc.achados.ocorrencia.service.OcorrenciaQueryService;
import br.udesc.achados.ocorrencia.service.OcorrenciaService;
import br.udesc.achados.ocorrencia.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;
    private final OcorrenciaQueryService queryService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService, OcorrenciaQueryService queryService) {
        this.ocorrenciaService = ocorrenciaService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OcorrenciaResponse criar(
            @Valid @RequestBody CreateOcorrenciaRequest request,
            CurrentUser user,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ocorrenciaService.criar(request, user, correlationId);
    }

    @GetMapping("/meus")
    public PageResponse<OcorrenciaResponse> listarMeus(
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CurrentUser user
    ) {
        int limitSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitSize, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return queryService.listarMeus(user, status, pageable);
    }

    @GetMapping
    public PageResponse<OcorrenciaResponse> listarAdmin(
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CurrentUser user
    ) {
        int limitSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitSize, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return queryService.listarAdmin(user, status, itemId, pageable);
    }

    @GetMapping("/{id}")
    public OcorrenciaResponse buscarPorId(
            @PathVariable UUID id,
            CurrentUser user
    ) {
        return queryService.buscarPorId(id, user);
    }

    @GetMapping("/{id}/historico")
    public List<OcorrenciaHistoricoResponse> buscarHistorico(
            @PathVariable UUID id,
            CurrentUser user
    ) {
        return queryService.buscarHistorico(id, user);
    }

    @PostMapping("/{id}/aprovar")
    public OcorrenciaResponse aprovar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovarRequest request,
            CurrentUser user
    ) {
        return ocorrenciaService.aprovar(id, request, user);
    }

    @PostMapping("/{id}/rejeitar")
    public OcorrenciaResponse rejeitar(
            @PathVariable UUID id,
            @Valid @RequestBody RejeitarRequest request,
            CurrentUser user
    ) {
        return ocorrenciaService.rejeitar(id, request, user);
    }

    @PostMapping("/{id}/devolucao")
    public OcorrenciaResponse devolucao(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) DevolucaoRequest request,
            CurrentUser user
    ) {
        return ocorrenciaService.devolucao(id, request, user);
    }

    @PostMapping("/{id}/cancelar")
    public OcorrenciaResponse cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelarRequest request,
            CurrentUser user
    ) {
        return ocorrenciaService.cancelar(id, request, user);
    }
}
