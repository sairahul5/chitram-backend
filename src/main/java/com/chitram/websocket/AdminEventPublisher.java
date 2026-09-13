package com.chitram.websocket;

import com.chitram.admin.dto.AdminDashboardResponse;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.dto.AdminOperationsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central publisher for all admin-panel WebSocket events.
 * Inject this service into any component that mutates data the admin panel
 * displays.
 */
@Service
public class AdminEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AdminEventPublisher.class);

    // Topic destinations — frontend subscribes to these
    public static final String TOPIC_DASHBOARD = "/topic/admin/dashboard";
    public static final String TOPIC_USERS = "/topic/admin/users";
    public static final String TOPIC_IMAGE_NEW = "/topic/admin/images/new";
    public static final String TOPIC_IMAGE_DEL = "/topic/admin/images/deleted";
    public static final String TOPIC_OPERATIONS = "/topic/admin/operations";

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public AdminEventPublisher(SimpMessagingTemplate messagingTemplate, SimpUserRegistry userRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    public boolean hasConnectedClients() {
        return !userRegistry.getUsers().isEmpty();
    }

    /** Broadcast a fresh dashboard snapshot to all connected admin clients. */
    public void publishDashboard(AdminDashboardResponse dashboard) {
        try {
            messagingTemplate.convertAndSend(TOPIC_DASHBOARD, dashboard);
            log.debug("Published dashboard update to {}", TOPIC_DASHBOARD);
        } catch (Exception e) {
            log.warn("Failed to publish dashboard update: {}", e.getMessage());
        }
    }

    /** Broadcast the full user list when any user changes. */
    public void publishUsers(List<AdminUserResponse> users) {
        try {
            messagingTemplate.convertAndSend(TOPIC_USERS, users);
            log.debug("Published user list update ({} users) to {}", users.size(), TOPIC_USERS);
        } catch (Exception e) {
            log.warn("Failed to publish user list update: {}", e.getMessage());
        }
    }

    /** Broadcast a newly uploaded image to admin feed. */
    public void publishNewImage(VisualItemResponse item) {
        try {
            messagingTemplate.convertAndSend(TOPIC_IMAGE_NEW, item);
            log.debug("Published new image (id={}) to {}", item.id(), TOPIC_IMAGE_NEW);
        } catch (Exception e) {
            log.warn("Failed to publish new image: {}", e.getMessage());
        }
    }

    /** Broadcast the ID of a deleted image so admin can remove it from the feed. */
    public void publishDeletedImage(long imageId) {
        try {
            messagingTemplate.convertAndSend(TOPIC_IMAGE_DEL, imageId);
            log.debug("Published deleted image id={} to {}", imageId, TOPIC_IMAGE_DEL);
        } catch (Exception e) {
            log.warn("Failed to publish deleted image: {}", e.getMessage());
        }
    }

    public void publishOperations(AdminOperationsResponse operations) {
        try {
            messagingTemplate.convertAndSend(TOPIC_OPERATIONS, operations);
            log.debug("Published admin operations update to {}", TOPIC_OPERATIONS);
        } catch (Exception e) {
            log.warn("Failed to publish admin operations update: {}", e.getMessage());
        }
    }
}
