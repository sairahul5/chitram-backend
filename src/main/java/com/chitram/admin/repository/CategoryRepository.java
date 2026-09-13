package com.chitram.admin.repository;

import com.chitram.admin.dto.AdminCategoryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdminCategoryResponse> findAll() {
        return jdbcTemplate.query("SELECT id, name, description, enabled FROM categories ORDER BY name",
                (rs, row) -> new AdminCategoryResponse(rs.getLong("id"), rs.getString("name"),
                        rs.getString("description"), rs.getBoolean("enabled")));
    }

    public List<String> findEnabledNames() {
        return jdbcTemplate.query("SELECT name FROM categories WHERE enabled = TRUE ORDER BY name",
                (rs, row) -> rs.getString("name"));
    }

    public void create(String name, String description) {
        jdbcTemplate.update("INSERT INTO categories (name, description) VALUES (?, ?)", name.trim(), description);
    }

    public void setEnabled(long id, boolean enabled) {
        jdbcTemplate.update("UPDATE categories SET enabled = ? WHERE id = ?", enabled, id);
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
    }
}
