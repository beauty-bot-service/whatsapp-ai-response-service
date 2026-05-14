package com.beautybot.whatsappairesponseservice.config.security;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PREFIXES = List.of(
            "/chat/test",
            "/leads",
            "/conversations",
            "/whatsapp/test",
            "/actuator"
    );

    private final BeautyBotProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PROTECTED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        BeautyBotProperties.Security security = properties.getSecurity();
        if (security == null || !security.isInternalApiKeyEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String expectedApiKey = security.getInternalApiKey();
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "internal_api_key_not_configured");
            return;
        }

        String headerName = security.getInternalApiKeyHeader();
        if (headerName == null || headerName.isBlank()) {
            headerName = "Authorization";
        }

        String receivedApiKey = request.getHeader(headerName);
        if (!expectedApiKey.equals(receivedApiKey)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "invalid_internal_api_key");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
