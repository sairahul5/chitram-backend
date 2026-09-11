package com.chitram.user.dto;

import com.chitram.admin.dto.VisualItemResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserProfileDetailsResponse(
        Long id,
        String name,
        String email,
        String pictureUrl,
        String username,
        long followersCount,
        long followingCount,
        long creationsCount,
        List<VisualItemResponse> creations,
        @JsonProperty("isAdmin") boolean isAdmin
) {}
