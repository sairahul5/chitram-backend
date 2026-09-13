package com.chitram.user.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.service.AdminAuthorizationService;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.user.dto.UserProfileDetailsResponse;
import com.chitram.user.dto.UserPanelResponse;
import com.chitram.user.dto.UserSummaryResponse;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.repository.UserAccountRepository;
import com.chitram.user.repository.UserPanelRepository;
import com.chitram.shared.service.PlatformSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class UserPanelService {

    private final UserPanelRepository userPanelRepository;
    private final UserAccountRepository userAccountRepository;
    private final PlatformSettingsService platformSettingsService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final RecommendationService recommendationService;

    public UserPanelService(
            UserPanelRepository userPanelRepository,
            UserAccountRepository userAccountRepository,
            PlatformSettingsService platformSettingsService,
            AdminAuthorizationService adminAuthorizationService,
            RecommendationService recommendationService) {
        this.userPanelRepository = userPanelRepository;
        this.userAccountRepository = userAccountRepository;
        this.platformSettingsService = platformSettingsService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.recommendationService = recommendationService;
    }

    public UserPanelResponse getPanel() {
        return new UserPanelResponse(
                "USER",
                "user-panel",
                "User panel foundation is ready.",
                userPanelRepository.findAvailableAreas());
    }

    public List<UserSummaryResponse> searchUsers(String query) {
        if (!platformSettingsService.arePublicProfilesEnabled()) {
            return List.of();
        }
        return userPanelRepository.searchUsers(query);
    }

    @SuppressWarnings("null")
    public UserProfileDetailsResponse getProfile(String usernameOrId) {
        if (!platformSettingsService.arePublicProfilesEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "Public profiles are currently disabled");
        }
        Optional<UserAccount> accountResult = userAccountRepository.findByUsername(usernameOrId.trim().toLowerCase());
        if (accountResult.isEmpty()) {
            try {
                accountResult = userAccountRepository.findById(Long.valueOf(usernameOrId));
            } catch (NumberFormatException exception) {
                accountResult = Optional.empty();
            }
        }
        if (accountResult.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + usernameOrId);
        }
        UserAccount account = accountResult.get();
        Long userId = account.getId();

        long followersCount = userPanelRepository.countFollowers(userId);
        long followingCount = userPanelRepository.countFollowing(userId);
        long creationsCount = userPanelRepository.countCreations(userId);
        var creations = userPanelRepository.findCreationsByUserId(userId);
        boolean isAdmin = adminAuthorizationService.hasAdminRole(account.getEmail());

        return new UserProfileDetailsResponse(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPictureUrl(),
                account.getUsername(),
                followersCount,
                followingCount,
                creationsCount,
                creations,
                isAdmin);
    }

    public UserProfileDetailsResponse getProfile(Long userId) {
        return getProfile(String.valueOf(userId));
    }

    public List<VisualItemResponse> getAllCreations() {
        return userPanelRepository.findAllCreations();
    }

    public List<VisualItemResponse> getSavedPins(Long userId) {
        return userPanelRepository.findSavedPinsByUserId(userId);
    }

    public List<Long> getSavedPinIds(Long userId) {
        return userPanelRepository.findSavedPinIds(userId);
    }

    public void savePin(Long userId, Long visualItemId) {
        userPanelRepository.savePin(userId, visualItemId);
        recommendationService.recordInteraction(userId, visualItemId, InteractionType.SAVE, null);
    }

    public void unsavePin(Long userId, Long visualItemId) {
        userPanelRepository.unsavePin(userId, visualItemId);
    }

    public List<UserSummaryResponse> getFollowers(Long currentUserId, Long targetUserId) {
        return userPanelRepository.findFollowers(currentUserId, targetUserId);
    }

    public List<UserSummaryResponse> getFollowing(Long currentUserId, Long targetUserId) {
        return userPanelRepository.findFollowing(currentUserId, targetUserId);
    }

    public List<UserSummaryResponse> getCreators(Long currentUserId) {
        return userPanelRepository.findSuggestedCreators(currentUserId);
    }

    public void follow(Long currentUserId, Long targetUserId) {
        userPanelRepository.follow(currentUserId, targetUserId);
    }

    public void unfollow(Long currentUserId, Long targetUserId) {
        userPanelRepository.unfollow(currentUserId, targetUserId);
    }

    public boolean isFollowing(Long currentUserId, Long targetUserId) {
        return currentUserId != null && userPanelRepository.isFollowing(currentUserId, targetUserId);
    }
}
