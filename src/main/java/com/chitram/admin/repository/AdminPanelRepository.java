package com.chitram.admin.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.admin.dto.AdminActivityResponse;
import com.chitram.admin.dto.AdminCategoryResponse;
import com.chitram.admin.dto.AdminReportResponse;
import com.chitram.admin.dto.AdminTableResponse;
import com.chitram.admin.dto.DatabaseTableInfo;

@Repository
public class AdminPanelRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminPanelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAvailableAreas() {
        return List.of("moderation", "platform-settings", "usage-overview");
    }

    public boolean areRecommendationsEnabled() {
        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT enabled FROM app_settings WHERE setting_key = ?",
                Boolean.class,
                "recommendations_enabled");
        return enabled == null || enabled;
    }

    public void setRecommendationsEnabled(boolean enabled) {
        jdbcTemplate.update(
                "UPDATE app_settings SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?",
                enabled,
                "recommendations_enabled");
    }

    public java.util.Map<String, Boolean> getPlatformSettings() {
        return jdbcTemplate.query("SELECT setting_key, enabled FROM app_settings ORDER BY setting_key", rs -> {
            java.util.Map<String, Boolean> values = new java.util.LinkedHashMap<>();
            while (rs.next())
                values.put(rs.getString("setting_key"), rs.getBoolean("enabled"));
            return values;
        });
    }

    public void setPlatformSetting(String key, boolean enabled) {
        jdbcTemplate.update("UPDATE app_settings SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?",
                enabled, key);
    }

    public boolean isPlatformSettingEnabled(String key) {
        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT enabled FROM app_settings WHERE setting_key = ?",
                Boolean.class,
                key);
        return enabled == null || enabled;
    }

    public int getSessionDurationDays() {
        String value = jdbcTemplate.queryForObject("SELECT setting_value FROM app_settings WHERE setting_key = ?", String.class, "session_duration_days");
        try { return value == null ? 30 : Integer.parseInt(value); } catch (NumberFormatException exception) { return 30; }
    }

    public void setSessionDurationDays(int days) {
        jdbcTemplate.update("UPDATE app_settings SET setting_value = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?", Integer.toString(days), "session_duration_days");
    }

    public void logAdminActivity(String action, String target) {
        jdbcTemplate.update("INSERT INTO admin_activity_log (admin_name, action, target) VALUES (?, ?, ?)", "admin",
                action, target);
    }

    public List<AdminActivityResponse> findAdminActivity() {
        return jdbcTemplate.query(
                "SELECT action, target, created_at::text AS occurred_at FROM admin_activity_log ORDER BY created_at DESC LIMIT 50",
                (rs, row) -> new AdminActivityResponse(rs.getString("action"), rs.getString("target"),
                        rs.getString("occurred_at")));
    }

    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0 : count;
    }

    public long countUsersCreatedThisWeek() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'",
                Long.class);
        return count == null ? 0 : count;
    }

    public long countActiveUsers() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM user_interactions WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'",
                Long.class);
        return count == null ? 0 : count;
    }

    public long countPinsCreatedToday() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visual_items WHERE created_at >= CURRENT_DATE",
                Long.class);
        return count == null ? 0 : count;
    }

    public long countLikesToday() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pin_likes WHERE created_at >= CURRENT_DATE",
                Long.class);
        return count == null ? 0 : count;
    }

    public long countStorageBytes() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(file_size), 0) FROM visual_items",
                Long.class);
        return count == null ? 0 : count;
    }

    public List<AdminActivityResponse> findRecentActivity() {
        return jdbcTemplate.query("""
                SELECT action, target, occurred_at::text FROM (
                    SELECT 'User joined' AS action, display_name AS target, created_at AS occurred_at FROM users
                    UNION ALL
                    SELECT 'New pin uploaded', title, created_at FROM visual_items
                    UNION ALL
                    SELECT 'Pin liked', CAST(visual_item_id AS TEXT), created_at FROM pin_likes
                    UNION ALL
                    SELECT 'Pin reported', COALESCE(target_type, 'PIN') || ' #' || COALESCE(target_id, visual_item_id), created_at FROM reports
                    UNION ALL
                    SELECT 'Pin saved', CAST(visual_item_id AS TEXT), created_at FROM saved_pins
                    UNION ALL
                    SELECT action, COALESCE(target, 'Platform'), created_at FROM admin_activity_log
                ) activity
                ORDER BY occurred_at DESC
                LIMIT 8
                """, (resultSet, rowNumber) -> new AdminActivityResponse(
                resultSet.getString("action"),
                resultSet.getString("target"),
                resultSet.getString("occurred_at")));
    }

    public long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(n_live_tup, 0) FROM pg_stat_user_tables WHERE schemaname = 'public' AND relname = ?",
                Long.class,
                tableName);
        return count == null ? 0 : count;
    }

    public boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    public List<AdminTableResponse> findExistingTables() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                (resultSet, rowNumber) -> {
                    String tableName = resultSet.getString("table_name");
                    return new AdminTableResponse(tableName, countRowsExact(tableName), "Healthy");
                });
    }

    public boolean checkDatabaseHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<DatabaseTableInfo> getDatabaseTableInfo() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                (resultSet, rowNumber) -> {
                    String tableName = resultSet.getString("table_name");
                    long rowCount = countRowsExact(tableName);
                    return new DatabaseTableInfo(tableName, rowCount, "HEALTHY");
                });
    }

    private long countRowsExact(String tableName) {
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tableName + "\"", Long.class);
        return count == null ? 0 : count;
    }

    public boolean isAdmin(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = ? AND r.code = 'ADMIN'",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    public List<AdminUserResponse> findUsers(String search) {
        return jdbcTemplate.query(
                """
                        SELECT u.id, u.email, u.display_name, u.picture_url, u.created_at::text, u.account_status,
                               COALESCE(string_agg(DISTINCT r.code, ', ' ORDER BY r.code), 'USER') AS role,
                               (SELECT COUNT(*) FROM visual_items v WHERE v.uploaded_by = u.id) AS pins,
                               (SELECT COUNT(*) FROM pin_likes pl WHERE pl.user_id = u.id) AS likes,
                               (SELECT COUNT(*) FROM user_follows uf WHERE uf.following_id = u.id) AS followers,
                               (SELECT COUNT(*) FROM user_follows uf WHERE uf.follower_id = u.id) AS following
                        FROM users u
                        LEFT JOIN user_roles ur ON ur.user_id = u.id
                        LEFT JOIN roles r ON r.id = ur.role_id
                        WHERE LOWER(u.display_name) LIKE LOWER(?) OR LOWER(u.email) LIKE LOWER(?) OR LOWER(COALESCE(u.username, '')) LIKE LOWER(?)
                        GROUP BY u.id ORDER BY u.created_at DESC
                        """,
                (resultSet, rowNumber) -> new AdminUserResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("email"),
                        resultSet.getString("display_name"),
                        resultSet.getString("picture_url"),
                        resultSet.getString("role"), resultSet.getString("created_at"),
                        resultSet.getString("account_status"), resultSet.getLong("pins"),
                        resultSet.getLong("likes"), resultSet.getLong("followers"), resultSet.getLong("following")),
                "%" + (search == null ? "" : search.trim()) + "%",
                "%" + (search == null ? "" : search.trim()) + "%",
                "%" + (search == null ? "" : search.trim()) + "%");
    }

    public void setAccountStatus(long userId, String status) {
        jdbcTemplate.update("UPDATE users SET account_status = ? WHERE id = ?", status, userId);
    }

    public void deleteUser(long userId) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    public List<AdminCategoryResponse> findCategories() {
        return jdbcTemplate.query("SELECT id, name, description, enabled FROM categories ORDER BY name",
                (rs, row) -> new AdminCategoryResponse(rs.getLong("id"), rs.getString("name"),
                        rs.getString("description"), rs.getBoolean("enabled")));
    }

    public void createCategory(String name, String description) {
        jdbcTemplate.update("INSERT INTO categories (name, description) VALUES (?, ?)", name.trim(), description);
    }

    public void setCategoryEnabled(long id, boolean enabled) {
        jdbcTemplate.update("UPDATE categories SET enabled = ? WHERE id = ?", enabled, id);
    }

    public void deleteCategory(long id) {
        jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
    }

    public List<AdminReportResponse> findReports() {
        return jdbcTemplate.query("""
                  SELECT r.id, COALESCE(r.target_type, 'PIN') AS target_type,
                      COALESCE(r.target_id, r.visual_item_id) AS target_id,
                        COALESCE(reporter.username, reporter.email, 'Unknown') AS reported_by,
                      r.reason, r.description, r.status, r.created_at::text
                    FROM reports r LEFT JOIN users reporter ON reporter.id = COALESCE(r.reporter_id, r.reported_by)
                ORDER BY r.created_at DESC LIMIT 100
                """, (rs, row) -> new AdminReportResponse(rs.getLong("id"), rs.getString("target_type"),
                rs.getLong("target_id"), rs.getString("reported_by"), rs.getString("reason"),
                rs.getString("description"), rs.getString("status"), rs.getString("created_at")));
    }

    public List<AdminReportResponse> findReportsFiltered(String status, String targetType) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, COALESCE(r.target_type, 'PIN') AS target_type,
                    COALESCE(r.target_id, r.visual_item_id) AS target_id,
                    COALESCE(reporter.username, reporter.email, 'Unknown') AS reported_by,
                    r.reason, r.description, r.status, r.created_at::text
                FROM reports r LEFT JOIN users reporter ON reporter.id = COALESCE(r.reporter_id, r.reported_by)
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
        return jdbcTemplate.query(sql.toString(), (rs, row) -> new AdminReportResponse(rs.getLong("id"), rs.getString("target_type"),
                rs.getLong("target_id"), rs.getString("reported_by"), rs.getString("reason"),
                rs.getString("description"), rs.getString("status"), rs.getString("created_at")), params.toArray());
    }

    public void setReportStatus(long reportId, String status, String adminEmail) {
        jdbcTemplate.update("""
                UPDATE reports SET status = ?, reviewed_at = CURRENT_TIMESTAMP,
                    reviewed_by = (SELECT id FROM users WHERE email = ?)
                WHERE id = ?
                """, status, adminEmail, reportId);
    }

    public void setModerationStatus(long pinId, String status) {
        jdbcTemplate.update("UPDATE visual_items SET moderation_status = ? WHERE id = ?", status, pinId);
    }

    public void replaceRole(long userId, String role) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE code = ?",
                userId,
                role);
    }

    private final org.springframework.jdbc.core.RowMapper<VisualItemResponse> visualItemRowMapper = (resultSet,
            rowNumber) -> {
        java.sql.Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
        java.time.Instant createdAt = createdAtTimestamp != null ? createdAtTimestamp.toInstant() : null;
        java.math.BigDecimal aspectRatio = resultSet.getBigDecimal("aspect_ratio");
        Integer width = resultSet.getObject("width", Integer.class);
        Integer height = resultSet.getObject("height", Integer.class);
        Long fileSize = resultSet.getObject("file_size", Long.class);
        Long uploadedBy = resultSet.getObject("uploaded_by", Long.class);

        return new VisualItemResponse(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("category"),
                resultSet.getString("image_url"),
                resultSet.getString("image_path"),
                width,
                height,
                aspectRatio,
                fileSize,
                resultSet.getString("mime_type"),
                resultSet.getString("description"),
                createdAt,
                uploadedBy,
                resultSet.getString("creator_name"),
                resultSet.getString("creator_username"),
                resultSet.getString("creator_picture_url"));
    };

    private final org.springframework.jdbc.core.RowMapper<VisualItemResponse> visualItemLikeRowMapper = (resultSet,
            rowNumber) -> {
        java.sql.Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
        java.time.Instant createdAt = createdAtTimestamp != null ? createdAtTimestamp.toInstant() : null;
        return new VisualItemResponse(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("category"),
                resultSet.getString("image_url"),
                resultSet.getString("image_path"),
                resultSet.getObject("width", Integer.class),
                resultSet.getObject("height", Integer.class),
                resultSet.getBigDecimal("aspect_ratio"),
                resultSet.getObject("file_size", Long.class),
                resultSet.getString("mime_type"),
                resultSet.getString("description"),
                createdAt,
                resultSet.getObject("uploaded_by", Long.class),
                resultSet.getString("creator_name"),
                resultSet.getString("creator_username"),
                resultSet.getString("creator_picture_url"),
                resultSet.getLong("like_count"),
                resultSet.getBoolean("liked_by_current_user"));
    };

    public List<VisualItemResponse> findVisualItems(String query) {
        String search = query == null ? "" : query.trim();
        String sql = """
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by,
                       u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE LOWER(v.title) LIKE LOWER(?) OR LOWER(v.category) LIKE LOWER(?)
                ORDER BY v.id DESC
                """;
        return jdbcTemplate.query(sql, visualItemRowMapper, "%" + search + "%", "%" + search + "%");
    }

    public List<VisualItemResponse> findFeed(String query, Long cursor, int limit, Long currentUserId) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String search = query == null ? "" : query.trim();
        boolean hasSearch = !search.isEmpty();
        boolean hasCursor = cursor != null && cursor > 0;

        StringBuilder sql = new StringBuilder(
                """
                        WITH like_stats AS (
                            SELECT visual_item_id, COUNT(*) AS like_count
                            FROM pin_likes
                            GROUP BY visual_item_id
                        )
                                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by,
                               u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url,
                               COALESCE(ls.like_count, 0) AS like_count,
                               EXISTS (
                               SELECT 1 FROM pin_likes current_like
                               WHERE current_like.visual_item_id = v.id
                                 AND current_like.user_id = COALESCE(CAST(? AS BIGINT), -1)
                               ) AS liked_by_current_user
                                FROM visual_items v
                                LEFT JOIN users u ON u.id = v.uploaded_by
                        LEFT JOIN like_stats ls ON ls.visual_item_id = v.id
                                WHERE v.moderation_status = 'APPROVED'
                                """);

        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(currentUserId);

        if (hasSearch) {
            sql.append(" AND (LOWER(v.title) LIKE LOWER(?) OR LOWER(v.category) LIKE LOWER(?))");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (hasCursor) {
            sql.append(" AND v.id < ?");
            params.add(cursor);
        }

        sql.append(" ORDER BY v.id DESC LIMIT ?");
        params.add(safeLimit + 1); // Fetch 1 extra to determine hasMore

        return jdbcTemplate.query(sql.toString(), visualItemLikeRowMapper, params.toArray());
    }

    public java.util.Optional<VisualItemResponse> findById(long id) {
        String sql = """
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by,
                       u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE v.id = ?
                """;
        List<VisualItemResponse> items = jdbcTemplate.query(sql, visualItemRowMapper, id);
        return items.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(items.get(0));
    }

    public VisualItemResponse insertVisualItem(
            String title,
            String category,
            String imageUrl,
            String imagePath,
            Integer width,
            Integer height,
            java.math.BigDecimal aspectRatio,
            Long fileSize,
            String mimeType,
            String description,
            Long uploadedBy) {
        String sql = """
                INSERT INTO visual_items (
                    title, category, image_url, image_path, width, height, aspect_ratio,
                    file_size, mime_type, description, uploaded_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        Long insertedId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                title,
                category,
                imageUrl,
                imagePath,
                width,
                height,
                aspectRatio,
                fileSize,
                mimeType,
                description,
                uploadedBy);
        return findById(insertedId != null ? insertedId : 0L).orElse(null);
    }

    public void deleteVisualItem(long id) {
        jdbcTemplate.update("DELETE FROM visual_items WHERE id = ?", id);
    }

    public void updateVisualItem(long id, String title, String category, String description) {
        jdbcTemplate.update(
                "UPDATE visual_items SET title = ?, category = ?, description = ? WHERE id = ?",
                title,
                category,
                description,
                id);
    }
}
