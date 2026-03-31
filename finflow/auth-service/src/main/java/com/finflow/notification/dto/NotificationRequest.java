package com.finflow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> model;
}
