package com.finflow.auth.dto;

import java.util.UUID;

public class SignupResponseData {

    private UUID userId;
    private String status;

    public SignupResponseData() {
    }

    public SignupResponseData(UUID userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
