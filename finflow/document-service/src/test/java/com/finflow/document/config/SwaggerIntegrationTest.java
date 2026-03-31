package com.finflow.document.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"
})
@AutoConfigureMockMvc
class SwaggerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerApiDocs_ShouldReturn200() throws Exception {
        // Tests that the OpenAPI JSON definition is generated and accessible
        mockMvc.perform(get("/api/v1/documents/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_ShouldReturn200() throws Exception {
        // Tests that the Swagger UI HTML page is accessible
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
