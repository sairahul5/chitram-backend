package com.chitram.shared.service;

import com.chitram.admin.repository.PlatformSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class PlatformSettingsService {

    private final PlatformSettingsRepository settingsRepository;

    public PlatformSettingsService(PlatformSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public boolean areRecommendationsEnabled() {
        return settingsRepository.areRecommendationsEnabled();
    }

    public boolean isImageUploadsEnabled() {
        return settingsRepository.isEnabled("image_uploads_enabled");
    }

    public boolean arePublicProfilesEnabled() {
        return settingsRepository.isEnabled("public_profiles_enabled");
    }

    public int getSessionDurationDays() {
        return settingsRepository.getSessionDurationDays();
    }
}
