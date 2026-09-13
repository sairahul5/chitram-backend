package com.chitram.recommendation.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.shared.service.PlatformSettingsService;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.model.RecommendationCandidate;
import com.chitram.recommendation.repository.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final PlatformSettingsService platformSettingsService;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            PlatformSettingsService platformSettingsService) {
        this.recommendationRepository = recommendationRepository;
        this.platformSettingsService = platformSettingsService;
    }

    public void recordInteraction(long userId, long pinId, InteractionType type, Long durationMs) {
        recommendationRepository.recordInteraction(userId, pinId, type, durationMs);
        if (platformSettingsService.areRecommendationsEnabled()) {
            recommendationRepository.updateInterest(userId, pinId, type.weight());
        }
    }

    public List<VisualItemResponse> getRecommendations(long userId, int limit) {
        List<RecommendationCandidate> candidates = recommendationRepository.findCandidates(userId, 200);
        return candidates.stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(candidate -> candidate.item())
                .toList();
    }
}
