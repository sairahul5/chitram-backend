package com.chitram.user.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String pictureUrl,
        String username
) {
    public UserProfileResponse(String name) {
        this(null, name, null, null, null);
    }
}