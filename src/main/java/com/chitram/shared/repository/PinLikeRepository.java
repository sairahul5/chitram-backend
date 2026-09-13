package com.chitram.shared.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PinLikeRepository {

    private final JdbcTemplate jdbcTemplate;

    public PinLikeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LikeMutation insertLike(long userId, long pinId) {
        return jdbcTemplate.queryForObject(
                """
                        WITH pin AS (
                            SELECT id FROM visual_items WHERE id = ?
                        ), inserted AS (
                            INSERT INTO pin_likes (user_id, visual_item_id)
                            SELECT ?, id FROM pin
                            ON CONFLICT (user_id, visual_item_id) DO NOTHING
                            RETURNING visual_item_id
                        )
                        SELECT EXISTS(SELECT 1 FROM pin) AS pin_exists,
                               EXISTS(SELECT 1 FROM inserted) AS changed
                        """,
                (resultSet, rowNumber) -> new LikeMutation(
                        resultSet.getBoolean("pin_exists"),
                        resultSet.getBoolean("changed")),
                pinId, userId);
    }

    public LikeMutation deleteLike(long userId, long pinId) {
        return jdbcTemplate.queryForObject(
                """
                        WITH pin AS (
                            SELECT id FROM visual_items WHERE id = ?
                        ), deleted AS (
                            DELETE FROM pin_likes
                            WHERE user_id = ? AND visual_item_id IN (SELECT id FROM pin)
                            RETURNING visual_item_id
                        )
                        SELECT EXISTS(SELECT 1 FROM pin) AS pin_exists,
                               EXISTS(SELECT 1 FROM deleted) AS changed
                        """,
                (resultSet, rowNumber) -> new LikeMutation(
                        resultSet.getBoolean("pin_exists"),
                        resultSet.getBoolean("changed")),
                pinId, userId);
    }

    public long countLikes(long pinId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pin_likes WHERE visual_item_id = ?",
                Long.class,
                pinId);
        return count == null ? 0 : count;
    }

    public boolean isLikedByUser(long userId, long pinId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pin_likes WHERE user_id = ? AND visual_item_id = ?",
                Integer.class,
                userId,
                pinId);
        return count != null && count > 0;
    }

    public record LikeMutation(boolean pinExists, boolean changed) {
    }
}
