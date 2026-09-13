package com.chitram.user.service;

import com.chitram.admin.service.AdminAuthorizationService;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.shared.service.PlatformSettingsService;
import com.chitram.user.repository.UserAccountRepository;
import com.chitram.user.repository.UserPanelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserPanelServiceTest {

    @Mock UserPanelRepository userPanelRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock PlatformSettingsService platformSettingsService;
    @Mock AdminAuthorizationService adminAuthorizationService;
    @Mock RecommendationService recommendationService;

    @Test
    void shouldSavePinAndRecordInteraction() {
        UserPanelService service = service();

        service.savePin(3L, 8L);

        verify(userPanelRepository).savePin(3L, 8L);
        verify(recommendationService).recordInteraction(3L, 8L, InteractionType.SAVE, null);
    }

    @Test
    void shouldUnsavePin() {
        UserPanelService service = service();

        service.unsavePin(3L, 8L);

        verify(userPanelRepository).unsavePin(3L, 8L);
    }

    @Test
    void shouldFollowUser() {
        UserPanelService service = service();

        service.follow(3L, 8L);

        verify(userPanelRepository).follow(3L, 8L);
    }

    @Test
    void shouldUnfollowUser() {
        UserPanelService service = service();

        service.unfollow(3L, 8L);

        verify(userPanelRepository).unfollow(3L, 8L);
    }

    @Test
    void shouldReturnFalseWithoutCurrentUser() {
        UserPanelService service = service();

        org.junit.jupiter.api.Assertions.assertFalse(service.isFollowing(null, 8L));
        org.mockito.Mockito.verifyNoInteractions(userPanelRepository);
    }

    private UserPanelService service() {
        return new UserPanelService(userPanelRepository, userAccountRepository, platformSettingsService,
                adminAuthorizationService, recommendationService);
    }
}
