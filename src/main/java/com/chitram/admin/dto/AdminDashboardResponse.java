package com.chitram.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        List<AdminMetricResponse> metrics,
        List<AdminTableResponse> tables,
        List<AdminActivityResponse> recentActivity) {
}