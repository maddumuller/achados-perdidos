package br.udesc.achados.ocorrencia.service;

import br.udesc.achados.ocorrencia.config.OcorrenciaProperties;
import br.udesc.achados.ocorrencia.domain.Ocorrencia;
import br.udesc.achados.ocorrencia.domain.StatusOcorrencia;
import br.udesc.achados.ocorrencia.repository.OcorrenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ReservaTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(ReservaTimeoutJob.class);

    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaService ocorrenciaService;
    private final OcorrenciaProperties properties;

    public ReservaTimeoutJob(
            OcorrenciaRepository ocorrenciaRepository,
            OcorrenciaService ocorrenciaService,
            OcorrenciaProperties properties
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.ocorrenciaService = ocorrenciaService;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 60000)
    public void verificarTimeouts() {
        int timeoutMinutes = properties.reservaTimeoutMinutes() > 0 ? properties.reservaTimeoutMinutes() : 5;
        Instant limite = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));

        List<Ocorrencia> expiradas = ocorrenciaRepository.findByStatusAndCriadoEmBefore(
                StatusOcorrencia.AGUARDANDO_RESERVA,
                limite
        );

        if (!expiradas.isEmpty()) {
            log.info("Encontradas {} ocorrências expiradas em AGUARDANDO_RESERVA para cancelamento por timeout", expiradas.size());
        }

        for (Ocorrencia ocorrencia : expiradas) {
            try {
                ocorrenciaService.cancelarPorTimeout(ocorrencia);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Ocorrência {} foi modificada simultaneamente por outro processo; será reavaliada na próxima execução", ocorrencia.getId());
            } catch (Exception e) {
                log.error("Erro ao cancelar ocorrência {} por timeout: {}", ocorrencia.getId(), e.getMessage());
            }
        }
    }
}
