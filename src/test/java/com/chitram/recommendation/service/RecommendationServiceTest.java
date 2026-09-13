package com.chitram.recommendation.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.model.RecommendationCandidate;
import com.chitram.recommendation.repository.RecommendationRepository;
import com.chitram.shared.service.PlatformSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock RecommendationRepository recommendationRepository;
    @Mock PlatformSettingsService platformSettingsService;

    @Test
    void shouldRecordInterestWhenRecommendationsEnabled() {
        RecommendationService service = service();
        when(platformSettingsService.areRecommendationsEnabled()).thenReturn(true);

        service.recordInteraction(2L, 5L, InteractionType.LIKE, null);

        verify(recommendationRepository).recordInteraction(2L, 5L, InteractionType.LIKE, null);
        verify(recommendationRepository).updateInterest(2L, 5L, InteractionType.LIKE.weight());
    }

    @Test
    void shouldSkipInterestWhenRecommendationsDisabled() {
        RecommendationService service = service();
        when(platformSettingsService.areRecommendationsEnabled()).thenReturn(false);

        service.recordInteraction(2L, 5L, InteractionType.VIEW, 120L);

        verify(recommendationRepository).recordInteraction(2L, 5L, InteractionType.VIEW, 120L);
        org.mockito.Mockito.verify(recommendationRepository, org.mockito.Mockito.never()).updateInterest(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void shouldRespectRecommendationLimit() {
        RecommendationService service = service();
        when(recommendationRepository.findCandidates(2L, 200)).thenReturn(List.of(candidate(1), candidate(2), candidate(3)));

        List<VisualItemResponse> result = service.getRecommendations(2L, 2);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenNoCandidatesExist() {
        RecommendationService service = service();
        when(recommendationRepository.findCandidates(2L, 200)).thenReturn(List.of());

        assertEquals(List.of(), service.getRecommendations(2L, 20));
    }

    private RecommendationService service() {
        return new RecommendationService(recommendationRepository, platformSettingsService);
    }

    private RecommendationCandidate candidate(long id) {
        return new RecommendationCandidate(new VisualItemResponse(id, "pin-" + id, "Nature", "url"), 1, 0, 1, 1);
    }
}
