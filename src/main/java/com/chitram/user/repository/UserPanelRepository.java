package com.chitram.user.repository;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.user.dto.UserSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPanelRepository {

        private final JdbcTemplate jdbcTemplate;

        public UserPanelRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<String> findAvailableAreas() {
                return List.of("discovery", "saved-collections", "profile-settings");
        }

        public List<UserSummaryResponse> searchUsers(String query) {
                String search = query == null ? "" : query.trim();
                return jdbcTemplate.query(
                                """
                                                SELECT u.id, u.display_name, u.picture_url, u.username,
                                                       (SELECT COUNT(*) FROM user_follows uf WHERE uf.following_id = u.id) AS followers_count
                                                FROM users u
                                                WHERE LOWER(u.display_name) LIKE LOWER(?) OR LOWER(COALESCE(u.username, '')) LIKE LOWER(?)
                                                ORDER BY u.display_name ASC
                                                LIMIT 30
                                                """,
                                (resultSet, rowNumber) -> new UserSummaryResponse(
                                                resultSet.getLong("id"),
                                                resultSet.getString("display_name"),
                                                null,
                                                resultSet.getString("picture_url"),
                                                resultSet.getString("username"),
                                                resultSet.getLong("followers_count"),
                                                false),
                                "%" + search + "%", "%" + search + "%");
        }

        public void savePin(long userId, long visualItemId) {
                jdbcTemplate.update(
                                "INSERT INTO saved_pins (user_id, visual_item_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                userId, visualItemId);
        }

        public void unsavePin(long userId, long visualItemId) {
                jdbcTemplate.update(
                                "DELETE FROM saved_pins WHERE user_id = ? AND visual_item_id = ?",
                                userId, visualItemId);
        }

        public boolean isPinSaved(long userId, long visualItemId) {
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM saved_pins WHERE user_id = ? AND visual_item_id = ?",
                                Integer.class, userId, visualItemId);
                return count != null && count > 0;
        }

        public List<Long> findSavedPinIds(long userId) {
                return jdbcTemplate.queryForList(
                                "SELECT visual_item_id FROM saved_pins WHERE user_id = ? ORDER BY created_at DESC",
                                Long.class, userId);
        }

        public List<VisualItemResponse> findSavedPinsByUserId(long userId) {
                String sql = """
                                SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                                       v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                                          u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url,
                                          (SELECT COUNT(*) FROM pin_likes item_likes WHERE item_likes.visual_item_id = v.id) AS like_count
                                FROM saved_pins s
                                JOIN visual_items v ON v.id = s.visual_item_id
                                LEFT JOIN users u ON u.id = v.uploaded_by
                                WHERE s.user_id = ?
                                ORDER BY s.created_at DESC
                                """;
                return jdbcTemplate.query(sql, visualItemRowMapper, userId);
        }

        public long countFollowers(long userId) {
                Long count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM user_follows WHERE following_id = ?",
                                Long.class,
                                userId);
                return count == null ? 0 : count;
        }

        public long countFollowing(long userId) {
                Long count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM user_follows WHERE follower_id = ?",
                                Long.class,
                                userId);
                return count == null ? 0 : count;
        }

        public long countCreations(long userId) {
                Long count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM visual_items WHERE uploaded_by = ?",
                                Long.class,
                                userId);
                return count == null ? 0 : count;
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
                                resultSet.getString("creator_picture_url"),
                                resultSet.getLong("like_count"),
                                false,
                                resultSet.getString("share_key"));
        };

        public List<VisualItemResponse> findCreationsByUserId(long userId) {
                String sql = """
                                                                                  SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                                                                                                        v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                                                                                                                u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url,
                                                                                                                (SELECT COUNT(*) FROM pin_likes item_likes WHERE item_likes.visual_item_id = v.id) AS like_count
                                FROM visual_items v
                                LEFT JOIN users u ON u.id = v.uploaded_by
                                WHERE v.uploaded_by = ?
                                ORDER BY v.id DESC
                                """;
                return jdbcTemplate.query(sql, visualItemRowMapper, userId);
        }

        public void follow(long followerId, long followingId) {
                if (followerId == followingId) {
                        return;
                }
                jdbcTemplate.update(
                                "INSERT INTO user_follows (follower_id, following_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                followerId, followingId);
        }

        public void unfollow(long followerId, long followingId) {
                jdbcTemplate.update(
                                "DELETE FROM user_follows WHERE follower_id = ? AND following_id = ?",
                                followerId, followingId);
        }

        public boolean isFollowing(long followerId, long followingId) {
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM user_follows WHERE follower_id = ? AND following_id = ?",
                                Integer.class,
                                followerId, followingId);
                return count != null && count > 0;
        }

        public List<UserSummaryResponse> findFollowers(long currentUserId, long targetUserId) {
                String sql = """
                                SELECT u.id, u.display_name, u.email, u.picture_url, u.username,
                                            COALESCE(follower_counts.followers_count, 0) AS followers_count,
                                       EXISTS(SELECT 1 FROM user_follows uf2 WHERE uf2.follower_id = ? AND uf2.following_id = u.id) AS is_following
                                FROM user_follows f
                                JOIN users u ON u.id = f.follower_id
                                    LEFT JOIN (
                                         SELECT following_id, COUNT(*) AS followers_count
                                         FROM user_follows
                                         GROUP BY following_id
                                    ) follower_counts ON follower_counts.following_id = u.id
                                WHERE f.following_id = ?
                                ORDER BY f.created_at DESC
                                """;
                return jdbcTemplate.query(sql, (rs, rowNum) -> new UserSummaryResponse(
                                rs.getLong("id"),
                                rs.getString("display_name"),
                                rs.getString("email"),
                                rs.getString("picture_url"),
                                rs.getString("username"),
                                rs.getLong("followers_count"),
                                rs.getBoolean("is_following")),
                                currentUserId, targetUserId);
        }

        public List<UserSummaryResponse> findFollowing(long currentUserId, long targetUserId) {
                String sql = """
                                SELECT u.id, u.display_name, u.email, u.picture_url, u.username,
                                            COALESCE(follower_counts.followers_count, 0) AS followers_count,
                                       EXISTS(SELECT 1 FROM user_follows uf2 WHERE uf2.follower_id = ? AND uf2.following_id = u.id) AS is_following
                                FROM user_follows f
                                JOIN users u ON u.id = f.following_id
                                    LEFT JOIN (
                                         SELECT following_id, COUNT(*) AS followers_count
                                         FROM user_follows
                                         GROUP BY following_id
                                    ) follower_counts ON follower_counts.following_id = u.id
                                WHERE f.follower_id = ?
                                ORDER BY f.created_at DESC
                                """;
                return jdbcTemplate.query(sql, (rs, rowNum) -> new UserSummaryResponse(
                                rs.getLong("id"),
                                rs.getString("display_name"),
                                rs.getString("email"),
                                rs.getString("picture_url"),
                                rs.getString("username"),
                                rs.getLong("followers_count"),
                                rs.getBoolean("is_following")),
                                currentUserId, targetUserId);
        }

        public List<UserSummaryResponse> findSuggestedCreators(long currentUserId) {
                String sql = """
                                SELECT u.id, u.display_name, u.email, u.picture_url, u.username,
                                            COALESCE(follower_counts.followers_count, 0) AS followers_count,
                                       EXISTS(SELECT 1 FROM user_follows uf2 WHERE uf2.follower_id = ? AND uf2.following_id = u.id) AS is_following
                                FROM users u
                                    LEFT JOIN (
                                         SELECT following_id, COUNT(*) AS followers_count
                                         FROM user_follows
                                         GROUP BY following_id
                                    ) follower_counts ON follower_counts.following_id = u.id
                                WHERE u.id != ?
                                    ORDER BY follower_counts.followers_count DESC NULLS LAST, u.created_at DESC
                                LIMIT 20
                                """;
                return jdbcTemplate.query(sql, (rs, rowNum) -> new UserSummaryResponse(
                                rs.getLong("id"),
                                rs.getString("display_name"),
                                rs.getString("email"),
                                rs.getString("picture_url"),
                                rs.getString("username"),
                                rs.getLong("followers_count"),
                                rs.getBoolean("is_following")),
                                currentUserId, currentUserId);
        }

        public List<VisualItemResponse> findAllCreations() {
                String sql = """
                                    SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                                            v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by, v.share_key,
                                          u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url,
                                          (SELECT COUNT(*) FROM pin_likes item_likes WHERE item_likes.visual_item_id = v.id) AS like_count
                                FROM visual_items v
                                LEFT JOIN users u ON u.id = v.uploaded_by
                                ORDER BY v.id DESC
                                """;
                return jdbcTemplate.query(sql, visualItemRowMapper);
        }
}
