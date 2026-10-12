package br.udesc.achados.ocorrencia.service;

import br.udesc.achados.ocorrencia.client.ItemClient;
import br.udesc.achados.ocorrencia.client.ItemResumo;
import br.udesc.achados.ocorrencia.domain.Ocorrencia;
import br.udesc.achados.ocorrencia.domain.OcorrenciaHistorico;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;
import br.udesc.achados.ocorrencia.exception.ConflictException;
import br.udesc.achados.ocorrencia.exception.ForbiddenException;
import br.udesc.achados.ocorrencia.exception.NotFoundException;
import br.udesc.achados.ocorrencia.messaging.events.*;
import br.udesc.achados.ocorrencia.repository.OcorrenciaHistoricoRepository;
import br.udesc.achados.ocorrencia.repository.OcorrenciaRepository;
import br.udesc.achados.ocorrencia.web.CurrentUser;
import br.udesc.achados.ocorrencia.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OcorrenciaService {

    private static final Logger log = LoggerFactory.getLogger(OcorrenciaService.class);

    private static final List<StatusOcorrencia> STATUS_ATIVOS = List.of(
            StatusOcorrencia.AGUARDANDO_RESERVA,
            StatusOcorrencia.EM_ANALISE,
            StatusOcorrencia.APROVADA
    );

    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaHistoricoRepository historicoRepository;
    private final ItemClient itemClient;
    private final ApplicationEventPublisher eventPublisher;

    public OcorrenciaService(
            OcorrenciaRepository ocorrenciaRepository,
            OcorrenciaHistoricoRepository historicoRepository,
            ItemClient itemClient,
            ApplicationEventPublisher eventPublisher
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.historicoRepository = historicoRepository;
        this.itemClient = itemClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OcorrenciaResponse criar(CreateOcorrenciaRequest request, CurrentUser user, String correlationId) {
        // RN-O1: Consultar item-service
        ItemResumo item = itemClient.buscar(request.itemId(), user, correlationId);

        if (!item.isAberto()) {
            throw new ConflictException("Item não está disponível para reivindicação (status atual: " + item.status() + ")");
        }

        if (item.usuarioId() != null && item.usuarioId().equalsIgnoreCase(user.id().toString())) {
            throw new ConflictException("Você não pode reivindicar um item que você mesmo cadastrou");
        }

        // RN-O2: Uma ocorrência ativa por usuário e item
        if (ocorrenciaRepository.existsByUsuarioIdAndItemIdAndStatusIn(user.id(), request.itemId(), STATUS_ATIVOS)) {
            throw new ConflictException("Você já possui uma ocorrência ativa para este item");
        }

        Ocorrencia ocorrencia = new Ocorrencia(
                request.itemId(),
                item.getTituloEfetivo(),
                item.getTipoEfetivo(),
                user.id(),
                request.justificativa()
        );

        Ocorrencia salva = ocorrenciaRepository.save(ocorrencia);

        // Histórico inicial
        historicoRepository.save(new OcorrenciaHistorico(
                salva,
                null,
                StatusOcorrencia.AGUARDANDO_RESERVA,
                "Abertura de ocorrência",
                user.id().toString()
        ));

        // Publica evento de início da Saga
        eventPublisher.publishEvent(new OcorrenciaReivindicadaEvent(
                salva.getId(),
                salva.getItemId(),
                salva.getUsuarioId()
        ));

        log.info("Ocorrência {} criada para o item {} pelo usuário {}", salva.getId(), salva.getItemId(), salva.getUsuarioId());
        return OcorrenciaResponse.fromEntity(salva);
    }

    @Transactional
    public OcorrenciaResponse aprovar(UUID id, AprovarRequest request, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Apenas administradores podem aprovar ocorrências");
        }

        Ocorrencia ocorrencia = buscarOuFalhar(id);
        ocorrencia.setAnalisadoPor(user.id());
        if (request != null && request.observacao() != null && !request.observacao().isBlank()) {
            ocorrencia.setObservacaoAdmin(request.observacao());
        }

        String motivo = (request != null && request.observacao() != null && !request.observacao().isBlank())
                ? request.observacao()
                : "Aprovada pelo administrador";

        mudarStatus(ocorrencia, StatusOcorrencia.APROVADA, motivo, user.id().toString(), true);
        return OcorrenciaResponse.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponse rejeitar(UUID id, RejeitarRequest request, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Apenas administradores podem rejeitar ocorrências");
        }

        Ocorrencia ocorrencia = buscarOuFalhar(id);
        ocorrencia.setAnalisadoPor(user.id());

        mudarStatus(ocorrencia, StatusOcorrencia.REJEITADA, request.motivo(), user.id().toString(), true);
        return OcorrenciaResponse.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponse devolucao(UUID id, DevolucaoRequest request, CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Apenas administradores podem confirmar a devolução");
        }

        Ocorrencia ocorrencia = buscarOuFalhar(id);
        ocorrencia.setAnalisadoPor(user.id());
        if (request != null && request.observacao() != null && !request.observacao().isBlank()) {
            ocorrencia.setObservacaoAdmin(request.observacao());
        }

        String motivo = (request != null && request.observacao() != null && !request.observacao().isBlank())
                ? request.observacao()
                : "Devolução confirmada";

        mudarStatus(ocorrencia, StatusOcorrencia.DEVOLVIDA, motivo, user.id().toString(), true);
        return OcorrenciaResponse.fromEntity(ocorrencia);
    }

    @Transactional
    public OcorrenciaResponse cancelar(UUID id, CancelarRequest request, CurrentUser user) {
        Ocorrencia ocorrencia = buscarOuFalhar(id);

        boolean isOwner = ocorrencia.getUsuarioId().equals(user.id());
        if (!isOwner && !user.isAdmin()) {
            throw new ForbiddenException("Você não tem permissão para cancelar esta ocorrência");
        }

        String motivo = (request != null && request.motivo() != null && !request.motivo().isBlank())
                ? request.motivo()
                : "Cancelada pelo " + (isOwner ? "usuário" : "administrador");

        mudarStatus(ocorrencia, StatusOcorrencia.CANCELADA, motivo, user.id().toString(), true);
        return OcorrenciaResponse.fromEntity(ocorrencia);
    }

    @Transactional
    public void processarItemReservado(ItemReservadoMessage msg) {
        if (msg.ocorrenciaId() == null) {
            log.warn("Mensagem item.reservado recebida sem ocorrenciaId");
            return;
        }

        Ocorrencia ocorrencia = ocorrenciaRepository.findById(msg.ocorrenciaId()).orElse(null);
        if (ocorrencia == null) {
            log.warn("Ocorrência {} não encontrada ao processar item.reservado", msg.ocorrenciaId());
            return;
        }

        if (ocorrencia.getStatus() == StatusOcorrencia.AGUARDANDO_RESERVA) {
            mudarStatus(ocorrencia, StatusOcorrencia.EM_ANALISE, "Item reservado com sucesso", "sistema", true);
            log.info("Ocorrência {} avançou para EM_ANALISE após confirmação da reserva do item {}", ocorrencia.getId(), msg.itemId());
        } else if (!ocorrencia.getStatus().ativa()) {
            // Reserva tardia: a ocorrência já foi finalizada/cancelada, compensa liberando o item
            log.warn("Reserva tardia recebida para ocorrência {} já em status {}. Enviando compensação...",
                    ocorrencia.getId(), ocorrencia.getStatus());
            eventPublisher.publishEvent(new OcorrenciaCanceladaEvent(
                    ocorrencia.getId(),
                    ocorrencia.getItemId(),
                    "Reserva tardia em ocorrência finalizada"
            ));
        } else {
            log.info("Ocorrência {} já no estado {}. Mensagem item.reservado ignorada (idempotência).",
                    ocorrencia.getId(), ocorrencia.getStatus());
        }
    }

    @Transactional
    public void processarItemReservaNegada(ItemReservaNegadaMessage msg) {
        if (msg.ocorrenciaId() == null) {
            log.warn("Mensagem item.reserva-negada recebida sem ocorrenciaId");
            return;
        }

        Ocorrencia ocorrencia = ocorrenciaRepository.findById(msg.ocorrenciaId()).orElse(null);
        if (ocorrencia == null) {
            log.warn("Ocorrência {} não encontrada ao processar item.reserva-negada", msg.ocorrenciaId());
            return;
        }

        if (ocorrencia.getStatus() == StatusOcorrencia.AGUARDANDO_RESERVA) {
            String motivo = (msg.motivo() != null && !msg.motivo().isBlank())
                    ? msg.motivo()
                    : "Reserva negada pelo item-service";
            // Não publica cancelamento porque o item não chegou a ser reservado
            mudarStatus(ocorrencia, StatusOcorrencia.CANCELADA, motivo, "sistema", false);
            log.info("Ocorrência {} cancelada devido a reserva negada: {}", ocorrencia.getId(), motivo);
        } else {
            log.info("Ocorrência {} já no estado {}. Mensagem item.reserva-negada ignorada.",
                    ocorrencia.getId(), ocorrencia.getStatus());
        }
    }

    @Transactional
    public void processarUsuarioDesativado(UsuarioDesativadoMessage msg) {
        if (msg.usuarioId() == null) {
            log.warn("Mensagem usuario.desativado recebida sem usuarioId");
            return;
        }

        List<Ocorrencia> ativas = ocorrenciaRepository.findByUsuarioIdAndStatusIn(msg.usuarioId(), STATUS_ATIVOS);
        log.info("Cancelando {} ocorrências ativas do usuário desativado {}", ativas.size(), msg.usuarioId());

        for (Ocorrencia ocorrencia : ativas) {
            mudarStatus(ocorrencia, StatusOcorrencia.CANCELADA, "Usuário desativado", "sistema", true);
        }
    }

    @Transactional
    public void cancelarPorTimeout(Ocorrencia ocorrencia) {
        if (ocorrencia.getStatus() == StatusOcorrencia.AGUARDANDO_RESERVA) {
            mudarStatus(ocorrencia, StatusOcorrencia.CANCELADA, "Sem resposta do item-service", "sistema", true);
            log.info("Ocorrência {} cancelada por timeout na reserva", ocorrencia.getId());
        }
    }

    private void mudarStatus(Ocorrencia ocorrencia, StatusOcorrencia destino, String motivo, String autor, boolean publicarEvento) {
        StatusOcorrencia origem = ocorrencia.getStatus();
        if (!origem.podeIrPara(destino)) {
            throw new ConflictException("Transição de status inválida: " + origem + " -> " + destino);
        }

        ocorrencia.setStatus(destino);
        if (destino == StatusOcorrencia.REJEITADA || destino == StatusOcorrencia.CANCELADA) {
            ocorrencia.setMotivoFinal(motivo);
        }

        ocorrenciaRepository.save(ocorrencia);

        // Histórico
        historicoRepository.save(new OcorrenciaHistorico(
                ocorrencia,
                origem,
                destino,
                motivo,
                autor
        ));

        // Publica eventos de negócio / Saga
        if (publicarEvento) {
            if (destino == StatusOcorrencia.DEVOLVIDA) {
                eventPublisher.publishEvent(new OcorrenciaDevolucaoConfirmadaEvent(
                        ocorrencia.getId(),
                        ocorrencia.getItemId()
                ));
            } else if (destino == StatusOcorrencia.CANCELADA || destino == StatusOcorrencia.REJEITADA) {
                eventPublisher.publishEvent(new OcorrenciaCanceladaEvent(
                        ocorrencia.getId(),
                        ocorrencia.getItemId(),
                        motivo
                ));
            }
        }
    }

    private Ocorrencia buscarOuFalhar(UUID id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ocorrência não encontrada com o id " + id));
    }
}
