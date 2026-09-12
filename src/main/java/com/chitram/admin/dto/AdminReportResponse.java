package com.chitram.admin.dto;

public record AdminReportResponse(long id, String targetType, long targetId, String reportedBy, String reason,
        String description, String status, String createdAt) {
}