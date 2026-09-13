package com.chitram.admin.service;

import com.chitram.admin.repository.AdminUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizationService {

    private final AdminUserRepository adminUserRepository;

    public AdminAuthorizationService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    public void requireAdmin(String email) {
        if (!hasAdminRole(email)) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    public boolean hasAdminRole(String email) {
        return email != null && adminUserRepository.isAdmin(email);
    }
}