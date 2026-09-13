package com.chitram.shared.repository;

import com.chitram.admin.dto.AdminReportResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean targetExists(String targetType, long targetId) {
        String table = "PIN".equals(targetType) ? "visual_items" : "users";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                targetId);
        return count != null && count > 0;
    }

    public boolean hasPendingReport(String targetType, long targetId, long reporterId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM reports
                        WHERE target_type = ? AND target_id = ? AND reporter_id = ?
                          AND status IN ('PENDING', 'REVIEWING')
                        """,
                Integer.class,
                targetType,
                targetId,
                reporterId);
        return count != null && count > 0;
    }

    public void insert(String targetType, long targetId, long reporterId, String reason, String description) {
        jdbcTemplate.update(
                """
                        INSERT INTO reports
                            (visual_item_id, reported_by, target_type, target_id, reporter_id, reason, description)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                "PIN".equals(targetType) ? targetId : null,
                reporterId,
                targetType,
                targetId,
                reporterId,
                reason,
                description);
    }

    public java.util.List<AdminReportResponse> findAll() {
        return jdbcTemplate.query("""
                SELECT r.id, COALESCE(r.target_type, 'PIN') AS target_type,
                       COALESCE(r.target_id, r.visual_item_id) AS target_id,
                       COALESCE(reporter.username, reporter.email, 'Unknown') AS reported_by,
                       r.reason, r.description, r.status, r.created_at::text
                FROM reports r
                LEFT JOIN users reporter ON reporter.id = COALESCE(r.reporter_id, r.reported_by)
                ORDER BY r.created_at DESC LIMIT 100
                """, this::mapAdminReport);
    }

    public java.util.List<AdminReportResponse> findFiltered(String status, String targetType) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, COALESCE(r.target_type, 'PIN') AS target_type,
                       COALESCE(r.target_id, r.visual_item_id) AS target_id,
                       COALESCE(reporter.username, reporter.email, 'Unknown') AS reported_by,
                       r.reason, r.description, r.status, r.created_at::text
                FROM reports r
                LEFT JOIN users reporter ON reporter.id = COALESCE(r.reporter_id, r.reported_by)
                WHERE 1=1
                """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND r.status = ?");
            params.add(status.toUpperCase());
        }
        if (targetType != null && !targetType.isEmpty()) {
            sql.append(" AND COALESCE(r.target_type, 'PIN') = ?");
            params.add(targetType.toUpperCase());
        }
        sql.append(" ORDER BY r.created_at DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), this::mapAdminReport, params.toArray());
    }

    public void setStatus(long reportId, String status, String adminEmail) {
        jdbcTemplate.update("""
                UPDATE reports SET status = ?, reviewed_at = CURRENT_TIMESTAMP,
                    reviewed_by = (SELECT id FROM users WHERE email = ?)
                WHERE id = ?
                """, status, adminEmail, reportId);
    }

    private AdminReportResponse mapAdminReport(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new AdminReportResponse(rs.getLong("id"), rs.getString("target_type"), rs.getLong("target_id"),
                rs.getString("reported_by"), rs.getString("reason"), rs.getString("description"),
                rs.getString("status"), rs.getString("created_at"));
    }
}
