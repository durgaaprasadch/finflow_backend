package com.finflow.auth.config;

import com.finflow.auth.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        role == null ? List.of() : List.of(new SimpleGrantedAuthority(role))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/signup") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/refresh") ||
               path.startsWith("/auth/internal/admin/") ||
               path.startsWith("/api/v1/auth/signup") ||
               path.startsWith("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/delete-request") ||
               path.equals("/api/v1/auth/delete-verify") ||
               path.equals("/api/v1/auth/v3/api-docs") ||
               path.startsWith("/api/v1/auth/v3/api-docs/") ||
               path.equals("/v3/api-docs") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-ui/");
    }
}
