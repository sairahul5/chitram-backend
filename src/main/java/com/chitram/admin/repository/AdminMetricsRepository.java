package com.chitram.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminMetricsRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        return count("SELECT COUNT(*) FROM users");
    }

    public long countUsersCreatedThisWeek() {
        return count("SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'");
    }

    public long countActiveUsers() {
        return count(
                "SELECT COUNT(DISTINCT user_id) FROM user_interactions WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'");
    }

    public long countPinsCreatedToday() {
        return count("SELECT COUNT(*) FROM visual_items WHERE created_at >= CURRENT_DATE");
    }

    public long countLikesToday() {
        return count("SELECT COUNT(*) FROM pin_likes WHERE created_at >= CURRENT_DATE");
    }

    public long countStorageBytes() {
        return count("SELECT COALESCE(SUM(file_size), 0) FROM visual_items");
    }

    public java.util.List<String> findAvailableAreas() {
        return java.util.List.of("moderation", "platform-settings", "usage-overview");
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
