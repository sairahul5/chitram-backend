package com.chitram.admin.dto;

import java.util.List;

public record AdminPanelResponse(
        String role,
        String panel,
        String message,
        List<String> availableAreas) {
}
