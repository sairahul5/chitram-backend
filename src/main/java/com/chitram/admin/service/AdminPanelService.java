package com.chitram.admin.service;

import com.chitram.admin.dto.AdminPanelResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.admin.dto.AdminDashboardResponse;
import com.chitram.admin.dto.AdminMetricResponse;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.websocket.AdminEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;
import com.chitram.admin.dto.AdminCategoryResponse;
import com.chitram.admin.dto.AdminReportResponse;
import com.chitram.admin.dto.AdminActivityResponse;
import com.chitram.admin.dto.DatabaseOverviewResponse;
import com.chitram.admin.dto.DatabaseTableInfo;
import com.chitram.admin.dto.AdminOperationsResponse;

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
                adminPanelRepository.findExistingTables(),
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
        publishAdminState();
    }

    public void deleteUser(long userId) {
        adminPanelRepository.deleteUser(userId);
        publishAdminState();
    }

    public List<AdminCategoryResponse> getCategories() {
        return adminPanelRepository.findCategories();
    }

    public List<String> getEnabledCategoryNames() {
        return adminPanelRepository.findEnabledCategoryNames();
    }

    public void createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Category name is required");
        adminPanelRepository.createCategory(name, description);
        publishAdminState();
    }

    public void setCategoryEnabled(long id, boolean enabled) {
        adminPanelRepository.setCategoryEnabled(id, enabled);
        publishAdminState();
    }

    public void deleteCategory(long id) {
        adminPanelRepository.deleteCategory(id);
        publishAdminState();
    }

    public List<AdminReportResponse> getReports() {
        return adminPanelRepository.findReports();
    }

    public List<AdminReportResponse> getReportsFiltered(String status, String targetType) {
        return adminPanelRepository.findReportsFiltered(status, targetType);
    }

    public void setReportStatus(long id, String status, String adminEmail) {
        if (!List.of("PENDING", "REVIEWING", "RESOLVED", "DISMISSED").contains(status))
            throw new IllegalArgumentException("Invalid report status");
        adminPanelRepository.setReportStatus(id, status, adminEmail);
        publishAdminState();
    }

    public void setModerationStatus(long pinId, String status) {
        if (!List.of("PENDING", "APPROVED", "REJECTED", "HIDDEN").contains(status)) {
            throw new IllegalArgumentException("Invalid moderation status");
        }
        adminPanelRepository.setModerationStatus(pinId, status);
        publishAdminState();
    }

    public void updateUserRole(long userId, String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        adminPanelRepository.replaceRole(userId, role);
        // Push live updates to admin panel
        adminEventPublisher.publishUsers(getUsers());
        adminEventPublisher.publishDashboard(getDashboard());
        adminEventPublisher.publishOperations(getOperations());
    }

    public List<VisualItemResponse> getVisualItems(String query) {
        return adminPanelRepository.findVisualItems(query, 12);
    }

    public boolean areRecommendationsEnabled() {
        return adminPanelRepository.areRecommendationsEnabled();
    }

    public void setRecommendationsEnabled(boolean enabled) {
        adminPanelRepository.setRecommendationsEnabled(enabled);
        adminPanelRepository.logAdminActivity(enabled ? "ENABLE_RECOMMENDATIONS" : "DISABLE_RECOMMENDATIONS", null);
        publishAdminState();
    }

    public java.util.Map<String, Boolean> getPlatformSettings() {
        java.util.Map<String, Boolean> settings = adminPanelRepository.getPlatformSettings();
        java.util.Set<String> supported = java.util.Set.of("registration_enabled", "image_uploads_enabled",
                "public_profiles_enabled");
        settings.keySet().removeIf(key -> !supported.contains(key));
        return settings;
    }

    public void setPlatformSetting(String key, boolean enabled) {
        if (!java.util.Set.of("registration_enabled", "image_uploads_enabled", "public_profiles_enabled")
                .contains(key)) {
            throw new IllegalArgumentException("Unknown platform setting");
        }
        adminPanelRepository.setPlatformSetting(key, enabled);
        adminPanelRepository.logAdminActivity(enabled ? "ENABLE_SETTING" : "DISABLE_SETTING", key);
        publishAdminState();
    }

    public List<AdminActivityResponse> getAdminActivity() {
        return adminPanelRepository.findAdminActivity();
    }

    public int getSessionDurationDays() {
        return adminPanelRepository.getSessionDurationDays();
    }

    public void setSessionDurationDays(int days) {
        if (!java.util.Set.of(7, 30, 90, 365).contains(days)) {
            throw new IllegalArgumentException("Session duration must be 7, 30, 90, or 365 days");
        }
        adminPanelRepository.setSessionDurationDays(days);
        adminPanelRepository.logAdminActivity("CHANGE_SESSION_DURATION", days + " days");
        publishAdminState();
    }

    public AdminOperationsResponse getOperations() {
        return new AdminOperationsResponse(
                getCategories(),
                getReports(),
                getVisualItems(null),
                getPlatformSettings(),
                getAdminActivity(),
                getSessionDurationDays());
    }

    public void publishCurrentOperations() {
        adminEventPublisher.publishOperations(getOperations());
        adminEventPublisher.publishDashboard(getDashboard());
    }

    private void publishAdminState() {
        publishCurrentOperations();
    }

    public DatabaseOverviewResponse getDatabaseOverview() {
        if (!adminPanelRepository.checkDatabaseHealth()) {
            return new DatabaseOverviewResponse("ERROR", 0, 0, List.of(), Instant.now());
        }

        try {
            List<DatabaseTableInfo> tables = adminPanelRepository.getDatabaseTableInfo();
            long totalRows = 0;
            for (DatabaseTableInfo table : tables) {
                totalRows += table.rowCount();
            }
            return new DatabaseOverviewResponse("CONNECTED", tables.size(), totalRows, tables, Instant.now());
        } catch (RuntimeException exception) {
            return new DatabaseOverviewResponse("ERROR", 0, 0, List.of(), Instant.now());
        }
    }

    private long countTableRows(String tableName) {
        return adminPanelRepository.tableExists(tableName) ? adminPanelRepository.countRows(tableName) : 0;
    }

}
