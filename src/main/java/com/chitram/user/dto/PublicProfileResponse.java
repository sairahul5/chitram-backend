package com.chitram.user.dto;

import com.chitram.admin.dto.VisualItemResponse;
import java.util.List;

public record PublicProfileResponse(
        Long id,
        String name,
        String username,
        String pictureUrl,
        long followersCount,
        long followingCount,
        long creationsCount,
        List<VisualItemResponse> creations,
        boolean isFollowing,
        boolean isAdmin) {
}
