package com.chitram.integration;

import com.chitram.admin.repository.AdminMetricsRepository;
import com.chitram.admin.repository.AdminUserRepository;
import com.chitram.admin.repository.CategoryRepository;
import com.chitram.shared.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminReportRepositoryIntegrationTest extends PostgresRepositoryIntegrationTest {

    @Autowired private ReportRepository reportRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AdminMetricsRepository metricsRepository;
    @Autowired private AdminUserRepository adminUserRepository;

    @Test
    void shouldFilterAndUpdateReports() {
        long reporterId = insertUser("reporter@example.com", "reporter");
        long creatorId = insertUser("reported@example.com", "reported");
        long pinId = insertPin(creatorId, "reported-pin", "Nature", "APPROVED");

        reportRepository.insert("PIN", pinId, reporterId, "SPAM", "duplicate content");
        assertTrue(reportRepository.hasPendingReport("PIN", pinId, reporterId));
        assertEquals(1, reportRepository.findFiltered("PENDING", "PIN").size());

        reportRepository.setStatus(1L, "RESOLVED", "reporter@example.com");
        assertFalse(reportRepository.hasPendingReport("PIN", pinId, reporterId));
        assertEquals(1, reportRepository.findFiltered("RESOLVED", "PIN").size());
    }

    @Test
    void shouldCreateAndToggleCategories() {
        categoryRepository.create("Nature", "nature category");
        assertEquals(1, categoryRepository.findAll().size());
        assertEquals(1, categoryRepository.findEnabledNames().size());

        long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = 'Nature'", Long.class);
        categoryRepository.setEnabled(categoryId, false);
        assertEquals(0, categoryRepository.findEnabledNames().size());
        categoryRepository.delete(categoryId);
        assertEquals(0, categoryRepository.findAll().size());
    }

    @Test
    void shouldReturnAdminMetricsAndRoleState() {
        long userId = insertUser("admin@example.com", "admin-user");
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'ADMIN'", Long.class);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);

        assertTrue(adminUserRepository.isAdmin("admin@example.com"));
        assertFalse(adminUserRepository.isAdmin("missing@example.com"));
        assertEquals(1L, metricsRepository.countUsers());
    }
}
