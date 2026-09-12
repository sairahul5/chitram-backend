package com.chitram.admin.service;

import com.chitram.admin.dto.AdminPanelResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.admin.dto.AdminDashboardResponse;
import com.chitram.admin.dto.AdminMetricResponse;
import com.chitram.admin.dto.AdminTableResponse;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.websocket.AdminEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import com.chitram.admin.dto.AdminCategoryResponse;
import com.chitram.admin.dto.AdminReportResponse;

@Service
public class AdminPanelService {

    private final AdminPanelRepository adminPanelRepository;
    private final AdminEventPublisher adminEventPublisher;

    public AdminPanelService(AdminPanelRepository adminPanelRepository, AdminEventPublisher adminEventPublisher) {
        this.adminPanelRepository = adminPanelRepository;
        this.adminEventPublisher = adminEventPublisher;
    }

    public AdminPanelResponse getPanel() {
        return new AdminPanelResponse(
                "ADMIN",
                "admin-panel",
                "Admin panel foundation is ready.",
                adminPanelRepository.findAvailableAreas());
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                List.of(
                        new AdminMetricResponse("Active users", adminPanelRepository.countUsers()),
                    new AdminMetricResponse("Total users", adminPanelRepository.countUsers()),
                    new AdminMetricResponse("Total pins", countTableRows("visual_items")),
                    new AdminMetricResponse("Pins today", adminPanelRepository.countPinsCreatedToday()),
                    new AdminMetricResponse("Likes today", adminPanelRepository.countLikesToday()),
                    new AdminMetricResponse("Reports pending", countTableRows("reports")),
                    new AdminMetricResponse("Storage used (bytes)", adminPanelRepository.countStorageBytes())),
                List.of(
                        table("users"),
                        table("oauth_accounts"),
                        table("pins"),
                        table("boards"),
                        table("reports")),
                    adminPanelRepository.findRecentActivity());
    }

    public List<AdminUserResponse> getUsers() {
        return adminPanelRepository.findUsers("");
    }

    public List<AdminUserResponse> searchUsers(String search) {
        return adminPanelRepository.findUsers(search);
    }

    public void setAccountStatus(long userId, String status) {
        if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        }
        adminPanelRepository.setAccountStatus(userId, status);
    }

    public void deleteUser(long userId) {
        adminPanelRepository.deleteUser(userId);
    }

    public List<AdminCategoryResponse> getCategories() { return adminPanelRepository.findCategories(); }
    public void createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Category name is required");
        adminPanelRepository.createCategory(name, description);
    }
    public void setCategoryEnabled(long id, boolean enabled) { adminPanelRepository.setCategoryEnabled(id, enabled); }
    public void deleteCategory(long id) { adminPanelRepository.deleteCategory(id); }
    public List<AdminReportResponse> getReports() { return adminPanelRepository.findReports(); }
    public void setReportStatus(long id, String status) {
        if (!List.of("PENDING", "RESOLVED").contains(status)) throw new IllegalArgumentException("Invalid report status");
        adminPanelRepository.setReportStatus(id, status);
    }

    public void setModerationStatus(long pinId, String status) {
        if (!List.of("PENDING", "APPROVED", "REJECTED", "HIDDEN").contains(status)) {
            throw new IllegalArgumentException("Invalid moderation status");
        }
        adminPanelRepository.setModerationStatus(pinId, status);
    }

    public void updateUserRole(long userId, String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        adminPanelRepository.replaceRole(userId, role);
        // Push live updates to admin panel
        adminEventPublisher.publishUsers(getUsers());
        adminEventPublisher.publishDashboard(getDashboard());
    }

    public List<VisualItemResponse> getVisualItems(String query) {
        return adminPanelRepository.findVisualItems(query);
    }

    public boolean areRecommendationsEnabled() {
        return adminPanelRepository.areRecommendationsEnabled();
    }

    public void setRecommendationsEnabled(boolean enabled) {
        adminPanelRepository.setRecommendationsEnabled(enabled);
        adminPanelRepository.logAdminActivity(enabled ? "ENABLE_RECOMMENDATIONS" : "DISABLE_RECOMMENDATIONS", null);
    }

    public java.util.Map<String, Boolean> getPlatformSettings() { return adminPanelRepository.getPlatformSettings(); }
    public void setPlatformSetting(String key, boolean enabled) {
        if (!java.util.Set.of("registration_enabled", "image_uploads_enabled", "comments_enabled", "public_profiles_enabled").contains(key)) {
            throw new IllegalArgumentException("Unknown platform setting");
        }
        adminPanelRepository.setPlatformSetting(key, enabled);
        adminPanelRepository.logAdminActivity(enabled ? "ENABLE_SETTING" : "DISABLE_SETTING", key);
    }
    public List<com.chitram.admin.dto.AdminActivityResponse> getAdminActivity() { return adminPanelRepository.findAdminActivity(); }

    private AdminTableResponse table(String tableName) {
        boolean exists = adminPanelRepository.tableExists(tableName);
        return new AdminTableResponse(
                tableName,
                exists ? adminPanelRepository.countRows(tableName) : 0,
                exists ? "Healthy" : "Planned");
    }

    private long countTableRows(String tableName) {
        return adminPanelRepository.tableExists(tableName) ? adminPanelRepository.countRows(tableName) : 0;
    }

}
