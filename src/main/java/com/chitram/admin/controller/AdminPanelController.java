package com.chitram.admin.controller;

import com.chitram.admin.dto.AdminPanelResponse;
import com.chitram.admin.dto.AdminDashboardResponse;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.RoleUpdateRequest;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.service.AdminPanelService;
import com.chitram.admin.service.AdminAuthorizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPanelController {

    private final AdminPanelService adminPanelService;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminPanelController(AdminPanelService adminPanelService,
            AdminAuthorizationService adminAuthorizationService) {
        this.adminPanelService = adminPanelService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @GetMapping("/panel")
    public AdminPanelResponse getPanel(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getPanel();
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getDashboard();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getUsers();
    }

    @PutMapping("/users/{userId}/role")
    public void updateUserRole(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable long userId,
            @RequestBody RoleUpdateRequest request) {
        requireAdmin(user);
        adminPanelService.updateUserRole(userId, request.role());
    }

    @GetMapping("/visual-items")
    public List<VisualItemResponse> getVisualItems(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(required = false) String query) {
        requireAdmin(user);
        return adminPanelService.getVisualItems(query);
    }

    private void requireAdmin(OAuth2User user) {
        adminAuthorizationService.requireAdmin(user.getAttribute("email"));
    }
}
