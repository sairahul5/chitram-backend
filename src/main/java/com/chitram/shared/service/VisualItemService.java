package com.chitram.shared.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.dto.VisualFeedResponse;
import com.chitram.admin.service.AdminPanelService;
import com.chitram.admin.repository.VisualItemRepository;
import com.chitram.admin.service.AdminAuthorizationService;
import com.chitram.websocket.AdminEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class VisualItemService {

    private static final Logger log = LoggerFactory.getLogger(VisualItemService.class);

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    public static final int MIN_RESOLUTION_PX = 400; // 400 x 400 px
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/avif");

    private final VisualItemRepository visualItemRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminPanelService adminPanelService;
    private final AdminEventPublisher adminEventPublisher;
    private final PlatformSettingsService platformSettingsService;
    private final StorageService storageService;

    public VisualItemService(
            VisualItemRepository visualItemRepository,
            AdminAuthorizationService adminAuthorizationService,
            AdminPanelService adminPanelService,
            AdminEventPublisher adminEventPublisher,
            PlatformSettingsService platformSettingsService,
            StorageService storageService) {
        this.visualItemRepository = visualItemRepository;
        this.adminAuthorizationService = adminAuthorizationService;
        this.adminPanelService = adminPanelService;
        this.adminEventPublisher = adminEventPublisher;
        this.platformSettingsService = platformSettingsService;
        this.storageService = storageService;
    }

    public VisualItemResponse upload(
            String title,
            String category,
            String description,
            MultipartFile file,
            Integer clientWidth,
            Integer clientHeight,
            Long uploadedBy) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }

        // 1. File Size Check (10 MB max)
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "Image file size exceeds maximum limit of 10 MB. Current size: %.2f MB",
                    fileSize / (1024.0 * 1024.0)));
        }

        // 2. MIME Type Validation
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported image format. Allowed formats: JPEG, PNG, WebP, AVIF");
        }

        // 3. Inspect and Validate Resolution
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read image bytes", e);
        }

        int width = 0;
        int height = 0;

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bufferedImage != null) {
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
        } catch (Exception e) {
            log.debug("ImageIO could not decode dimensions directly: {}", e.getMessage());
        }

        // Fallback to client-measured dimensions if ImageIO didn't decode (e.g. WebP /
        // AVIF)
        if (width <= 0 || height <= 0) {
            if (clientWidth != null && clientHeight != null && clientWidth > 0 && clientHeight > 0) {
                width = clientWidth;
                height = clientHeight;
            } else {
                width = MIN_RESOLUTION_PX;
                height = MIN_RESOLUTION_PX;
            }
        }

        if (width < MIN_RESOLUTION_PX || height < MIN_RESOLUTION_PX) {
            throw new IllegalArgumentException(String.format(
                    "Image resolution is too low. Minimum required: %d × %d px. Got: %d × %d px",
                    MIN_RESOLUTION_PX, MIN_RESOLUTION_PX, width, height));
        }

        // 4. Calculate Aspect Ratio
        BigDecimal aspectRatio = BigDecimal.valueOf(width)
                .divide(BigDecimal.valueOf(height), 4, RoundingMode.HALF_UP);

        // 5. Structure Storage Path: pins/{userId}/{uuid}_{filename}
        String userFolder = uploadedBy != null ? String.valueOf(uploadedBy) : "anon";
        String extension = extractExtension(file.getOriginalFilename(), mimeType);
        String baseName = sanitizeBaseName(file.getOriginalFilename());
        String storagePath = String.format("pins/%s/%s_%s%s", userFolder, UUID.randomUUID(), baseName, extension);

        // 6. Upload to Supabase Storage
        String imageUrl = storageService.upload(bytes, storagePath, mimeType);

        // 7. Save metadata into PostgreSQL
        VisualItemResponse saved = visualItemRepository.insertVisualItem(
                title.trim(),
                category != null && !category.trim().isEmpty() ? category.trim() : "General",
                imageUrl,
                storagePath,
                width,
                height,
                aspectRatio,
                fileSize,
                mimeType,
                description != null ? description.trim() : null,
                uploadedBy);
        // Broadcast new image to admin WebSocket subscribers
        adminEventPublisher.publishNewImage(saved);
        adminPanelService.publishCurrentOperations();
        return saved;
    }

    public List<VisualItemResponse> findVisualItems(String query) {
        return visualItemRepository.findVisualItems(query, 100);
    }

    public Optional<VisualItemResponse> findById(long id, Long currentUserId) {
        return visualItemRepository.findById(id, currentUserId);
    }

    public Optional<VisualItemResponse> findByShareKey(String username, String shareKey) {
        return visualItemRepository.findByShareKey(username, shareKey);
    }

    public VisualFeedResponse findFeed(String query, Long cursor, int limit, Long currentUserId) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<VisualItemResponse> rawItems = visualItemRepository.findFeed(query, cursor, safeLimit, currentUserId);
        boolean hasMore = rawItems.size() > safeLimit;
        List<VisualItemResponse> items = hasMore ? new ArrayList<>(rawItems.subList(0, safeLimit)) : rawItems;
        Long nextCursor = hasMore && !items.isEmpty() ? items.get(items.size() - 1).id() : null;
        return new VisualFeedResponse(items, nextCursor, hasMore);
    }

    public Optional<VisualItemResponse> findRandomTechItem(Long excludeId) {
        return visualItemRepository.findRandomApprovedByCategory("tech", excludeId);
    }

    public boolean areUploadsEnabled() {
        return platformSettingsService.isImageUploadsEnabled();
    }

    public boolean isAdmin(String email) {
        return adminAuthorizationService.hasAdminRole(email);
    }

    public void deletePin(long pinId, Long currentUserId, boolean isAdmin) {
        VisualItemResponse item = visualItemRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("Pin not found"));

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(item.uploadedBy()))) {
            throw new SecurityException("You are not authorized to delete this pin");
        }

        // Delete from Supabase Storage if image_path is tracked
        if (item.imagePath() != null && !item.imagePath().isBlank()) {
            storageService.delete(item.imagePath());
        }

        // Delete record from database
        visualItemRepository.deleteVisualItem(pinId);
        // Broadcast deletion to admin WebSocket subscribers
        adminEventPublisher.publishDeletedImage(pinId);
        adminPanelService.publishCurrentOperations();
    }

    public VisualItemResponse updatePin(long pinId, String title, String category, String description,
            Long currentUserId, boolean isAdmin) {
        VisualItemResponse item = visualItemRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("Pin not found"));

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(item.uploadedBy()))) {
            throw new SecurityException("You are not authorized to edit this pin");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        visualItemRepository.updateVisualItem(
                pinId,
                title.trim(),
                category == null || category.trim().isEmpty() ? "General" : category.trim(),
                description == null || description.trim().isEmpty() ? null : description.trim());
        VisualItemResponse updated = visualItemRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("Pin not found"));
        adminPanelService.publishCurrentOperations();
        return updated;
    }

    private String sanitizeBaseName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "pin";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        String name = dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase(Locale.ROOT);
        if (name.length() > 30) {
            name = name.substring(0, 30);
        }
        return name.isEmpty() ? "pin" : name;
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.lastIndexOf('.') >= 0) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equalsIgnoreCase(contentType))
            return ".png";
        if ("image/webp".equalsIgnoreCase(contentType))
            return ".webp";
        if ("image/avif".equalsIgnoreCase(contentType))
            return ".avif";
        return ".jpg";
    }
}
