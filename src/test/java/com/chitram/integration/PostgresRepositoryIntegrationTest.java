package com.chitram.integration;

import com.chitram.admin.repository.AdminActivityRepository;
import com.chitram.admin.repository.AdminMetricsRepository;
import com.chitram.admin.repository.AdminUserRepository;
import com.chitram.admin.repository.CategoryRepository;
import com.chitram.admin.repository.DatabaseInspectionRepository;
import com.chitram.admin.repository.PlatformSettingsRepository;
import com.chitram.admin.repository.VisualItemRepository;
import com.chitram.shared.repository.PinLikeRepository;
import com.chitram.shared.repository.ReportRepository;
import com.chitram.user.repository.UserPanelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({
        VisualItemRepository.class,
        UserPanelRepository.class,
        PinLikeRepository.class,
        ReportRepository.class,
        CategoryRepository.class,
        AdminMetricsRepository.class,
        AdminActivityRepository.class,
        AdminUserRepository.class,
        DatabaseInspectionRepository.class,
        PlatformSettingsRepository.class
})
public abstract class PostgresRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("chitram_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("integration-schema.sql");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void verifyIsolatedDatabase() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }

    protected long insertUser(String email, String username) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (email, display_name, username)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, email, username, username);
    }

    protected long insertPin(long userId, String title, String category, String moderationStatus) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO visual_items
                    (title, category, image_url, width, height, aspect_ratio, uploaded_by, moderation_status, share_key)
                VALUES (?, ?, ?, 800, 1000, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, title, category, "https://example.test/" + title,
                BigDecimal.valueOf(0.8), userId, moderationStatus, java.util.UUID.randomUUID().toString());
    }

    protected void agePin(long pinId, int secondsAgo) {
        jdbcTemplate.update("UPDATE visual_items SET created_at = CURRENT_TIMESTAMP - (? * INTERVAL '1 second') WHERE id = ?", secondsAgo, pinId);
    }
}
