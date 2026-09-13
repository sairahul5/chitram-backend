package com.chitram.security;

import com.chitram.shared.service.PlatformSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DynamicSessionTimeoutFilter extends OncePerRequestFilter {

    private final PlatformSettingsService platformSettingsService;

    public DynamicSessionTimeoutFilter(PlatformSettingsService platformSettingsService) {
        this.platformSettingsService = platformSettingsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setMaxInactiveInterval(platformSettingsService.getSessionDurationDays() * 24 * 60 * 60);
        }
        filterChain.doFilter(request, response);
    }
}