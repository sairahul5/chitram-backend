package com.chitram.recommendation.model;

import com.chitram.admin.dto.VisualItemResponse;

public record RecommendationCandidate(
        VisualItemResponse item,
        double interestScore,
        double creatorScore,
        double popularityScore,
        double freshnessScore) {
}
