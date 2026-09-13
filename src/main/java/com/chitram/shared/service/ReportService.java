package com.chitram.shared.service;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.shared.repository.ReportRepository;
import com.chitram.user.entity.UserAccount;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class ReportService {

    private static final Set<String> TARGET_TYPES = Set.of("PIN", "USER");
    private static final Set<String> REASONS = Set.of(
            "SPAM", "NUDITY", "VIOLENCE", "HARASSMENT", "COPYRIGHT",
            "MISLEADING", "IMPERSONATION", "INAPPROPRIATE_CONTENT", "HATE", "OTHER");

    private final ReportRepository reportRepository;
    private final RecommendationService recommendationService;

    public ReportService(ReportRepository reportRepository, RecommendationService recommendationService) {
        this.reportRepository = reportRepository;
        this.recommendationService = recommendationService;
    }

    public boolean submit(UserAccount reporter, String targetType, Long targetId, String reason, String description) {
        String normalizedTargetType = normalize(targetType);
        String normalizedReason = normalize(reason);
        String normalizedDescription = description == null ? null : description.trim();

        validate(normalizedTargetType, targetId, normalizedReason, normalizedDescription);
        if ("USER".equals(normalizedTargetType) && reporter.getId().equals(targetId)) {
            throw new IllegalArgumentException("You cannot report your own account");
        }
        if (!reportRepository.targetExists(normalizedTargetType, targetId)) {
            throw new IllegalArgumentException("Report target was not found");
        }
        if (reportRepository.hasPendingReport(normalizedTargetType, targetId, reporter.getId())) {
            return false;
        }

        reportRepository.insert(normalizedTargetType, targetId, reporter.getId(), normalizedReason,
                normalizedDescription);
        if ("PIN".equals(normalizedTargetType)) {
            recommendationService.recordInteraction(reporter.getId(), targetId, InteractionType.REPORT, null);
        }
        return true;
    }

    private void validate(String targetType, Long targetId, String reason, String description) {
        if (!TARGET_TYPES.contains(targetType) || !REASONS.contains(reason)
                || targetId == null || targetId <= 0
                || (description != null && description.length() > 1000)) {
            throw new IllegalArgumentException("Invalid report target or reason");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
