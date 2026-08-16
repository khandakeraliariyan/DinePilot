package com.dinepilot.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMessageConfigTest {

    @Test
    void exposesJsonMessageConverter() {
        RabbitMessageConfig config = new RabbitMessageConfig();

        MessageConverter converter = config.jsonMessageConverter();

        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
