package br.udesc.achados.ocorrencia.messaging;

import br.udesc.achados.ocorrencia.config.RabbitConfig;
import br.udesc.achados.ocorrencia.messaging.events.ItemReservaNegadaMessage;
import br.udesc.achados.ocorrencia.messaging.events.ItemReservadoMessage;
import br.udesc.achados.ocorrencia.service.OcorrenciaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ItemEventListener {

    private static final Logger log = LoggerFactory.getLogger(ItemEventListener.class);

    private final OcorrenciaService ocorrenciaService;

    public ItemEventListener(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ITEM)
    public void onMessage(Object message) {
        // Mensagens recebidas da fila ocorrencia-service.item
        // O conversor JSON deserializa para ItemReservadoMessage ou ItemReservaNegadaMessage
        if (message instanceof ItemReservadoMessage reservado) {
            log.info("Recebido evento item.reservado para ocorrencia {} e item {}", reservado.ocorrenciaId(), reservado.itemId());
            ocorrenciaService.processarItemReservado(reservado);
        } else if (message instanceof ItemReservaNegadaMessage negada) {
            log.info("Recebido evento item.reserva-negada para ocorrencia {} e item {}: {}", negada.ocorrenciaId(), negada.itemId(), negada.motivo());
            ocorrenciaService.processarItemReservaNegada(negada);
        } else {
            log.warn("Mensagem inesperada na fila {}: {}", RabbitConfig.QUEUE_ITEM, message);
        }
    }
}
