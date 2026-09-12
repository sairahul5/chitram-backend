package com.chitram.websocket;

import com.chitram.admin.service.AdminPanelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pushes a fresh dashboard snapshot to all connected admin clients every 10
 * seconds.
 * This keeps metrics (user counts, image counts, etc.) live without any user
 * action.
 */
@Component
@EnableScheduling
public class AdminScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdminScheduler.class);

    private final AdminPanelService adminPanelService;
    private final AdminEventPublisher adminEventPublisher;

    public AdminScheduler(AdminPanelService adminPanelService, AdminEventPublisher adminEventPublisher) {
        this.adminPanelService = adminPanelService;
        this.adminEventPublisher = adminEventPublisher;
    }

    @Scheduled(fixedDelay = 10_000)
    public void pushDashboardUpdate() {
        if (!adminEventPublisher.hasConnectedClients()) {
            return;
        }
        try {
            adminEventPublisher.publishDashboard(adminPanelService.getDashboard());
        } catch (Exception e) {
            log.warn("Scheduled dashboard push failed: {}", e.getMessage());
        }
    }
}
