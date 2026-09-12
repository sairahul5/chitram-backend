package com.chitram.shared.api;

public record ReportRequest(String targetType, Long targetId, String reason, String description) {
}