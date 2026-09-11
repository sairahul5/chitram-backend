package com.chitram.security;

import com.chitram.admin.service.AdminPanelService;
import com.chitram.user.service.UserAccountService;
import com.chitram.websocket.AdminEventPublisher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAccountService userAccountService;
    private final AdminPanelService adminPanelService;
    private final AdminEventPublisher adminEventPublisher;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserAccountService userAccountService,
            AdminPanelService adminPanelService,
            AdminEventPublisher adminEventPublisher,
            @Value("${FRONTEND_URL:http://localhost:3000}") String frontendUrl) {
        this.userAccountService = userAccountService;
        this.adminPanelService = adminPanelService;
        this.adminEventPublisher = adminEventPublisher;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        userAccountService.upsertGoogleUser(oAuth2User);
        String email = oAuth2User.getAttribute("email");
        // Push live updates to admin panel
        try {
            adminEventPublisher.publishUsers(adminPanelService.getUsers());
            adminEventPublisher.publishDashboard(adminPanelService.getDashboard());
        } catch (Exception ignored) { /* don't break login if WS publish fails */ }
        String destination = userAccountService.isAdmin(email) ? "/admin" : "/user";
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + destination);
    }
}
