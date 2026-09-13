package com.chitram.admin.repository;

import com.chitram.admin.dto.AdminUserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isAdmin(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = ? AND r.code = 'ADMIN'",
                Integer.class, email);
        return count != null && count > 0;
    }

    public List<AdminUserResponse> findUsers(String search) {
        String value = search == null ? "" : search.trim();
        return jdbcTemplate.query(
                """
                        SELECT u.id, u.email, u.display_name, u.picture_url, u.created_at::text, u.account_status,
                               COALESCE(string_agg(DISTINCT r.code, ', ' ORDER BY r.code), 'USER') AS role,
                               (SELECT COUNT(*) FROM visual_items v WHERE v.uploaded_by = u.id) AS pins,
                               (SELECT COUNT(*) FROM pin_likes pl WHERE pl.user_id = u.id) AS likes,
                               (SELECT COUNT(*) FROM user_follows uf WHERE uf.following_id = u.id) AS followers,
                               (SELECT COUNT(*) FROM user_follows uf WHERE uf.follower_id = u.id) AS following
                        FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id LEFT JOIN roles r ON r.id = ur.role_id
                        WHERE LOWER(u.display_name) LIKE LOWER(?) OR LOWER(u.email) LIKE LOWER(?) OR LOWER(COALESCE(u.username, '')) LIKE LOWER(?)
                        GROUP BY u.id ORDER BY u.created_at DESC
                        """,
                (rs, row) -> new AdminUserResponse(rs.getLong("id"), rs.getString("email"),
                        rs.getString("display_name"), rs.getString("picture_url"), rs.getString("role"),
                        rs.getString("created_at"), rs.getString("account_status"), rs.getLong("pins"),
                        rs.getLong("likes"), rs.getLong("followers"), rs.getLong("following")),
                "%" + value + "%", "%" + value + "%", "%" + value + "%");
    }

    public void setAccountStatus(long userId, String status) {
        jdbcTemplate.update("UPDATE users SET account_status = ? WHERE id = ?", status, userId);
    }

    public void delete(long userId) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    public void replaceRole(long userId, String role) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE code = ?", userId,
                role);
    }
}
