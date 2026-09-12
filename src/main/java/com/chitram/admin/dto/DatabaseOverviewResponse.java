package com.chitram.admin.dto;

import java.util.List;
import java.time.Instant;

public record DatabaseOverviewResponse(
                String status,
                int tableCount,
                long totalRows,
                List<DatabaseTableInfo> tables,
                Instant checkedAt) {
}
