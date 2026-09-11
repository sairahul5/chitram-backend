package com.chitram.admin.dto;

public record AdminUserResponse(
        Long id,
        String email,
        String displayName,
        String pictureUrl,
        String role,
        String createdAt) {
}
