package com.chitram.admin.repository;

import com.chitram.admin.dto.VisualItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class VisualItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public VisualItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final org.springframework.jdbc.core.RowMapper<VisualItemResponse> visualItemRowMapper = (resultSet,
            rowNumber) -> new VisualItemResponse(
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
                    toInstant(resultSet.getTimestamp("created_at")),
                    resultSet.getObject("uploaded_by", Long.class),
                    resultSet.getString("creator_name"),
                    resultSet.getString("creator_username"),
                    resultSet.getString("creator_picture_url"),
                    0,
                    false,
                    resultSet.getString("share_key"));

    private final org.springframework.jdbc.core.RowMapper<VisualItemResponse> visualItemLikeRowMapper = (resultSet,
            rowNumber) -> new VisualItemResponse(
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
                    toInstant(resultSet.getTimestamp("created_at")),
                    resultSet.getObject("uploaded_by", Long.class),
                    resultSet.getString("creator_name"),
                    resultSet.getString("creator_username"),
                    resultSet.getString("creator_picture_url"),
                    resultSet.getLong("like_count"),
                    resultSet.getBoolean("liked_by_current_user"),
                    resultSet.getString("share_key"));

    public List<VisualItemResponse> findVisualItems(String query, int limit) {
        String search = query == null ? "" : query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.query("""
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                       u.display_name AS creator_name, u.username AS creator_username,
                       u.picture_url AS creator_picture_url
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE LOWER(v.title) LIKE LOWER(?) OR LOWER(v.category) LIKE LOWER(?)
                ORDER BY v.id DESC
                LIMIT ?
                """, visualItemRowMapper, "%" + search + "%", "%" + search + "%", safeLimit);
    }

    public List<VisualItemResponse> findFeed(String query, Long cursor, int limit, Long currentUserId) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String search = query == null ? "" : query.trim();
        StringBuilder sql = new StringBuilder("""
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                       u.display_name AS creator_name, u.username AS creator_username,
                       u.picture_url AS creator_picture_url,
                       (SELECT COUNT(*) FROM pin_likes item_likes
                        WHERE item_likes.visual_item_id = v.id) AS like_count,
                       EXISTS (SELECT 1 FROM pin_likes current_like
                               WHERE current_like.visual_item_id = v.id
                                 AND current_like.user_id = COALESCE(CAST(? AS BIGINT), -1))
                           AS liked_by_current_user
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE v.moderation_status = 'APPROVED'
                """);
        List<Object> params = new java.util.ArrayList<>();
        params.add(currentUserId);
        if (!search.isEmpty()) {
            sql.append(" AND (LOWER(v.title) LIKE LOWER(?) OR LOWER(v.category) LIKE LOWER(?))");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        if (cursor != null && cursor > 0) {
            sql.append(" AND v.id < ?");
            params.add(cursor);
        }
        sql.append(" ORDER BY v.id DESC LIMIT ?");
        params.add(safeLimit + 1);
        return jdbcTemplate.query(sql.toString(), visualItemLikeRowMapper, params.toArray());
    }

    public Optional<VisualItemResponse> findRandomApprovedByCategory(String category, Long excludeId) {
        String sql = """
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                       u.display_name AS creator_name, u.username AS creator_username,
                       u.picture_url AS creator_picture_url
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE v.moderation_status = 'APPROVED'
                  AND LOWER(v.category) = LOWER(?)
                                    AND (CAST(? AS BIGINT) IS NULL OR v.id <> CAST(? AS BIGINT))
                ORDER BY RANDOM()
                LIMIT 1
                """;
        List<VisualItemResponse> items = jdbcTemplate.query(
                sql,
                visualItemRowMapper,
                category,
                excludeId,
                excludeId);
        return items.stream().findFirst();
    }

    public Optional<VisualItemResponse> findById(long id) {
        List<VisualItemResponse> items = jdbcTemplate.query("""
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                       u.display_name AS creator_name, u.username AS creator_username,
                       u.picture_url AS creator_picture_url
                FROM visual_items v
                LEFT JOIN users u ON u.id = v.uploaded_by
                WHERE v.id = ?
                """, visualItemRowMapper, id);
        return items.stream().findFirst();
    }

    public Optional<VisualItemResponse> findByShareKey(String username, String shareKey) {
        List<VisualItemResponse> items = jdbcTemplate.query("""
                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                       u.display_name AS creator_name, u.username AS creator_username,
                       u.picture_url AS creator_picture_url
                FROM visual_items v
                JOIN users u ON u.id = v.uploaded_by
                WHERE LOWER(u.username) = LOWER(?) AND v.share_key = ?
                """, visualItemRowMapper, username, shareKey);
        return items.stream().findFirst();
    }

    public VisualItemResponse insertVisualItem(
            String title, String category, String imageUrl, String imagePath,
            Integer width, Integer height, java.math.BigDecimal aspectRatio, Long fileSize,
            String mimeType, String description, Long uploadedBy) {
        Long insertedId = jdbcTemplate.queryForObject("""
                INSERT INTO visual_items (
                    title, category, image_url, image_path, width, height, aspect_ratio,
                    file_size, mime_type, description, uploaded_by, share_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, title, category, imageUrl, imagePath, width, height,
                aspectRatio, fileSize, mimeType, description, uploadedBy, java.util.UUID.randomUUID().toString());
        return findById(insertedId == null ? 0L : insertedId).orElse(null);
    }

    public void deleteVisualItem(long id) {
        jdbcTemplate.update("DELETE FROM visual_items WHERE id = ?", id);
    }

    public void updateVisualItem(long id, String title, String category, String description) {
        jdbcTemplate.update(
                "UPDATE visual_items SET title = ?, category = ?, description = ? WHERE id = ?",
                title, category, description, id);
    }

    public void setModerationStatus(long pinId, String status) {
        jdbcTemplate.update("UPDATE visual_items SET moderation_status = ? WHERE id = ?", status, pinId);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
