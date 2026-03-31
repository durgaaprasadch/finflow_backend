package com.finflow.gateway.filter;

import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;





@Component
public class RouteValidator {

    private static final java.util.List<String> EXACT_PUBLIC_PATHS = java.util.List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/login/verify",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/reset-password",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/api/v1/auth/v3/api-docs",
            "/api/v1/applications/v3/api-docs",
            "/api/v1/admin/v3/api-docs",
            "/api/v1/documents/v3/api-docs",
            "/api/v1/auth/delete-request",
            "/api/v1/auth/delete-verify"
    );

    private static final java.util.List<String> PUBLIC_PATH_PREFIXES = java.util.List.of(
            "/eureka",
            "/api/v1/auth/signup/",
            "/v3/api-docs/",
            "/swagger-ui/",
            "/webjars/"
    );

    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return EXACT_PUBLIC_PATHS.stream().noneMatch(path::equals)
                && PUBLIC_PATH_PREFIXES.stream().noneMatch(path::startsWith);
    }
}
