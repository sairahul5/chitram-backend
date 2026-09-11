package com.chitram.admin.dto;

import java.util.List;

public record VisualFeedResponse(
        List<VisualItemResponse> items,
        Long nextCursor,
        boolean hasMore
) {}
