package com.finflow.auth.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("null")
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter converter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*");
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("NotificationRequest", com.finflow.notification.dto.NotificationRequest.class);
        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    @org.springframework.context.annotation.Lazy
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }
}
