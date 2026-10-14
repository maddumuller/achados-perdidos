package br.udesc.achados.ocorrencia.messaging;

import br.udesc.achados.ocorrencia.config.RabbitConfig;
import br.udesc.achados.ocorrencia.messaging.events.OcorrenciaCanceladaEvent;
import br.udesc.achados.ocorrencia.messaging.events.OcorrenciaDevolucaoConfirmadaEvent;
import br.udesc.achados.ocorrencia.messaging.events.OcorrenciaReivindicadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OcorrenciaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OcorrenciaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OcorrenciaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOcorrenciaReivindicada(OcorrenciaReivindicadaEvent event) {
        try {
            log.info("Publicando evento ocorrencia.reivindicada para ocorrencia {} item {}",
                    event.ocorrenciaId(), event.itemId());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "ocorrencia.reivindicada", event);
        } catch (Exception e) {
            log.error("Falha ao publicar ocorrencia.reivindicada {}: {}", event.ocorrenciaId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOcorrenciaDevolucaoConfirmada(OcorrenciaDevolucaoConfirmadaEvent event) {
        try {
            log.info("Publicando evento ocorrencia.devolucao-confirmada para ocorrencia {} item {}",
                    event.ocorrenciaId(), event.itemId());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "ocorrencia.devolucao-confirmada", event);
        } catch (Exception e) {
            log.error("Falha ao publicar ocorrencia.devolucao-confirmada {}: {}", event.ocorrenciaId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOcorrenciaCancelada(OcorrenciaCanceladaEvent event) {
        try {
            log.info("Publicando evento ocorrencia.cancelada para ocorrencia {} item {} (motivo: {})",
                    event.ocorrenciaId(), event.itemId(), event.motivo());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "ocorrencia.cancelada", event);
        } catch (Exception e) {
            log.error("Falha ao publicar ocorrencia.cancelada {}: {}", event.ocorrenciaId(), e.getMessage());
        }
    }

    public void publishDirectly(Object event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, routingKey, event);
        } catch (Exception e) {
            log.error("Falha ao publicar diretamente {}: {}", routingKey, e.getMessage());
        }
    }
}
