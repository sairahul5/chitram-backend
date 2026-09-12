package com.chitram.shared.api;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final Set<String> TARGET_TYPES = Set.of("PIN", "USER");
    private static final Set<String> REASONS = Set.of("SPAM", "NUDITY", "VIOLENCE", "HARASSMENT", "COPYRIGHT", "MISLEADING", "IMPERSONATION", "INAPPROPRIATE_CONTENT", "HATE", "OTHER");

    private final JdbcTemplate jdbcTemplate;
    private final UserProfileService userProfileService;
    private final RecommendationService recommendationService;

    public ReportController(JdbcTemplate jdbcTemplate, UserProfileService userProfileService, RecommendationService recommendationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userProfileService = userProfileService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<Void> report(@AuthenticationPrincipal OAuth2User principal, @RequestBody ReportRequest request) {
        UserAccount reporter = userProfileService.getCurrentUser(principal);
        String targetType = request.targetType() == null ? "" : request.targetType().trim().toUpperCase();
        String reason = request.reason() == null ? "" : request.reason().trim().toUpperCase();
        if (!TARGET_TYPES.contains(targetType) || !REASONS.contains(reason) || request.targetId() == null) {
            throw new IllegalArgumentException("Invalid report target or reason");
        }
        if ("USER".equals(targetType) && reporter.getId().equals(request.targetId())) {
            throw new IllegalArgumentException("You cannot report your own account");
        }
        String targetTable = "PIN".equals(targetType) ? "visual_items" : "users";
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + targetTable + " WHERE id = ?", Integer.class, request.targetId()) == 0) {
            throw new IllegalArgumentException("Report target was not found");
        }
        Integer duplicate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reports WHERE target_type = ? AND target_id = ? AND reporter_id = ? AND status IN ('PENDING', 'REVIEWING')", Integer.class, targetType, request.targetId(), reporter.getId());
        if (duplicate != null && duplicate > 0) {
            return ResponseEntity.accepted().build();
        }
        jdbcTemplate.update("INSERT INTO reports (visual_item_id, reported_by, target_type, target_id, reporter_id, reason, description) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "PIN".equals(targetType) ? request.targetId() : null, reporter.getId(), targetType, request.targetId(), reporter.getId(), reason, request.description());
        if ("PIN".equals(targetType)) {
            recommendationService.recordInteraction(reporter.getId(), request.targetId(), InteractionType.REPORT, null);
        }
        return ResponseEntity.accepted().build();
    }
}