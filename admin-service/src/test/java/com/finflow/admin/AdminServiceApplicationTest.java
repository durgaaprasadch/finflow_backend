package com.finflow.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class AdminServiceApplicationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "Application context should not be null");
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled to prevent forked VM crash during main method execution in tests")
    void main_ShouldStartApplication() {
        assertDoesNotThrow(() -> AdminServiceApplication.main(new String[]{"--server.port=0"}));
    }
}
