package com.chitram.recommendation.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.model.RecommendationCandidate;
import com.chitram.recommendation.repository.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final AdminPanelRepository adminPanelRepository;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            AdminPanelRepository adminPanelRepository) {
        this.recommendationRepository = recommendationRepository;
        this.adminPanelRepository = adminPanelRepository;
    }

    public void recordInteraction(long userId, long pinId, InteractionType type, Long durationMs) {
        if (!adminPanelRepository.areRecommendationsEnabled()) {
            return;
        }
        recommendationRepository.recordInteraction(userId, pinId, type, durationMs);
        recommendationRepository.updateInterest(userId, pinId, type.weight());
    }

    public List<VisualItemResponse> getRecommendations(long userId, int limit) {
        List<RecommendationCandidate> candidates = recommendationRepository.findCandidates(userId, 200);
        double maxInterest = max(candidates, candidate -> candidate.interestScore());
        double maxPopularity = max(candidates, candidate -> candidate.popularityScore());

        return candidates.stream()
                .sorted(Comparator
                        .comparingDouble(
                                (RecommendationCandidate candidate) -> score(candidate, maxInterest, maxPopularity))
                        .reversed())
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(candidate -> candidate.item())
                .toList();
    }

    private double score(RecommendationCandidate candidate, double maxInterest, double maxPopularity) {
        double normalizedInterest = normalize(candidate.interestScore(), maxInterest);
        double normalizedPopularity = normalize(candidate.popularityScore(), maxPopularity);

        return normalizedInterest * 0.45
                + candidate.creatorScore() * 0.20
                + normalizedPopularity * 0.20
                + candidate.freshnessScore() * 0.15;
    }

    private double max(List<RecommendationCandidate> candidates,
            java.util.function.ToDoubleFunction<RecommendationCandidate> extractor) {
        return candidates.stream().mapToDouble(extractor).max().orElse(0);
    }

    private double normalize(double value, double maximum) {
        return maximum <= 0 ? 0 : Math.max(0, Math.min(1, value / maximum));
    }
}
