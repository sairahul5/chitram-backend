package com.chitram.admin.repository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import com.chitram.admin.dto.AdminUserResponse;
import com.chitram.admin.dto.VisualItemResponse;

@Repository
public class AdminPanelRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminPanelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAvailableAreas() {
        return List.of("moderation", "platform-settings", "usage-overview");
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

    public boolean isAdmin(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = ? AND r.code = 'ADMIN'",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    public List<AdminUserResponse> findUsers() {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.display_name, u.picture_url, COALESCE(string_agg(r.code, ', ' ORDER BY r.code), 'USER') AS role, u.created_at::text FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id LEFT JOIN roles r ON r.id = ur.role_id GROUP BY u.id ORDER BY u.created_at DESC",
                (resultSet, rowNumber) -> new AdminUserResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("email"),
                        resultSet.getString("display_name"),
                        resultSet.getString("picture_url"),
                        resultSet.getString("role"),
                        resultSet.getString("created_at")));
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

    public List<VisualItemResponse> findFeed(String query, Long cursor, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String search = query == null ? "" : query.trim();
        boolean hasSearch = !search.isEmpty();
        boolean hasCursor = cursor != null && cursor > 0;

        StringBuilder sql = new StringBuilder(
                """
                        SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                               v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by,
                               u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url
                        FROM visual_items v
                        LEFT JOIN users u ON u.id = v.uploaded_by
                        WHERE 1=1
                        """);

        java.util.List<Object> params = new java.util.ArrayList<>();

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

        return jdbcTemplate.query(sql.toString(), visualItemRowMapper, params.toArray());
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
