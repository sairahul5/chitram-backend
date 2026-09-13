package com.chitram.admin.dto;

import java.util.List;
import java.util.Map;

public record AdminOperationsResponse(
        List<AdminCategoryResponse> categories,
        List<AdminReportResponse> reports,
        List<VisualItemResponse> pins,
        Map<String, Boolean> platformSettings,
        List<AdminActivityResponse> activity,
        int sessionDurationDays) {
}