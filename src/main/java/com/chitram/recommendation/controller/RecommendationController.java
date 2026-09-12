package com.chitram.recommendation.controller;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserProfileService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserProfileService userProfileService;
    private final AdminPanelRepository adminPanelRepository;

    public RecommendationController(
            RecommendationService recommendationService,
            UserProfileService userProfileService,
            AdminPanelRepository adminPanelRepository) {
        this.recommendationService = recommendationService;
        this.userProfileService = userProfileService;
        this.adminPanelRepository = adminPanelRepository;
    }

    @GetMapping("/status")
    public RecommendationStatus getStatus() {
        return new RecommendationStatus(adminPanelRepository.areRecommendationsEnabled());
    }

    @GetMapping
    public List<VisualItemResponse> getRecommendations(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(defaultValue = "20") int limit) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return recommendationService.getRecommendations(account.getId(), limit);
    }

    @PostMapping("/interactions")
    public ResponseEntity<Void> recordInteraction(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody InteractionRequest request) {
        if (!adminPanelRepository.areRecommendationsEnabled()) {
            return ResponseEntity.accepted().build();
        }
        UserAccount account = userProfileService.getCurrentUser(principal);
        try {
            recommendationService.recordInteraction(
                    account.getId(),
                    request.pinId(),
                    InteractionType.valueOf(request.type().trim().toUpperCase()),
                    request.durationMs());
        } catch (DataAccessException exception) {
            // Interaction tracking is optional and must not disrupt the gallery.
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.noContent().build();
    }

    public record InteractionRequest(Long pinId, String type, Long durationMs) {
    }

    public record RecommendationStatus(boolean enabled) {
    }
}
