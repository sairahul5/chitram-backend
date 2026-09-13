package com.chitram.user.controller;

import com.chitram.user.dto.UpdateProfileRequest;
import com.chitram.user.dto.UserProfileDetailsResponse;
import com.chitram.user.dto.UserProfileResponse;
import com.chitram.user.dto.UserPanelResponse;
import com.chitram.user.dto.PublicProfileResponse;
import com.chitram.user.dto.UserSummaryResponse;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserAccountService;
import com.chitram.user.service.UserPanelService;
import com.chitram.user.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserPanelController {

    private final UserPanelService userPanelService;
    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    public UserPanelController(
            UserPanelService userPanelService,
            UserProfileService userProfileService,
            UserAccountService userAccountService) {
        this.userPanelService = userPanelService;
        this.userProfileService = userProfileService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/panel")
    public UserPanelResponse getPanel() {
        return userPanelService.getPanel();
    }

    @GetMapping("/search")
    public List<UserSummaryResponse> searchUsers(@RequestParam(defaultValue = "") String query) {
        return userPanelService.searchUsers(query);
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return new UserProfileResponse(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPictureUrl(),
                account.getUsername());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody UpdateProfileRequest request) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        try {
            userAccountService.updateProfile(account.getId(), request.displayName(), request.username());
            return ResponseEntity.ok(userPanelService.getProfile(account.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/username/suggest")
    public Map<String, String> suggestUsername(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        String suggested = userAccountService.generateSuggestedUsername(account.getDisplayName());
        return Map.of("username", suggested);
    }

    @GetMapping("/username/check")
    public Map<String, Object> checkUsername(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String username) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        boolean available = userAccountService.isUsernameAvailable(username, account.getId());
        return Map.of("username", username, "available", available);
    }

    @GetMapping("/profile")
    public UserProfileDetailsResponse getMyProfile(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getProfile(account.getId());
    }

    @GetMapping("/all-creations")
    public List<com.chitram.admin.dto.VisualItemResponse> getAllCreations() {
        return userPanelService.getAllCreations();
    }

    @GetMapping("/saved")
    public List<com.chitram.admin.dto.VisualItemResponse> getSavedPins(
            @AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getSavedPins(account.getId());
    }

    @GetMapping("/saved-ids")
    public List<Long> getSavedPinIds(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getSavedPinIds(account.getId());
    }

    @PostMapping("/saved/{visualItemId}")
    public ResponseEntity<Map<String, Object>> savePin(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long visualItemId) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        userPanelService.savePin(account.getId(), visualItemId);
        return ResponseEntity.ok(Map.of("saved", true, "visualItemId", visualItemId));
    }

    @DeleteMapping("/saved/{visualItemId}")
    public ResponseEntity<Map<String, Object>> unsavePin(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long visualItemId) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        userPanelService.unsavePin(account.getId(), visualItemId);
        return ResponseEntity.ok(Map.of("saved", false, "visualItemId", visualItemId));
    }

    @GetMapping("/profile/{usernameOrId}")
    public PublicProfileResponse getUserProfile(@PathVariable String usernameOrId,
            @AuthenticationPrincipal OAuth2User principal) {
        UserProfileDetailsResponse profile = userPanelService.getProfile(usernameOrId);
        Long currentUserId = null;
        if (principal != null) {
            currentUserId = userProfileService.getCurrentUser(principal).getId();
        }
        return new PublicProfileResponse(
                profile.id(),
                profile.name(),
                profile.username(),
                profile.pictureUrl(),
                profile.followersCount(),
                profile.followingCount(),
                profile.creationsCount(),
                profile.creations(),
                userPanelService.isFollowing(currentUserId, profile.id()),
                profile.isAdmin());
    }

    @GetMapping("/followers")
    public List<UserSummaryResponse> getMyFollowers(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getFollowers(account.getId(), account.getId());
    }

    @GetMapping("/following")
    public List<UserSummaryResponse> getMyFollowing(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getFollowing(account.getId(), account.getId());
    }

    @GetMapping("/creators")
    public List<UserSummaryResponse> getCreators(@AuthenticationPrincipal OAuth2User principal) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        return userPanelService.getCreators(account.getId());
    }

    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<Map<String, Object>> followUser(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long targetUserId) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        userPanelService.follow(account.getId(), targetUserId);
        return ResponseEntity.ok(Map.of("success", true, "following", true));
    }

    @DeleteMapping("/follow/{targetUserId}")
    public ResponseEntity<Map<String, Object>> unfollowUser(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long targetUserId) {
        UserAccount account = userProfileService.getCurrentUser(principal);
        userPanelService.unfollow(account.getId(), targetUserId);
        return ResponseEntity.ok(Map.of("success", true, "following", false));
    }
}
