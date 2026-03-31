package com.finflow.admin.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AdminConfig {

    @Bean
    public CommandLineRunner tracingTest() {
        return args -> log.info("=== Admin Service Observability Diagnostics Active ===");
    }
}
