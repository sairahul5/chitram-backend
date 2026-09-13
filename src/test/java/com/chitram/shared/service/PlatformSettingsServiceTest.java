package com.chitram.shared.service;

import com.chitram.admin.repository.PlatformSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsServiceTest {

    @Mock PlatformSettingsRepository repository;

    @Test
    void shouldReadRecommendationSetting() {
        PlatformSettingsService service = new PlatformSettingsService(repository);
        when(repository.areRecommendationsEnabled()).thenReturn(false);

        assertFalse(service.areRecommendationsEnabled());
        verify(repository).areRecommendationsEnabled();
    }

    @Test
    void shouldReadUploadAndProfileSettings() {
        PlatformSettingsService service = new PlatformSettingsService(repository);
        when(repository.isEnabled("image_uploads_enabled")).thenReturn(true);
        when(repository.isEnabled("public_profiles_enabled")).thenReturn(false);

        assertTrue(service.isImageUploadsEnabled());
        assertFalse(service.arePublicProfilesEnabled());
        verify(repository).isEnabled("image_uploads_enabled");
        verify(repository).isEnabled("public_profiles_enabled");
    }
}
