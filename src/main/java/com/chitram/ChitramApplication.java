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
