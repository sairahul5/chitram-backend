package com.chitram.admin.service;

import com.chitram.admin.repository.AdminPanelRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizationService {

    private final AdminPanelRepository adminPanelRepository;

    public AdminAuthorizationService(AdminPanelRepository adminPanelRepository) {
        this.adminPanelRepository = adminPanelRepository;
    }

    public void requireAdmin(String email) {
        if (email == null || !adminPanelRepository.isAdmin(email)) {
            throw new AccessDeniedException("Admin role required");
        }
    }
}