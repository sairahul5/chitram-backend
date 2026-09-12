package com.chitram.shared.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PinLikeRepository {

    private final JdbcTemplate jdbcTemplate;

    public PinLikeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean pinExists(long pinId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visual_items WHERE id = ?",
                Integer.class,
                pinId);
        return count != null && count > 0;
    }

    public boolean insertLike(long userId, long pinId) {
        return jdbcTemplate.update(
                "INSERT INTO pin_likes (user_id, visual_item_id) VALUES (?, ?) ON CONFLICT (user_id, visual_item_id) DO NOTHING",
                userId,
                pinId) > 0;
    }

    public boolean deleteLike(long userId, long pinId) {
        return jdbcTemplate.update(
                "DELETE FROM pin_likes WHERE user_id = ? AND visual_item_id = ?",
                userId,
                pinId) > 0;
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
}
