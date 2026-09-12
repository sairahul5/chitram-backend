package com.chitram.admin.dto;

public record AdminReportResponse(long id, long pinId, String reportedBy, String reason, String status, String createdAt) {
}