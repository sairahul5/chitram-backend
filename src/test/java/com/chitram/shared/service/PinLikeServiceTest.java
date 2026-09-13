package com.chitram.shared.service;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.shared.repository.PinLikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinLikeServiceTest {

    @Mock PinLikeRepository pinLikeRepository;
    @Mock RecommendationService recommendationService;

    @Test
    void shouldLikePinSuccessfully() {
        PinLikeService service = new PinLikeService(pinLikeRepository, recommendationService);
        when(pinLikeRepository.insertLike(7L, 11L)).thenReturn(new PinLikeRepository.LikeMutation(true, true));
        when(pinLikeRepository.countLikes(11L)).thenReturn(4L);

        PinLikeService.LikeResult result = service.like(7L, 11L);

        assertTrue(result.liked());
        assertEquals(4L, result.likeCount());
        verify(recommendationService).recordInteraction(7L, 11L, InteractionType.LIKE, null);
        verify(pinLikeRepository).countLikes(11L);
    }

    @Test
    void shouldIgnoreDuplicateLikeInteraction() {
        PinLikeService service = new PinLikeService(pinLikeRepository, recommendationService);
        when(pinLikeRepository.insertLike(7L, 11L)).thenReturn(new PinLikeRepository.LikeMutation(true, false));
        when(pinLikeRepository.countLikes(11L)).thenReturn(4L);

        PinLikeService.LikeResult result = service.like(7L, 11L);

        assertTrue(result.liked());
        assertEquals(4L, result.likeCount());
        verify(recommendationService, never()).recordInteraction(7L, 11L, InteractionType.LIKE, null);
    }

    @Test
    void shouldRejectLikeForMissingPin() {
        PinLikeService service = new PinLikeService(pinLikeRepository, recommendationService);
        when(pinLikeRepository.insertLike(7L, 99L)).thenReturn(new PinLikeRepository.LikeMutation(false, false));

        assertThrows(IllegalArgumentException.class, () -> service.like(7L, 99L));
        verify(pinLikeRepository, never()).countLikes(99L);
        verifyNoInteractions(recommendationService);
    }

    @Test
    void shouldUnlikePinSuccessfully() {
        PinLikeService service = new PinLikeService(pinLikeRepository, recommendationService);
        when(pinLikeRepository.deleteLike(7L, 11L)).thenReturn(new PinLikeRepository.LikeMutation(true, true));
        when(pinLikeRepository.countLikes(11L)).thenReturn(3L);

        PinLikeService.LikeResult result = service.unlike(7L, 11L);

        assertFalse(result.liked());
        assertEquals(3L, result.likeCount());
        verify(recommendationService, never()).recordInteraction(7L, 11L, InteractionType.LIKE, null);
    }

    @Test
    void shouldRejectUnlikeForMissingPin() {
        PinLikeService service = new PinLikeService(pinLikeRepository, recommendationService);
        when(pinLikeRepository.deleteLike(7L, 99L)).thenReturn(new PinLikeRepository.LikeMutation(false, false));

        assertThrows(IllegalArgumentException.class, () -> service.unlike(7L, 99L));
        verify(pinLikeRepository, never()).countLikes(99L);
    }

    private void verifyNoInteractions(RecommendationService service) {
        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
