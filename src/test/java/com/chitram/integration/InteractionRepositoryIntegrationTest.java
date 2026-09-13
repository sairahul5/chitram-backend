package com.chitram.integration;

import com.chitram.shared.repository.PinLikeRepository;
import com.chitram.user.repository.UserPanelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionRepositoryIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired private PinLikeRepository pinLikeRepository;
    @Autowired private UserPanelRepository userPanelRepository;

    @Test
    void shouldPreventDuplicateLikesAndSupportUnlike() {
        long userId = insertUser("likes@example.com", "likes");
        long pinId = insertPin(userId, "like-target", "Nature", "APPROVED");

        PinLikeRepository.LikeMutation first = pinLikeRepository.insertLike(userId, pinId);
        PinLikeRepository.LikeMutation duplicate = pinLikeRepository.insertLike(userId, pinId);

        assertTrue(first.pinExists());
        assertTrue(first.changed());
        assertFalse(duplicate.changed());
        assertEquals(1L, pinLikeRepository.countLikes(pinId));
        assertTrue(pinLikeRepository.isLikedByUser(userId, pinId));

        PinLikeRepository.LikeMutation deleted = pinLikeRepository.deleteLike(userId, pinId);
        assertTrue(deleted.changed());
        assertEquals(0L, pinLikeRepository.countLikes(pinId));
        assertFalse(pinLikeRepository.isLikedByUser(userId, pinId));
    }

    @Test
    void shouldKeepSavesIdempotentAndRemoveThem() {
        long userId = insertUser("saves@example.com", "saves");
        long pinId = insertPin(userId, "save-target", "Nature", "APPROVED");

        userPanelRepository.savePin(userId, pinId);
        userPanelRepository.savePin(userId, pinId);

        assertTrue(userPanelRepository.isPinSaved(userId, pinId));
        assertEquals(List.of(pinId), userPanelRepository.findSavedPinIds(userId));

        userPanelRepository.unsavePin(userId, pinId);
        assertFalse(userPanelRepository.isPinSaved(userId, pinId));
    }

    @Test
    void shouldIgnoreSelfFollowAndPreventDuplicateFollows() {
        long userId = insertUser("follow-a@example.com", "follow-a");
        long targetId = insertUser("follow-b@example.com", "follow-b");

        userPanelRepository.follow(userId, userId);
        userPanelRepository.follow(userId, targetId);
        userPanelRepository.follow(userId, targetId);

        assertFalse(userPanelRepository.isFollowing(userId, userId));
        assertTrue(userPanelRepository.isFollowing(userId, targetId));
        assertEquals(1L, userPanelRepository.countFollowers(targetId));

        userPanelRepository.unfollow(userId, targetId);
        assertFalse(userPanelRepository.isFollowing(userId, targetId));
    }
}
