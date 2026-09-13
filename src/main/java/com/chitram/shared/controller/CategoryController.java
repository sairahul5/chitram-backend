package com.chitram.shared.controller;

import com.chitram.admin.service.AdminPanelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final AdminPanelService adminPanelService;

    public CategoryController(AdminPanelService adminPanelService) {
        this.adminPanelService = adminPanelService;
    }

    @GetMapping
    public List<String> getEnabledCategories() {
        return adminPanelService.getEnabledCategoryNames();
    }
}