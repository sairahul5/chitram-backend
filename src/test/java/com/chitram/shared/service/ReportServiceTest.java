package com.chitram.shared.service;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.shared.repository.ReportRepository;
import com.chitram.user.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock RecommendationService recommendationService;
    @Mock UserAccount reporter;

    @Test
    void shouldReportPinAndRecordInteraction() {
        ReportService service = service();
        when(reporter.getId()).thenReturn(7L);
        when(reportRepository.targetExists("PIN", 11L)).thenReturn(true);
        when(reportRepository.hasPendingReport("PIN", 11L, 7L)).thenReturn(false);

        assertTrue(service.submit(reporter, "pin", 11L, "spam", "bad content"));

        verify(reportRepository).insert("PIN", 11L, 7L, "SPAM", "bad content");
        verify(recommendationService).recordInteraction(7L, 11L, InteractionType.REPORT, null);
    }

    @Test
    void shouldReportAccountWithoutRecommendationInteraction() {
        ReportService service = service();
        when(reporter.getId()).thenReturn(7L);
        when(reportRepository.targetExists("USER", 12L)).thenReturn(true);

        assertTrue(service.submit(reporter, "USER", 12L, "harassment", null));

        verify(reportRepository).insert("USER", 12L, 7L, "HARASSMENT", null);
        verifyNoRecommendationInteraction();
    }

    @Test
    void shouldRejectInvalidReport() {
        ReportService service = service();

        assertThrows(IllegalArgumentException.class, () -> service.submit(reporter, "PIN", 0L, "SPAM", null));
        org.mockito.Mockito.verifyNoInteractions(reportRepository);
    }

    @Test
    void shouldReturnFalseForDuplicatePendingReport() {
        ReportService service = service();
        when(reporter.getId()).thenReturn(7L);
        when(reportRepository.targetExists("PIN", 11L)).thenReturn(true);
        when(reportRepository.hasPendingReport("PIN", 11L, 7L)).thenReturn(true);

        assertFalse(service.submit(reporter, "PIN", 11L, "SPAM", null));
        verify(reportRepository, never()).insert(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectReportingOwnAccount() {
        ReportService service = service();
        when(reporter.getId()).thenReturn(7L);

        assertThrows(IllegalArgumentException.class, () -> service.submit(reporter, "USER", 7L, "SPAM", null));
        org.mockito.Mockito.verifyNoInteractions(reportRepository);
    }

    private ReportService service() {
        return new ReportService(reportRepository, recommendationService);
    }

    private void verifyNoRecommendationInteraction() {
        org.mockito.Mockito.verifyNoInteractions(recommendationService);
    }
}
