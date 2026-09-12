package com.chitram.admin.controller;

import com.chitram.admin.dto.AdminPanelResponse;
import com.chitram.admin.dto.AdminDashboardResponse;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.RoleUpdateRequest;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.dto.AdminCategoryResponse;
import com.chitram.admin.dto.AdminReportResponse;
import com.chitram.admin.dto.AdminActivityResponse;
import com.chitram.admin.dto.DatabaseOverviewResponse;
import com.chitram.admin.service.AdminPanelService;
import com.chitram.admin.service.AdminAuthorizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public List<AdminUserResponse> getUsers(@AuthenticationPrincipal OAuth2User user,
            @RequestParam(required = false) String search) {
        requireAdmin(user);
        return adminPanelService.searchUsers(search);
    }

    @PutMapping("/users/{userId}/role")
    public void updateUserRole(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable long userId,
            @RequestBody RoleUpdateRequest request) {
        requireAdmin(user);
        adminPanelService.updateUserRole(userId, request.role());
    }

    @PutMapping("/users/{userId}/status")
    public void updateUserStatus(@AuthenticationPrincipal OAuth2User user, @PathVariable long userId,
            @RequestBody UserStatusRequest request) {
        requireAdmin(user);
        adminPanelService.setAccountStatus(userId, request.status());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/{userId}")
    public void deleteUser(@AuthenticationPrincipal OAuth2User user, @PathVariable long userId) {
        requireAdmin(user);
        adminPanelService.deleteUser(userId);
    }

    @GetMapping("/categories")
    public List<AdminCategoryResponse> getCategories(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getCategories();
    }

    @PostMapping("/categories")
    public AdminCategoryResponse createCategory(@AuthenticationPrincipal OAuth2User user,
            @RequestBody CategoryRequest request) {
        requireAdmin(user);
        adminPanelService.createCategory(request.name(), request.description());
        return adminPanelService.getCategories().stream()
                .filter(category -> category.name().equalsIgnoreCase(request.name().trim())).findFirst().orElseThrow();
    }

    @PutMapping("/categories/{categoryId}/status")
    public void updateCategoryStatus(@AuthenticationPrincipal OAuth2User user, @PathVariable long categoryId,
            @RequestBody EnabledRequest request) {
        requireAdmin(user);
        adminPanelService.setCategoryEnabled(categoryId, request.enabled());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/categories/{categoryId}")
    public void deleteCategory(@AuthenticationPrincipal OAuth2User user, @PathVariable long categoryId) {
        requireAdmin(user);
        adminPanelService.deleteCategory(categoryId);
    }

    @GetMapping("/reports")
    public List<AdminReportResponse> getReports(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        requireAdmin(user);
        if ((status != null && !status.isEmpty()) || (targetType != null && !targetType.isEmpty())) {
            return adminPanelService.getReportsFiltered(status, targetType);
        }
        return adminPanelService.getReports();
    }

    @PutMapping("/reports/{reportId}/status")
    public void updateReportStatus(@AuthenticationPrincipal OAuth2User user, @PathVariable long reportId,
            @RequestBody StatusRequest request) {
        requireAdmin(user);
        adminPanelService.setReportStatus(reportId, request.status(), user.getAttribute("email"));
    }

    @PutMapping("/visual-items/{pinId}/moderation")
    public void updateModerationStatus(@AuthenticationPrincipal OAuth2User user, @PathVariable long pinId,
            @RequestBody StatusRequest request) {
        requireAdmin(user);
        adminPanelService.setModerationStatus(pinId, request.status());
    }

    @GetMapping("/visual-items")
    public List<VisualItemResponse> getVisualItems(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(required = false) String query) {
        requireAdmin(user);
        return adminPanelService.getVisualItems(query);
    }

    @GetMapping("/settings/recommendations")
    public RecommendationSettings getRecommendationSettings(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return new RecommendationSettings(adminPanelService.areRecommendationsEnabled());
    }

    @PutMapping("/settings/recommendations")
    public RecommendationSettings updateRecommendationSettings(
            @AuthenticationPrincipal OAuth2User user,
            @RequestBody RecommendationSettings request) {
        requireAdmin(user);
        adminPanelService.setRecommendationsEnabled(request.enabled());
        return new RecommendationSettings(adminPanelService.areRecommendationsEnabled());
    }

    @GetMapping("/settings/platform")
    public java.util.Map<String, Boolean> getPlatformSettings(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getPlatformSettings();
    }

    @PutMapping("/settings/platform/{key}")
    public void updatePlatformSetting(@AuthenticationPrincipal OAuth2User user, @PathVariable String key,
            @RequestBody EnabledRequest request) {
        requireAdmin(user);
        adminPanelService.setPlatformSetting(key, request.enabled());
    }

    @GetMapping("/activity")
    public List<AdminActivityResponse> getAdminActivity(
            @AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getAdminActivity();
    }

    @GetMapping("/database/overview")
    public DatabaseOverviewResponse getDatabaseOverview(
            @AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return adminPanelService.getDatabaseOverview();
    }

    @GetMapping("/settings/session")
    public SessionDuration getSessionDuration(@AuthenticationPrincipal OAuth2User user) {
        requireAdmin(user);
        return new SessionDuration(adminPanelService.getSessionDurationDays());
    }

    @PutMapping("/settings/session")
    public SessionDuration updateSessionDuration(@AuthenticationPrincipal OAuth2User user,
            @RequestBody SessionDuration request) {
        requireAdmin(user);
        adminPanelService.setSessionDurationDays(request.sessionDurationDays());
        return new SessionDuration(adminPanelService.getSessionDurationDays());
    }

    private void requireAdmin(OAuth2User user) {
        adminAuthorizationService.requireAdmin(user.getAttribute("email"));
    }

    public record RecommendationSettings(boolean enabled) {
    }

    public record UserStatusRequest(String status) {
    }

    public record CategoryRequest(String name, String description) {
    }

    public record EnabledRequest(boolean enabled) {
    }

    public record StatusRequest(String status) {
    }

    public record SessionDuration(Integer sessionDurationDays) {
    }
}
