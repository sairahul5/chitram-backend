package com.chitram;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class ChitramApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitramApplication.class, args);
    }

    @Bean
    CommandLineRunner verifyDatabaseConnection(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS saved_pins (
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, visual_item_id)
                        )
                        """);
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS pin_likes (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uq_pin_likes_user_pin UNIQUE (user_id, visual_item_id)
                        )
                        """);
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pin_likes_user ON pin_likes (user_id)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pin_likes_pin ON pin_likes (visual_item_id)");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS user_interactions (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
                            interaction_type VARCHAR(30) NOT NULL,
                            duration_ms BIGINT,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                jdbcTemplate.execute(
                        "ALTER TABLE user_interactions ADD COLUMN IF NOT EXISTS duration_ms BIGINT");
                jdbcTemplate.execute(
                        "ALTER TABLE user_interactions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS user_interests (
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            category VARCHAR(120) NOT NULL,
                            score DOUBLE PRECISION NOT NULL DEFAULT 0,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, category)
                        )
                        """);
                jdbcTemplate.execute(
                        "ALTER TABLE user_interests ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION NOT NULL DEFAULT 0");
                jdbcTemplate.execute(
                        "ALTER TABLE user_interests ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP");
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS uq_user_interests_user_category ON user_interests (user_id, category)");
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_user_interactions_user_pin ON user_interactions (user_id, visual_item_id)");
                jdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_user_interactions_pin ON user_interactions (visual_item_id)");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS app_settings (
                            setting_key VARCHAR(120) PRIMARY KEY,
                            enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                                        setting_value VARCHAR(120),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                                jdbcTemplate.execute("ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS setting_value VARCHAR(120)");
                jdbcTemplate.update(
                        "INSERT INTO app_settings (setting_key, enabled) VALUES (?, TRUE) ON CONFLICT (setting_key) DO NOTHING",
                        "recommendations_enabled");
                jdbcTemplate.update(
                        "INSERT INTO app_settings (setting_key, enabled, setting_value) VALUES (?, TRUE, ?) ON CONFLICT (setting_key) DO UPDATE SET setting_value = COALESCE(app_settings.setting_value, EXCLUDED.setting_value)",
                        "session_duration_days", "30");
                for (String setting : new String[] { "registration_enabled", "image_uploads_enabled",
                        "public_profiles_enabled" }) {
                    jdbcTemplate.update(
                            "INSERT INTO app_settings (setting_key, enabled) VALUES (?, TRUE) ON CONFLICT (setting_key) DO NOTHING",
                            setting);
                }
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS admin_activity_log (
                            id BIGSERIAL PRIMARY KEY,
                            admin_name VARCHAR(160) NOT NULL,
                            action VARCHAR(120) NOT NULL,
                            target VARCHAR(240),
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                jdbcTemplate.execute(
                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
                jdbcTemplate.execute(
                        "ALTER TABLE visual_items ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(120) NOT NULL UNIQUE,
                            description VARCHAR(500),
                            enabled BOOLEAN NOT NULL DEFAULT TRUE
                        )
                        """);
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS reports (
                            id BIGSERIAL PRIMARY KEY,
                            visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
                            reported_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                            reason VARCHAR(120) NOT NULL,
                            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS target_type VARCHAR(20) NOT NULL DEFAULT 'PIN'");
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS target_id BIGINT");
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS reporter_id BIGINT REFERENCES users(id) ON DELETE SET NULL");
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS description VARCHAR(1000)");
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ");
                jdbcTemplate.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL");
                jdbcTemplate.execute("ALTER TABLE reports ALTER COLUMN visual_item_id DROP NOT NULL");
                jdbcTemplate.execute("UPDATE reports SET target_id = visual_item_id, reporter_id = reported_by WHERE target_id IS NULL");
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                System.out.println("Chitram database connection successful");
            } catch (DataAccessException exception) {
                Throwable cause = exception.getMostSpecificCause();
                String message = cause.getMessage() == null ? exception.getMessage() : cause.getMessage();
                throw new IllegalStateException("Chitram database connection failed: " + message, exception);
            }
        };
    }
}
