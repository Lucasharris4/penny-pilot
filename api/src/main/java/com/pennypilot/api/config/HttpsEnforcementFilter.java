package com.pennypilot.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.security.require-https", havingValue = "true")
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isHealthCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!request.isSecure()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"HTTPS is required. Configure a reverse proxy with TLS termination.\"}");
            return;
        }

        // Add HSTS header — tell browsers to always use HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        filterChain.doFilter(request, response);
    }

    private boolean isHealthCheck(HttpServletRequest request) {
        return "/actuator/health".equals(request.getRequestURI());
    }
}
