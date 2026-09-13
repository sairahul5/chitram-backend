package com.chitram.admin.service;

import com.chitram.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock AdminUserRepository adminUserRepository;

    @Test
    void shouldAllowAdmin() {
        AdminAuthorizationService service = new AdminAuthorizationService(adminUserRepository);
        when(adminUserRepository.isAdmin("admin@example.com")).thenReturn(true);

        assertTrue(service.hasAdminRole("admin@example.com"));
        service.requireAdmin("admin@example.com");
    }

    @Test
    void shouldRejectNonAdminAndMissingEmail() {
        AdminAuthorizationService service = new AdminAuthorizationService(adminUserRepository);
        when(adminUserRepository.isAdmin("user@example.com")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.requireAdmin("user@example.com"));
        assertThrows(AccessDeniedException.class, () -> service.requireAdmin(null));
    }
}
