package com.chitram.shared.controller;

import com.chitram.shared.service.PinLikeService;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pins")
public class PinLikeController {

    private final PinLikeService pinLikeService;
    private final UserProfileService userProfileService;

    public PinLikeController(
            PinLikeService pinLikeService,
            UserProfileService userProfileService) {
        this.pinLikeService = pinLikeService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/{pinId}/like")
    public ResponseEntity<?> like(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable long pinId) {
        return execute(principal, pinId, true);
    }

    @DeleteMapping("/{pinId}/like")
    public ResponseEntity<?> unlike(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable long pinId) {
        return execute(principal, pinId, false);
    }

    private ResponseEntity<?> execute(OAuth2User principal, long pinId, boolean liked) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        try {
            UserAccount account = userProfileService.getCurrentUser(principal);
            PinLikeService.LikeResult result = liked
                    ? pinLikeService.like(account.getId(), pinId)
                    : pinLikeService.unlike(account.getId(), pinId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
