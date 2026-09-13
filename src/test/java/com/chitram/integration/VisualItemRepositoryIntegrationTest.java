package com.chitram.integration;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.repository.VisualItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualItemRepositoryIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired
    private VisualItemRepository repository;

    @Test
    void shouldReturnApprovedFeedInKeysetPagesWithoutDuplicates() {
        long userId = insertUser("creator@example.com", "creator");
        for (int index = 0; index < 25; index++) {
            insertPin(userId, "pin-" + index, index % 2 == 0 ? "Nature" : "Travel", "APPROVED");
        }
        insertPin(userId, "hidden", "Nature", "HIDDEN");

        List<VisualItemResponse> firstPage = repository.findFeed(null, null, 5, null);
        Long cursor = firstPage.get(4).id();
        List<VisualItemResponse> secondPage = repository.findFeed(null, cursor, 5, null);

        assertEquals(6, firstPage.size());
        assertEquals(6, secondPage.size());
        assertTrue(firstPage.get(0).id() > firstPage.get(1).id());
        assertTrue(firstPage.stream().noneMatch(item -> "hidden".equals(item.title())));
        assertTrue(new HashSet<>(firstPage.subList(0, 5)).stream()
                .noneMatch(item -> secondPage.subList(0, 5).contains(item)));
        assertEquals(cursor, firstPage.get(4).id());
        assertNotEquals(firstPage.get(5).id(), secondPage.get(0).id());
    }

    @Test
    void shouldFilterFeedByCategoryAndHonorLimit() {
        long userId = insertUser("category@example.com", "category-user");
        insertPin(userId, "nature-1", "Nature", "APPROVED");
        insertPin(userId, "travel-1", "Travel", "APPROVED");
        insertPin(userId, "nature-2", "Nature", "APPROVED");

        List<VisualItemResponse> result = repository.findFeed("nature", null, 1, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().limit(1).allMatch(item -> "Nature".equals(item.category())));
    }

    @Test
    void shouldReturnLikeCountAndCurrentUserLikeStateFromProjection() {
        long creatorId = insertUser("projection@example.com", "projection");
        long viewerId = insertUser("viewer@example.com", "viewer");
        long pinId = insertPin(creatorId, "liked-pin", "Nature", "APPROVED");
        jdbcTemplate.update("INSERT INTO pin_likes (user_id, visual_item_id) VALUES (?, ?)", viewerId, pinId);
        jdbcTemplate.update("INSERT INTO pin_likes (user_id, visual_item_id) VALUES (?, ?)", creatorId, pinId);

        VisualItemResponse item = repository.findFeed(null, null, 10, viewerId).stream()
                .filter(value -> value.id().equals(pinId)).findFirst().orElseThrow();

        assertEquals(2L, item.likeCount());
        assertTrue(item.likedByCurrentUser());
    }
}
