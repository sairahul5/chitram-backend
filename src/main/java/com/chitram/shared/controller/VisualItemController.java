package com.chitram.shared.controller;

import com.chitram.admin.dto.VisualFeedResponse;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.shared.service.VisualItemService;
import com.chitram.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visual-items")
public class VisualItemController {

    private final AdminPanelRepository adminPanelRepository;
    private final VisualItemService visualItemService;
    private final UserAccountRepository userAccountRepository;

    public VisualItemController(
            AdminPanelRepository adminPanelRepository,
            VisualItemService visualItemService,
            UserAccountRepository userAccountRepository) {
        this.adminPanelRepository = adminPanelRepository;
        this.visualItemService = visualItemService;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public List<VisualItemResponse> getVisualItems(@RequestParam(required = false) String query) {
        return adminPanelRepository.findVisualItems(query);
    }

    @GetMapping("/feed")
    public VisualFeedResponse getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String query) {

        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<VisualItemResponse> rawItems = adminPanelRepository.findFeed(query, cursor, safeLimit);

        boolean hasMore = rawItems.size() > safeLimit;
        List<VisualItemResponse> items = hasMore ? new ArrayList<>(rawItems.subList(0, safeLimit)) : rawItems;

        Long nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            nextCursor = items.get(items.size() - 1).id();
        }

        return new VisualFeedResponse(items, nextCursor, hasMore);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisualItemResponse uploadVisualItem(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height) {

        Long uploadedBy = null;
        if (user != null) {
            String email = user.getAttribute("email");
            if (email != null) {
                uploadedBy = userAccountRepository.findByEmail(email)
                        .map(account -> account.getId())
                        .orElse(null);
            }
        }
        return visualItemService.upload(title, category, description, image, width, height, uploadedBy);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVisualItem(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal OAuth2User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        String email = user.getAttribute("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid user session"));
        }

        Long userId = userAccountRepository.findByEmail(email)
                .map(account -> account.getId())
                .orElse(null);

        boolean isAdmin = adminPanelRepository.isAdmin(email);

        try {
            visualItemService.deletePin(id, userId, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Pin deleted successfully", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
