package com.finflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    public static final String TOKEN_TYPE_BEARER = "Bearer";
    private String accessToken;
    @Builder.Default
    private String tokenType = TOKEN_TYPE_BEARER;
    private long expiresIn;
    private String role;
    private boolean mfaRequired;
}
