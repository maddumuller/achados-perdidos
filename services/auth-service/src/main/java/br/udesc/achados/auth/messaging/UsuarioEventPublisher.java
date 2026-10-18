package br.udesc.achados.auth.messaging;

import br.udesc.achados.auth.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UsuarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UsuarioEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UsuarioEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUsuarioDesativado(UsuarioDesativadoEvent event) {
        try {
            log.info("Publicando evento usuario.desativado para o usuário {}", event.usuarioId());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "usuario.desativado", event);
        } catch (Exception e) {
            log.error("Falha ao publicar evento usuario.desativado para {}: {}", event.usuarioId(), e.getMessage());
        }
    }
}
