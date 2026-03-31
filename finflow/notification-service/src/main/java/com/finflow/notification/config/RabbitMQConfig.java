package com.finflow.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.finflow.notification.dto.NotificationRequest;

@Configuration
@SuppressWarnings("null")
public class RabbitMQConfig {

    @Value("${notification.queues.registration}")
    private String registrationQueue;

    @Value("${notification.queues.loan-status}")
    private String loanStatusQueue;

    @Value("${notification.queues.login}")
    private String loginQueue;

    @Value("${notification.exchanges.notification}")
    private String notificationExchange;

    @Value("${notification.routing-keys.registration}")
    private String registrationRoutingKey;

    @Value("${notification.routing-keys.loan-status}")
    private String loanStatusRoutingKey;

    @Value("${notification.routing-keys.login}")
    private String loginRoutingKey;

    @Bean
    public Queue registrationQueue() {
        return new Queue(registrationQueue);
    }

    @Bean
    public Queue loanStatusQueue() {
        return new Queue(loanStatusQueue);
    }

    @Bean
    public Queue loginQueue() {
        return new Queue(loginQueue);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchange);
    }

    @Bean
    public Binding registrationBinding(Queue registrationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(registrationQueue).to(notificationExchange).with(registrationRoutingKey);
    }

    @Bean
    public Binding loanStatusBinding(Queue loanStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(loanStatusQueue).to(notificationExchange).with(loanStatusRoutingKey);
    }

    @Bean
    public Binding loginBinding(Queue loginQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(loginQueue).to(notificationExchange).with(loginRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
         Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
         DefaultClassMapper classMapper = new DefaultClassMapper();
         classMapper.setTrustedPackages("*");
         java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
         
         // Logical Mapping
         idClassMapping.put("NotificationRequest", NotificationRequest.class);
         
         // Direct FQCN Mapping from other services (Fallback)
         idClassMapping.put("com.finflow.auth.dto.NotificationRequest", NotificationRequest.class);
         idClassMapping.put("com.finflow.application.dto.NotificationRequest", NotificationRequest.class);
         
         classMapper.setIdClassMapping(idClassMapping);
         converter.setClassMapper(classMapper);
         return converter;
    }

    @Bean
    @org.springframework.context.annotation.Lazy
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    @org.springframework.context.annotation.Lazy
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setObservationEnabled(true);
        return factory;
    }
}
