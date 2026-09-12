package com.chitram.user.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.user.dto.UserProfileDetailsResponse;
import com.chitram.user.dto.UserPanelResponse;
import com.chitram.user.dto.UserSummaryResponse;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.repository.UserAccountRepository;
import com.chitram.user.repository.UserPanelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPanelService {

    private final UserPanelRepository userPanelRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminPanelRepository adminPanelRepository;
    private final RecommendationService recommendationService;

    public UserPanelService(
            UserPanelRepository userPanelRepository,
            UserAccountRepository userAccountRepository,
            AdminPanelRepository adminPanelRepository,
            RecommendationService recommendationService) {
        this.userPanelRepository = userPanelRepository;
        this.userAccountRepository = userAccountRepository;
        this.adminPanelRepository = adminPanelRepository;
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
        return userPanelRepository.searchUsers(query);
    }

    public UserProfileDetailsResponse getProfile(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        long followersCount = userPanelRepository.countFollowers(userId);
        long followingCount = userPanelRepository.countFollowing(userId);
        long creationsCount = userPanelRepository.countCreations(userId);
        var creations = userPanelRepository.findCreationsByUserId(userId);
        boolean isAdmin = adminPanelRepository.isAdmin(account.getEmail());

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
}
