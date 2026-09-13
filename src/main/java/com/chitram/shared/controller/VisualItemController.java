package com.chitram.shared.controller;

import com.chitram.admin.dto.VisualFeedResponse;
import com.chitram.admin.dto.VisualItemResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visual-items")
public class VisualItemController {

    private final VisualItemService visualItemService;
    private final UserAccountRepository userAccountRepository;

    public VisualItemController(
            VisualItemService visualItemService,
            UserAccountRepository userAccountRepository) {
        this.visualItemService = visualItemService;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public List<VisualItemResponse> getVisualItems(@RequestParam(required = false) String query) {
        return visualItemService.findVisualItems(query);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisualItemResponse> getVisualItem(@PathVariable Long id) {
        return visualItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/share/{username}/{shareKey}")
    public ResponseEntity<VisualItemResponse> getSharedVisualItem(
            @PathVariable String username,
            @PathVariable String shareKey) {
        return visualItemService.findByShareKey(username, shareKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/feed")
    public VisualFeedResponse getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal OAuth2User user) {

        Long currentUserId = null;
        if (user != null && user.getAttribute("email") != null) {
            currentUserId = userAccountRepository.findByEmail(user.getAttribute("email"))
                    .map(account -> account.getId())
                    .orElse(null);
        }
        return visualItemService.findFeed(query, cursor, limit, currentUserId);
    }

    @GetMapping("/random/tech")
    public ResponseEntity<VisualItemResponse> getRandomTechItem(
            @RequestParam(required = false) Long excludeId) {
        return visualItemService.findRandomTechItem(excludeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVisualItem(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height) {

        if (!visualItemService.areUploadsEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Image uploads are currently disabled by the administrator"));
        }

        Long uploadedBy = null;
        if (user != null) {
            String email = user.getAttribute("email");
            if (email != null) {
                uploadedBy = userAccountRepository.findByEmail(email)
                        .map(account -> account.getId())
                        .orElse(null);
            }
        }
        return ResponseEntity
                .ok(visualItemService.upload(title, category, description, image, width, height, uploadedBy));
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

        boolean isAdmin = visualItemService.isAdmin(email);

        try {
            visualItemService.deletePin(id, userId, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Pin deleted successfully", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVisualItem(
            @PathVariable("id") Long id,
            @RequestBody UpdateVisualItemRequest request,
            @AuthenticationPrincipal OAuth2User user) {

        if (user == null || user.getAttribute("email") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        String email = user.getAttribute("email");
        Long userId = userAccountRepository.findByEmail(email)
                .map(account -> account.getId())
                .orElse(null);

        try {
            VisualItemResponse updated = visualItemService.updatePin(
                    id,
                    request.title(),
                    request.category(),
                    request.description(),
                    userId,
                    visualItemService.isAdmin(email));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    public record UpdateVisualItemRequest(String title, String category, String description) {
    }
}
