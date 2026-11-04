package com.kinetirx.common;

import com.kinetirx.config.KinetiRxProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Applies the same CORS rules and security headers as the Go middleware:
 * - CORS only for origins listed in ALLOWED_ORIGINS, with credentials; OPTIONS answers 204.
 * - Baseline security headers on every response (JSON API, so CSP is locked down).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HttpPolicyFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    public HttpPolicyFilter(KinetiRxProperties props) {
        this.allowedOrigins = Set.copyOf(props.allowedOrigins());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy", "default-src 'none'");
        response.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains");

        String origin = request.getHeader("Origin");
        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-Id");
        }
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(204);
            return;
        }
        chain.doFilter(request, response);
    }
}
