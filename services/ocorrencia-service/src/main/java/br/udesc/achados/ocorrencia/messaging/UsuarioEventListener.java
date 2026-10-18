package br.udesc.achados.ocorrencia.messaging;

import br.udesc.achados.ocorrencia.config.RabbitConfig;
import br.udesc.achados.ocorrencia.messaging.events.UsuarioDesativadoMessage;
import br.udesc.achados.ocorrencia.service.OcorrenciaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UsuarioEventListener {

    private static final Logger log = LoggerFactory.getLogger(UsuarioEventListener.class);

    private final OcorrenciaService ocorrenciaService;

    public UsuarioEventListener(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_USUARIO)
    public void onUsuarioDesativado(UsuarioDesativadoMessage message) {
        log.info("Recebido evento usuario.desativado para o usuário {}", message.usuarioId());
        try {
            ocorrenciaService.processarUsuarioDesativado(message);
        } catch (Exception e) {
            log.error("Erro ao processar usuario.desativado para {}: {}", message.usuarioId(), e.getMessage());
            throw e;
        }
    }
}
