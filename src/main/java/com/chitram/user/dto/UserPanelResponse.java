package com.chitram.user.dto;

import java.util.List;

public record UserPanelResponse(
        String role,
        String panel,
        String message,
        List<String> availableAreas) {
}
