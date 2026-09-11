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
                        new AdminMetricResponse("New this week", adminPanelRepository.countUsersCreatedThisWeek()),
                        new AdminMetricResponse("Images shared", countTableRows("visual_items")),
                        new AdminMetricResponse("Reports pending", countTableRows("reports"))),
                List.of(
                        table("users"),
                        table("oauth_accounts"),
                        table("pins"),
                        table("boards"),
                        table("reports")));
    }

    public List<AdminUserResponse> getUsers() {
        return adminPanelRepository.findUsers();
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
