package com.chitram.user.dto;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        String pictureUrl,
        String username,
        long followersCount,
        boolean following
) {}
