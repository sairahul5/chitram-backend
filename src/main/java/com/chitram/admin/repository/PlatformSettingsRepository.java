package com.chitram.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PlatformSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlatformSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isEnabled(String key) {
        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT enabled FROM app_settings WHERE setting_key = ?",
                Boolean.class,
                key);
        return enabled == null || enabled;
    }

    public boolean areRecommendationsEnabled() {
        return isEnabled("recommendations_enabled");
    }

    public void setRecommendationsEnabled(boolean enabled) {
        setEnabled("recommendations_enabled", enabled);
    }

    public Map<String, Boolean> findAll() {
        return jdbcTemplate.query("SELECT setting_key, enabled FROM app_settings ORDER BY setting_key", resultSet -> {
            Map<String, Boolean> values = new LinkedHashMap<>();
            while (resultSet.next()) {
                values.put(resultSet.getString("setting_key"), resultSet.getBoolean("enabled"));
            }
            return values;
        });
    }

    public void setEnabled(String key, boolean enabled) {
        jdbcTemplate.update(
                "UPDATE app_settings SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?",
                enabled,
                key);
    }

    public int getSessionDurationDays() {
        List<String> values = jdbcTemplate.query(
                "SELECT setting_value FROM app_settings WHERE setting_key = ?",
                (resultSet, rowNumber) -> resultSet.getString("setting_value"),
                "session_duration_days");
        if (values.isEmpty() || values.get(0) == null) {
            return 30;
        }
        try {
            return Integer.parseInt(values.get(0));
        } catch (NumberFormatException exception) {
            return 30;
        }
    }

    public void setSessionDurationDays(int days) {
        jdbcTemplate.update("""
                INSERT INTO app_settings (setting_key, enabled, setting_value, updated_at)
                VALUES (?, TRUE, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (setting_key) DO UPDATE SET
                    setting_value = EXCLUDED.setting_value,
                    updated_at = CURRENT_TIMESTAMP
                """, "session_duration_days", Integer.toString(days));
    }
}
