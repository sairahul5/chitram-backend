package com.chitram.admin.dto;

import java.util.List;

public record DatabaseOverviewResponse(
        String status,
        int tableCount,
        long totalRows,
        List<DatabaseTableInfo> tables) {
}
