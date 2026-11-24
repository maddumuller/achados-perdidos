package br.edu.udesc.item.service;

import br.edu.udesc.item.model.StatusItem;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "item.reservations.enabled", havingValue = "true")
public class ItemReservationEvents {
    private static final String EXCHANGE = "achados.events";
    private static final String QUEUE = "item-service.ocorrencias";

    private final ItemReservationService reservations;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    public ItemReservationEvents(ItemReservationService reservations, RabbitTemplate rabbit, ObjectMapper json) {
        this.reservations = reservations;
        this.rabbit = rabbit;
        this.json = json;
    }

    @Bean
    Declarables itemReservationBindings() {
        var exchange = new TopicExchange(EXCHANGE);
        var queue = QueueBuilder.durable(QUEUE).withArgument("x-single-active-consumer", true).build();
        return new Declarables(exchange, queue, BindingBuilder.bind(queue).to(exchange).with("ocorrencia.*"));
    }

    @RabbitListener(queues = QUEUE)
    public void handle(Message message) {
        var event = json.readValue(message.getBody(), OccurrenceEvent.class);
        long id = ItemReference.numericId(event.itemId().toString());
        switch (message.getMessageProperties().getReceivedRoutingKey()) {
            case "ocorrencia.reivindicada" -> sendResult(event, reservations.reserve(id, event.ocorrenciaId()));
            case "ocorrencia.cancelada" -> reservations.finish(id, event.ocorrenciaId(), StatusItem.DISPONIVEL);
            case "ocorrencia.devolucao-confirmada" -> reservations.finish(id, event.ocorrenciaId(), StatusItem.DEVOLVIDO);
            default -> { }
        }
    }

    private void sendResult(OccurrenceEvent event, boolean reserved) {
        var payload = new java.util.HashMap<String, Object>(Map.of(
                "itemId", event.itemId(),
                "ocorrenciaId", event.ocorrenciaId(),
                "occurredAt", Instant.now().toString()));
        if (!reserved) {
            payload.put("motivo", "Item indisponível para reserva");
        }
        var message = MessageBuilder.withBody(json.writeValueAsBytes(payload))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbit.send(EXCHANGE, reserved ? "item.reservado" : "item.reserva-negada", message);
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record OccurrenceEvent(UUID itemId, UUID ocorrenciaId) {}
}
