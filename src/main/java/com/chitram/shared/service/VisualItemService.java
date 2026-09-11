package com.chitram.shared.service;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.repository.AdminPanelRepository;
import com.chitram.websocket.AdminEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
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

    private final AdminPanelRepository adminPanelRepository;
    private final AdminEventPublisher adminEventPublisher;
    private final String supabaseUrl;
    private final String supabaseServiceRoleKey;
    private final String supabaseBucket;
    private final HttpClient httpClient;

    public VisualItemService(
            AdminPanelRepository adminPanelRepository,
            AdminEventPublisher adminEventPublisher,
            @Value("${supabase.url:https://vioobzddncnyqbfljqhp.supabase.co}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String supabaseServiceRoleKey,
            @Value("${supabase.storage.bucket:chitram-images}") String supabaseBucket) {
        this.adminPanelRepository = adminPanelRepository;
        this.adminEventPublisher = adminEventPublisher;
        this.supabaseUrl = supabaseUrl.replaceAll("/$", "");
        this.supabaseServiceRoleKey = supabaseServiceRoleKey;
        this.supabaseBucket = supabaseBucket;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
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
        String imageUrl = uploadToSupabaseStorage(bytes, storagePath, mimeType);

        // 7. Save metadata into PostgreSQL
        VisualItemResponse saved = adminPanelRepository.insertVisualItem(
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
        return saved;
    }

    public void deletePin(long pinId, Long currentUserId, boolean isAdmin) {
        VisualItemResponse item = adminPanelRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("Pin not found"));

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(item.uploadedBy()))) {
            throw new SecurityException("You are not authorized to delete this pin");
        }

        // Delete from Supabase Storage if image_path is tracked
        if (item.imagePath() != null && !item.imagePath().isBlank()) {
            deleteFromSupabaseStorage(item.imagePath());
        }

        // Delete record from database
        adminPanelRepository.deleteVisualItem(pinId);
        // Broadcast deletion to admin WebSocket subscribers
        adminEventPublisher.publishDeletedImage(pinId);
    }

    public VisualItemResponse updatePin(long pinId, String title, String category, String description,
            Long currentUserId, boolean isAdmin) {
        VisualItemResponse item = adminPanelRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("Pin not found"));

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(item.uploadedBy()))) {
            throw new SecurityException("You are not authorized to edit this pin");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        adminPanelRepository.updateVisualItem(
                pinId,
                title.trim(),
                category == null || category.trim().isEmpty() ? "General" : category.trim(),
                description == null || description.trim().isEmpty() ? null : description.trim());
        return adminPanelRepository.findById(pinId).orElseThrow(() -> new IllegalArgumentException("Pin not found"));
    }

    private String uploadToSupabaseStorage(byte[] fileBytes, String storagePath, String contentType) {
        try {
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, supabaseBucket, storagePath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("apikey", supabaseServiceRoleKey)
                    .header("Authorization", "Bearer " + supabaseServiceRoleKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Supabase storage upload failed: HTTP {} - {}", response.statusCode(), response.body());
                throw new IllegalStateException("Failed to upload to Supabase storage: " + response.body());
            }

            return String.format("%s/storage/v1/object/public/%s/%s",
                    supabaseUrl, supabaseBucket, storagePath);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Exception during Supabase storage upload", e);
            throw new IllegalStateException("Failed to upload image to Supabase Storage", e);
        }
    }

    private void deleteFromSupabaseStorage(String storagePath) {
        try {
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, supabaseBucket, storagePath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("apikey", supabaseServiceRoleKey)
                    .header("Authorization", "Bearer " + supabaseServiceRoleKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully deleted image object from Supabase: {}", storagePath);
            } else {
                log.warn("Supabase returned status {} when deleting {}: {}", response.statusCode(), storagePath,
                        response.body());
            }
        } catch (Exception e) {
            log.error("Could not delete file from Supabase storage: {}", storagePath, e);
        }
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
