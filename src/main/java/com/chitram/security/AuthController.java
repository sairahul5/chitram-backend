package com.chitram.security;

import com.chitram.user.dto.UserProfileResponse;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserProfileService userProfileService;

    public AuthController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/session")
    public UserProfileResponse getSession(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return null;
        }

        UserAccount account = userProfileService.getCurrentUser(principal);
        return new UserProfileResponse(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPictureUrl(),
                account.getUsername());
    }
}