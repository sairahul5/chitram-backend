package com.chitram.shared.api;

import com.chitram.shared.service.ReportService;
import com.chitram.user.entity.UserAccount;
import com.chitram.user.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final UserProfileService userProfileService;
    private final ReportService reportService;

    public ReportController(UserProfileService userProfileService, ReportService reportService) {
        this.userProfileService = userProfileService;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Void> report(@AuthenticationPrincipal OAuth2User principal,
            @RequestBody ReportRequest request) {
        UserAccount reporter = userProfileService.getCurrentUser(principal);
        reportService.submit(reporter, request.targetType(), request.targetId(), request.reason(),
                request.description());
        return ResponseEntity.accepted().build();
    }
}