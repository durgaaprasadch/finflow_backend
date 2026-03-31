package com.finflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseData {

    private String accessToken;
    private boolean mfaRequired;

    public LoginResponseData(String accessToken) {
        this.accessToken = accessToken;
    }
}
