package com.chitram.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VisualItemResponse(
        Long id,
        String title,
        String category,
        String imageUrl,
        String imagePath,
        Integer width,
        Integer height,
        BigDecimal aspectRatio,
        Long fileSize,
        String mimeType,
        String description,
        Instant createdAt,
        Long uploadedBy,
        String creatorName,
        String creatorUsername,
        String creatorPictureUrl,
        long likeCount,
        boolean likedByCurrentUser
) {
    public VisualItemResponse(Long id, String title, String category, String imageUrl) {
        this(id, title, category, imageUrl, null, 800, 1000, BigDecimal.valueOf(0.8000), null, null, null, null, null, null, null, null, 0, false);
    }

    public VisualItemResponse(
            Long id,
            String title,
            String category,
            String imageUrl,
            String imagePath,
            Integer width,
            Integer height,
            BigDecimal aspectRatio,
            Long fileSize,
            String mimeType,
            String description,
            Instant createdAt,
            Long uploadedBy,
            String creatorName,
            String creatorUsername,
            String creatorPictureUrl) {
        this(id, title, category, imageUrl, imagePath, width, height, aspectRatio, fileSize, mimeType,
                description, createdAt, uploadedBy, creatorName, creatorUsername, creatorPictureUrl, 0, false);
    }
}
