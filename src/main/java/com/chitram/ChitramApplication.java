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
