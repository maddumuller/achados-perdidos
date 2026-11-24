package br.udesc.achados.ocorrencia.messaging;

import br.udesc.achados.ocorrencia.config.RabbitConfig;
import br.udesc.achados.ocorrencia.messaging.events.ItemReservaNegadaMessage;
import br.udesc.achados.ocorrencia.messaging.events.ItemReservadoMessage;
import br.udesc.achados.ocorrencia.service.OcorrenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ItemEventListener {

    private static final Logger log = LoggerFactory.getLogger(ItemEventListener.class);

    private final OcorrenciaService ocorrenciaService;
    private final ObjectMapper objectMapper;

    public ItemEventListener(OcorrenciaService ocorrenciaService, ObjectMapper objectMapper) {
        this.ocorrenciaService = ocorrenciaService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ITEM)
    public void onMessage(Message message) throws IOException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        switch (routingKey) {
            case "item.reservado" -> ocorrenciaService.processarItemReservado(
                    objectMapper.readValue(message.getBody(), ItemReservadoMessage.class));
            case "item.reserva-negada" -> ocorrenciaService.processarItemReservaNegada(
                    objectMapper.readValue(message.getBody(), ItemReservaNegadaMessage.class));
            default -> log.warn("Chave de roteamento inesperada na fila {}: {}", RabbitConfig.QUEUE_ITEM, routingKey);
        }
    }
}
