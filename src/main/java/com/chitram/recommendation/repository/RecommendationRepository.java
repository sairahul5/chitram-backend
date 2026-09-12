package com.chitram.recommendation.repository;

import com.chitram.admin.dto.VisualItemResponse;
import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.model.RecommendationCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class RecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordInteraction(long userId, long pinId, InteractionType type, Long durationMs) {
        jdbcTemplate.update(
                "INSERT INTO user_interactions (user_id, visual_item_id, interaction_type, duration_ms) VALUES (?, ?, ?, ?)",
                userId, pinId, type.name(), durationMs);
    }

    public void updateInterest(long userId, long pinId, double weight) {
        List<String> categories = jdbcTemplate.queryForList(
                "SELECT category FROM visual_items WHERE id = ?",
                String.class,
                pinId);
        if (categories.isEmpty() || categories.get(0) == null || categories.get(0).isBlank()) {
            return;
        }

        jdbcTemplate.update(
                """
                        INSERT INTO user_interests (user_id, category, score, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT (user_id, category) DO UPDATE SET
                            score = user_interests.score + EXCLUDED.score,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                userId, categories.get(0), weight);
    }

    public List<RecommendationCandidate> findCandidates(long userId, int limit) {
        String sql = """
                WITH pin_stats AS (
                    SELECT visual_item_id,
                           COALESCE(SUM(CASE interaction_type
                               WHEN 'VIEW' THEN 1
                               WHEN 'LONG_VIEW' THEN 2
                               WHEN 'CLICK' THEN 3
                               WHEN 'LIKE' THEN 5
                               WHEN 'SAVE' THEN 8
                               WHEN 'SHARE' THEN 10
                               WHEN 'HIDE' THEN -10
                               WHEN 'REPORT' THEN -20
                               ELSE 0 END), 0) AS popularity_score
                    FROM user_interactions
                    GROUP BY visual_item_id
                ), like_stats AS (
                    SELECT visual_item_id, COUNT(*) AS like_count
                    FROM pin_likes
                    GROUP BY visual_item_id
                ), candidate_data AS (
                    SELECT v.id, v.title, v.category, v.image_url, v.image_path, v.width, v.height, v.aspect_ratio,
                           v.file_size, v.mime_type, v.description, v.created_at, v.uploaded_by,
                           u.display_name AS creator_name, u.username AS creator_username, u.picture_url AS creator_picture_url,
                           COALESCE(ls.like_count, 0) AS like_count,
                           EXISTS (
                               SELECT 1 FROM pin_likes current_like
                               WHERE current_like.visual_item_id = v.id AND current_like.user_id = ?
                           ) AS liked_by_current_user,
                           COALESCE(ui.score, 0) AS interest_score,
                           CASE WHEN EXISTS (
                               SELECT 1 FROM user_follows uf
                               WHERE uf.follower_id = ? AND uf.following_id = v.uploaded_by
                           ) THEN 1 ELSE 0 END AS creator_score,
                           COALESCE(ps.popularity_score, 0) AS popularity_score,
                           EXP(-0.02 * EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - v.created_at)) / 3600.0) AS freshness_score
                    FROM visual_items v
                    LEFT JOIN users u ON u.id = v.uploaded_by
                    LEFT JOIN user_interests ui ON ui.user_id = ? AND LOWER(ui.category) = LOWER(v.category)
                    LEFT JOIN pin_stats ps ON ps.visual_item_id = v.id
                    LEFT JOIN like_stats ls ON ls.visual_item_id = v.id
                                        WHERE v.moderation_status = 'APPROVED'
                                            AND NOT EXISTS (
                        SELECT 1 FROM user_interactions seen
                        WHERE seen.user_id = ?
                          AND seen.visual_item_id = v.id
                          AND seen.interaction_type IN ('VIEW', 'SAVE', 'HIDE', 'REPORT')
                    )
                ), scored_candidates AS (
                    SELECT candidate_data.*,
                           CASE WHEN MAX(interest_score) OVER () <= 0 THEN 0
                                ELSE GREATEST(0, LEAST(1, interest_score / MAX(interest_score) OVER ()))
                           END * 0.45
                           + creator_score * 0.20
                           + CASE WHEN MAX(popularity_score) OVER () <= 0 THEN 0
                                  ELSE GREATEST(0, LEAST(1, popularity_score / MAX(popularity_score) OVER ()))
                             END * 0.20
                           + freshness_score * 0.15 AS recommendation_score
                    FROM candidate_data
                )
                SELECT id, title, category, image_url, image_path, width, height, aspect_ratio,
                       file_size, mime_type, description, created_at, uploaded_by,
                       creator_name, creator_username, creator_picture_url, like_count,
                       liked_by_current_user, interest_score, creator_score, popularity_score, freshness_score
                FROM scored_candidates
                ORDER BY recommendation_score DESC, created_at DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new RecommendationCandidate(
                new VisualItemResponse(
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
                        resultSet.getBoolean("liked_by_current_user")),
                resultSet.getDouble("interest_score"),
                resultSet.getDouble("creator_score"),
                resultSet.getDouble("popularity_score"),
                resultSet.getDouble("freshness_score")),
                userId, userId, userId, userId, Math.max(1, Math.min(limit, 500)));
    }

    private static Instant toInstant(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
