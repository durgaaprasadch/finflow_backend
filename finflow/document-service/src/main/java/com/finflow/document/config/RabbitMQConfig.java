package com.finflow.document.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
@SuppressWarnings("null")
public class RabbitMQConfig {

    public static final String UPLOADED_QUEUE = "document_uploaded_queue";

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key.uploaded}")
    private String uploadedRoutingKey;

    @Bean
    public Queue uploadedQueue() {
        return new Queue(UPLOADED_QUEUE, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding binding(Queue uploadedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(uploadedQueue).to(exchange).with(uploadedRoutingKey);
    }

    @Bean
    @Lazy
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        classMapper.setTrustedPackages("*");
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("DocumentMessage", com.finflow.document.dto.DocumentMessage.class);
        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        return converter;
    }
}
