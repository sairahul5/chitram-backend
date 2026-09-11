package com.chitram.user.dto;

public record UpdateProfileRequest(
        String displayName,
        String username
) {}
