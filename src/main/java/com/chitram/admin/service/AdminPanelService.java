package com.chitram.admin.service;

import com.chitram.admin.dto.AdminPanelResponse;
import com.chitram.admin.repository.PlatformSettingsRepository;
import com.chitram.admin.repository.VisualItemRepository;
import com.chitram.admin.repository.AdminMetricsRepository;
import com.chitram.admin.repository.AdminActivityRepository;
import com.chitram.admin.repository.AdminUserRepository;
import com.chitram.admin.repository.CategoryRepository;
import com.chitram.admin.repository.DatabaseInspectionRepository;
import com.chitram.shared.repository.ReportRepository;
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

    private final AdminMetricsRepository adminMetricsRepository;
    private final AdminActivityRepository adminActivityRepository;
    private final AdminUserRepository adminUserRepository;
    private final CategoryRepository categoryRepository;
    private final ReportRepository reportRepository;
    private final DatabaseInspectionRepository databaseInspectionRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final VisualItemRepository visualItemRepository;
    private final AdminEventPublisher adminEventPublisher;

    public AdminPanelService(AdminMetricsRepository adminMetricsRepository,
            AdminActivityRepository adminActivityRepository,
            AdminUserRepository adminUserRepository,
            CategoryRepository categoryRepository,
            ReportRepository reportRepository,
            DatabaseInspectionRepository databaseInspectionRepository,
            PlatformSettingsRepository platformSettingsRepository,
            VisualItemRepository visualItemRepository,
            AdminEventPublisher adminEventPublisher) {
        this.adminMetricsRepository = adminMetricsRepository;
        this.adminActivityRepository = adminActivityRepository;
        this.adminUserRepository = adminUserRepository;
        this.categoryRepository = categoryRepository;
        this.reportRepository = reportRepository;
        this.databaseInspectionRepository = databaseInspectionRepository;
        this.platformSettingsRepository = platformSettingsRepository;
        this.visualItemRepository = visualItemRepository;
        this.adminEventPublisher = adminEventPublisher;
    }

    public AdminPanelResponse getPanel() {
        return new AdminPanelResponse(
                "ADMIN",
                "admin-panel",
                "Admin panel foundation is ready.",
                adminMetricsRepository.findAvailableAreas());
    }

    public AdminDashboardResponse getDashboard() {
        long totalUsers = adminMetricsRepository.countUsers();
        return new AdminDashboardResponse(
                List.of(
                        new AdminMetricResponse("Active users", totalUsers),
                        new AdminMetricResponse("Total users", totalUsers),
                        new AdminMetricResponse("Total pins", countTableRows("visual_items")),
                        new AdminMetricResponse("Pins today", adminMetricsRepository.countPinsCreatedToday()),
                        new AdminMetricResponse("Likes today", adminMetricsRepository.countLikesToday()),
                        new AdminMetricResponse("Reports pending", countTableRows("reports")),
                        new AdminMetricResponse("Storage used (bytes)", adminMetricsRepository.countStorageBytes())),
                databaseInspectionRepository.findExistingTables(),
                adminActivityRepository.findRecent());
    }

    public List<AdminUserResponse> getUsers() {
        return adminUserRepository.findUsers("");
    }

    public List<AdminUserResponse> searchUsers(String search) {
        return adminUserRepository.findUsers(search);
    }

    public void setAccountStatus(long userId, String status) {
        if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        }
        adminUserRepository.setAccountStatus(userId, status);
        publishAdminState();
    }

    public void deleteUser(long userId) {
        adminUserRepository.delete(userId);
        publishAdminState();
    }

    public List<AdminCategoryResponse> getCategories() {
        return categoryRepository.findAll();
    }

    public List<String> getEnabledCategoryNames() {
        return categoryRepository.findEnabledNames();
    }

    public void createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Category name is required");
        categoryRepository.create(name, description);
        publishAdminState();
    }

    public void setCategoryEnabled(long id, boolean enabled) {
        categoryRepository.setEnabled(id, enabled);
        publishAdminState();
    }

    public void deleteCategory(long id) {
        categoryRepository.delete(id);
        publishAdminState();
    }

    public List<AdminReportResponse> getReports() {
        return reportRepository.findAll();
    }

    public List<AdminReportResponse> getReportsFiltered(String status, String targetType) {
        return reportRepository.findFiltered(status, targetType);
    }

    public void setReportStatus(long id, String status, String adminEmail) {
        if (!List.of("PENDING", "REVIEWING", "RESOLVED", "DISMISSED").contains(status))
            throw new IllegalArgumentException("Invalid report status");
        reportRepository.setStatus(id, status, adminEmail);
        publishAdminState();
    }

    public void setModerationStatus(long pinId, String status) {
        if (!List.of("PENDING", "APPROVED", "REJECTED", "HIDDEN").contains(status)) {
            throw new IllegalArgumentException("Invalid moderation status");
        }
        visualItemRepository.setModerationStatus(pinId, status);
        publishAdminState();
    }

    public void updateUserRole(long userId, String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        adminUserRepository.replaceRole(userId, role);
        // Push live updates to admin panel
        adminEventPublisher.publishUsers(getUsers());
        adminEventPublisher.publishDashboard(getDashboard());
        adminEventPublisher.publishOperations(getOperations());
    }

    public List<VisualItemResponse> getVisualItems(String query) {
        return visualItemRepository.findVisualItems(query, 12);
    }

    public boolean areRecommendationsEnabled() {
        return platformSettingsRepository.areRecommendationsEnabled();
    }

    public void setRecommendationsEnabled(boolean enabled) {
        platformSettingsRepository.setRecommendationsEnabled(enabled);
        adminActivityRepository.log(enabled ? "ENABLE_RECOMMENDATIONS" : "DISABLE_RECOMMENDATIONS", null);
        publishAdminState();
    }

    public java.util.Map<String, Boolean> getPlatformSettings() {
        java.util.Map<String, Boolean> settings = platformSettingsRepository.findAll();
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
        platformSettingsRepository.setEnabled(key, enabled);
        adminActivityRepository.log(enabled ? "ENABLE_SETTING" : "DISABLE_SETTING", key);
        publishAdminState();
    }

    public List<AdminActivityResponse> getAdminActivity() {
        return adminActivityRepository.findAll();
    }

    public int getSessionDurationDays() {
        return platformSettingsRepository.getSessionDurationDays();
    }

    public void setSessionDurationDays(int days) {
        if (!java.util.Set.of(7, 30, 90, 365).contains(days)) {
            throw new IllegalArgumentException("Session duration must be 7, 30, 90, or 365 days");
        }
        platformSettingsRepository.setSessionDurationDays(days);
        adminActivityRepository.log("CHANGE_SESSION_DURATION", days + " days");
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
        if (!databaseInspectionRepository.checkHealth()) {
            return new DatabaseOverviewResponse("ERROR", 0, 0, List.of(), Instant.now());
        }

        try {
            List<DatabaseTableInfo> tables = databaseInspectionRepository.findTableInfo();
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
        return databaseInspectionRepository.tableExists(tableName) ? databaseInspectionRepository.countRows(tableName)
                : 0;
    }

}
