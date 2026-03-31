package com.finflow.document.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Service
public class ApplicationServiceClient {

    private final RestTemplate restTemplate;

    public ApplicationServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String APP_SERVICE_URL = "http://application-service/api/v1/applications/status";

    public Long getActiveApplicationId(String username, String role, String applicantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("loggedInUser", username);
        headers.set("userRole", role);
        headers.set("applicantId", applicantId);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            @SuppressWarnings({ "unchecked", "null" })
            Map<String, Object> response = restTemplate.exchange(APP_SERVICE_URL, HttpMethod.GET, entity, Map.class)
                    .getBody();
            if (response != null && response.get("data") instanceof Map<?, ?> dataMap) {
                return Long.valueOf(dataMap.get("applicationId").toString());
            }
        } catch (Exception e) {
            // Log error but don't fail yet, let service layer handle it
        }
        return null; // Return null instead of throwing to let service handle decision
    }
}
