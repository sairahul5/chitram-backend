package com.chitram.integration;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationRepositoryIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired
    private RecommendationRepository repository;

    @Test
    void shouldExcludeHiddenAndAlreadyReportedPinsAndRespectCandidateLimit() {
        long userId = insertUser("recommend@example.com", "recommend");
        long creatorId = insertUser("recommend-creator@example.com", "recommend-creator");
        long visible = insertPin(creatorId, "visible", "Nature", "APPROVED");
        long hidden = insertPin(creatorId, "hidden", "Nature", "HIDDEN");
        long reported = insertPin(creatorId, "reported", "Nature", "APPROVED");
        insertPin(creatorId, "another", "Travel", "APPROVED");

        repository.updateInterest(userId, visible, 5.0);
        repository.recordInteraction(userId, reported, InteractionType.REPORT, null);
        repository.recordInteraction(userId, hidden, InteractionType.HIDE, null);

        List<com.chitram.recommendation.model.RecommendationCandidate> candidates = repository.findCandidates(userId, 2);

        assertTrue(candidates.size() <= 2);
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.item().id().equals(visible)));
        assertFalse(candidates.stream().anyMatch(candidate -> candidate.item().id().equals(hidden)));
        assertFalse(candidates.stream().anyMatch(candidate -> candidate.item().id().equals(reported)));
    }

    @Test
    void shouldRecordInteractionAndUpsertCategoryInterest() {
        long userId = insertUser("interest@example.com", "interest");
        long creatorId = insertUser("interest-creator@example.com", "interest-creator");
        long pinId = insertPin(creatorId, "interest-pin", "Nature", "APPROVED");

        repository.recordInteraction(userId, pinId, InteractionType.LIKE, null);
        repository.updateInterest(userId, pinId, InteractionType.LIKE.weight());
        repository.updateInterest(userId, pinId, InteractionType.SAVE.weight());

        assertEquals(13.0, jdbcTemplate.queryForObject(
                "SELECT score FROM user_interests WHERE user_id = ? AND category = ?",
                Double.class, userId, "Nature"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_interactions WHERE user_id = ? AND visual_item_id = ?",
                Integer.class, userId, pinId));
    }
}
