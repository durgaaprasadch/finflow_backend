package com.finflow.gateway.filter;

import com.finflow.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthenticationFilterTest {

        @Mock
        private RouteValidator validator;

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private GatewayFilterChain chain;

        @InjectMocks
        private AuthenticationFilter filter;

    @Test
    void apply_ShouldBypassPublicEndpoints() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                        .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

        @Test
        void apply_ShouldReturnUnauthorized_WhenMissingHeader() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications").build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(validator.isSecured(any())).thenReturn(true);

                StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                                .verifyComplete();

                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                verifyNoInteractions(chain);
        }

        @Test
        void apply_ShouldReturnUnauthorized_WhenTokenIsInvalid() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(validator.isSecured(any())).thenReturn(true);
                doThrow(new RuntimeException("Invalid token")).when(jwtUtil).validateToken("invalid-token");

                StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                                .verifyComplete();

                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                verifyNoInteractions(chain);
        }

        @Test
        void apply_ShouldReturnForbidden_WhenApplicantTriesToAccessAdminRoute() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/system")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(validator.isSecured(any())).thenReturn(true);
                when(jwtUtil.extractUsername("good-token")).thenReturn("testuser");
                when(jwtUtil.extractRole("good-token")).thenReturn("APPLICANT");

                StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                                .verifyComplete();

                assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
                verifyNoInteractions(chain);
        }

        @Test
        void apply_ShouldMutateRequestAndProceed_WhenTokenIsValid() {
                when(chain.filter(any())).thenReturn(Mono.empty());
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                when(validator.isSecured(any())).thenReturn(true);
                when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
                when(jwtUtil.extractRole("valid-token")).thenReturn("APPLICANT");

                StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                                .verifyComplete();

                verify(chain, times(1)).filter(any());
        }

    @Test
    void apply_ShouldBypassOptionsRequest() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.options("/api/v1/applications").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void apply_ShouldReturnUnauthorized_WhenInvalidHeaderFormat() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, "InvalidTokenFormat")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(validator.isSecured(any())).thenReturn(true);
        doThrow(new RuntimeException("invalid format")).when(jwtUtil).validateToken("InvalidTokenFormat");

        StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void apply_ShouldAllowAdminToAccessAdminRoute() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(validator.isSecured(any())).thenReturn(true);
        when(jwtUtil.extractUsername("admin-token")).thenReturn("admin");
        when(jwtUtil.extractRole("admin-token")).thenReturn("ADMIN");

        StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void apply_ShouldReturnUnauthorized_WhenTokenIsMalformed() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/loans")
                .header(HttpHeaders.AUTHORIZATION, "Bearer malformed.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(validator.isSecured(any())).thenReturn(true);
        doThrow(new RuntimeException("Invalid Token")).when(jwtUtil).validateToken(anyString());

        StepVerifier.create(filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void apply_ShouldAllowAccess_ForPublicEndpoints() {
        String[] publicPaths = {
            "/api/v1/auth/signup", "/api/v1/auth/login",
            "/api/v1/auth/v3/api-docs", "/api/v1/admin/v3/api-docs", "/swagger-ui/index.html",
            "/webjars/some-lib", "/v3/api-docs/some-module"
        };

        for (String path : publicPaths) {
            MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());
            
            filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain).block();
            
            verify(chain, atLeastOnce()).filter(exchange);
        }
    }

    @Test
    void apply_ShouldReturnUnauthorized_WhenAuthHeaderIsEmpty() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/loans")
                .header(HttpHeaders.AUTHORIZATION, "")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(validator.isSecured(any())).thenReturn(true);

        filter.apply(mock(AuthenticationFilter.Config.class)).filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
