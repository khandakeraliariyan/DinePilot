package com.dinepilot.restaurant.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {
    @Bean TopicExchange dinepilotExchange() { return new TopicExchange("dinepilot.events"); }
    @Bean Queue reservationCreatedQueue() { return new Queue("reservation.created.queue", true); }
    @Bean Queue reservationStatusChangedQueue() { return new Queue("reservation.status.changed.queue", true); }
    @Bean Binding reservationCreatedBinding(TopicExchange dinepilotExchange, Queue reservationCreatedQueue) {
        return BindingBuilder.bind(reservationCreatedQueue).to(dinepilotExchange).with("reservation.created");
    }
    @Bean Binding reservationStatusChangedBinding(TopicExchange dinepilotExchange, Queue reservationStatusChangedQueue) {
        return BindingBuilder.bind(reservationStatusChangedQueue).to(dinepilotExchange).with("reservation.status.changed");
    }
}
