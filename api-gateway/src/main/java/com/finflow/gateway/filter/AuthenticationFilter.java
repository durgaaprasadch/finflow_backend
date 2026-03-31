package com.finflow.gateway.filter;

import com.finflow.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    private static final String LOGIN_USER_HEADER = "loggedInUser";
    private static final String USER_ROLE_HEADER = "userRole";
    private static final String APPLICANT_ID_HEADER = "applicantId";

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublicEndpoint(path) || isOptionsRequest(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            if (!validator.isSecured(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isEmpty()) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            return handleSecuredRequest(exchange, chain, path, authHeader);
        };
    }

    private Mono<Void> handleSecuredRequest(ServerWebExchange exchange, GatewayFilterChain chain, String path, String authHeader) {
        String token = extractToken(authHeader);
        try {
            jwtUtil.validateToken(token);
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            String userId = jwtUtil.extractUserId(token);

            if (isAdminRestricted(path, role)) {
                return onError(exchange, "Forbidden: Admin access only", HttpStatus.FORBIDDEN);
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(LOGIN_USER_HEADER, username)
                    .header(USER_ROLE_HEADER, role)
                    .header(APPLICANT_ID_HEADER, userId == null ? "" : userId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
        }
    }


    private boolean isOptionsRequest(ServerHttpRequest request) {
        return request.getMethod() == org.springframework.http.HttpMethod.OPTIONS;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }

    private boolean isAdminRestricted(String path, String role) {
        return path.startsWith("/api/v1/admin")
                && !"ADMIN".equalsIgnoreCase(role);
    }



    // [METHOD] PUBLIC ENDPOINT CHECK
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth/signup") ||
               path.startsWith("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/login/verify") ||
               path.equals("/api/v1/auth/forgot-password") ||
               path.equals("/api/v1/auth/verify-otp") ||
               path.equals("/api/v1/auth/reset-password") ||
               path.equals("/api/v1/auth/delete-request") ||
               path.equals("/api/v1/auth/delete-verify") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/webjars/") ||
               path.equals("/v3/api-docs") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/api/v1/auth/v3/api-docs") ||
               path.startsWith("/api/v1/auth/v3/api-docs/") ||
               path.equals("/api/v1/applications/v3/api-docs") ||
               path.startsWith("/api/v1/applications/v3/api-docs/") ||
               path.equals("/api/v1/admin/v3/api-docs") ||
               path.startsWith("/api/v1/admin/v3/api-docs/") ||
               path.equals("/api/v1/documents/v3/api-docs") ||
               path.startsWith("/api/v1/documents/v3/api-docs/");
    }

    // [ERROR] ERROR HANDLER
    @SuppressWarnings("null")
    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus status) {
        org.springframework.http.server.reactive.ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String path = exchange.getRequest().getURI().getPath();
        String jsonError = String.format("{%n  \"timestamp\": \"%s\",%n  \"status\": %d,%n  \"error\": \"%s\",%n  \"message\": \"%s\",%n  \"path\": \"%s\"%n}",
                java.time.Instant.now().toString(), status.value(), status.getReasonPhrase(), errMessage, path);

        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(jsonError.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Configuration interface for AuthenticationFilter.
     */
    public static class Config {
        private String name = "default";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
