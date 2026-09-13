package com.chitram.admin.repository;

import com.chitram.admin.dto.AdminActivityResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminActivityRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void log(String action, String target) {
        jdbcTemplate.update("INSERT INTO admin_activity_log (admin_name, action, target) VALUES (?, ?, ?)", "admin",
                action, target);
    }

    public List<AdminActivityResponse> findAll() {
        return jdbcTemplate.query(
                "SELECT action, target, created_at::text AS occurred_at FROM admin_activity_log ORDER BY created_at DESC LIMIT 50",
                (rs, row) -> new AdminActivityResponse(rs.getString("action"), rs.getString("target"),
                        rs.getString("occurred_at")));
    }

    public List<AdminActivityResponse> findRecent() {
        return jdbcTemplate.query(
                """
                        SELECT action, target, occurred_at::text FROM (
                            SELECT 'User joined' AS action, display_name AS target, created_at AS occurred_at FROM users
                            UNION ALL SELECT 'New pin uploaded', title, created_at FROM visual_items
                            UNION ALL SELECT 'Pin liked', CAST(visual_item_id AS TEXT), created_at FROM pin_likes
                            UNION ALL SELECT 'Pin reported', COALESCE(target_type, 'PIN') || ' #' || COALESCE(target_id, visual_item_id), created_at FROM reports
                            UNION ALL SELECT 'Pin saved', CAST(visual_item_id AS TEXT), created_at FROM saved_pins
                            UNION ALL SELECT action, COALESCE(target, 'Platform'), created_at FROM admin_activity_log
                        ) activity
                        ORDER BY occurred_at DESC LIMIT 8
                        """,
                (rs, row) -> new AdminActivityResponse(rs.getString("action"), rs.getString("target"),
                        rs.getString("occurred_at")));
    }
}
