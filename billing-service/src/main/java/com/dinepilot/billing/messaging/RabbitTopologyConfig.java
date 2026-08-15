package com.dinepilot.billing.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {
    @Bean TopicExchange dinepilotExchange() { return new TopicExchange("dinepilot.events"); }
    @Bean Queue orderStatusChangedQueue() { return new Queue("order.status.changed.queue", true); }
    @Bean Queue paymentCompletedQueue() { return new Queue("payment.completed.queue", true); }
    @Bean Binding orderStatusChangedBinding(TopicExchange dinepilotExchange, Queue orderStatusChangedQueue) {
        return BindingBuilder.bind(orderStatusChangedQueue).to(dinepilotExchange).with("order.status.changed");
    }
    @Bean Binding paymentCompletedBinding(TopicExchange dinepilotExchange, Queue paymentCompletedQueue) {
        return BindingBuilder.bind(paymentCompletedQueue).to(dinepilotExchange).with("payment.completed");
    }
}
