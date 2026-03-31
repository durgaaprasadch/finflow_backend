package com.finflow.document.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    @org.springframework.context.annotation.Lazy
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.requestFactory(HttpComponentsClientHttpRequestFactory.class).build();
    }
}
