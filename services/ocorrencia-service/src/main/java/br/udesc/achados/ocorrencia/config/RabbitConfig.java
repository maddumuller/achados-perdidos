package br.udesc.achados.ocorrencia.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "achados.events";

    public static final String QUEUE_ITEM = "ocorrencia-service.item";
    public static final String QUEUE_USUARIO = "ocorrencia-service.usuario";

    @Bean
    public TopicExchange achadosEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue itemQueue() {
        return QueueBuilder.durable(QUEUE_ITEM).build();
    }

    @Bean
    public Queue usuarioQueue() {
        return QueueBuilder.durable(QUEUE_USUARIO).build();
    }

    @Bean
    public Binding itemReservadoBinding(Queue itemQueue, TopicExchange achadosEventsExchange) {
        return BindingBuilder.bind(itemQueue).to(achadosEventsExchange).with("item.reservado");
    }

    @Bean
    public Binding itemReservaNegadaBinding(Queue itemQueue, TopicExchange achadosEventsExchange) {
        return BindingBuilder.bind(itemQueue).to(achadosEventsExchange).with("item.reserva-negada");
    }

    @Bean
    public Binding usuarioDesativadoBinding(Queue usuarioQueue, TopicExchange achadosEventsExchange) {
        return BindingBuilder.bind(usuarioQueue).to(achadosEventsExchange).with("usuario.desativado");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
