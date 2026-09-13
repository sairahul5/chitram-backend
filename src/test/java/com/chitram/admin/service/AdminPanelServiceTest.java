package com.chitram.admin.service;

import com.chitram.admin.repository.AdminActivityRepository;
import com.chitram.admin.repository.AdminMetricsRepository;
import com.chitram.admin.repository.AdminUserRepository;
import com.chitram.admin.repository.CategoryRepository;
import com.chitram.admin.repository.DatabaseInspectionRepository;
import com.chitram.admin.repository.PlatformSettingsRepository;
import com.chitram.admin.repository.VisualItemRepository;
import com.chitram.shared.repository.ReportRepository;
import com.chitram.websocket.AdminEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AdminPanelServiceTest {

    @Mock AdminMetricsRepository metrics;
    @Mock AdminActivityRepository activity;
    @Mock AdminUserRepository users;
    @Mock CategoryRepository categories;
    @Mock ReportRepository reports;
    @Mock DatabaseInspectionRepository database;
    @Mock PlatformSettingsRepository settings;
    @Mock VisualItemRepository visualItems;
    @Mock AdminEventPublisher publisher;

    private AdminPanelService service;

    @BeforeEach
    void setUp() {
        service = new AdminPanelService(metrics, activity, users, categories, reports, database, settings, visualItems, publisher);
        lenient().when(categories.findAll()).thenReturn(List.of());
        lenient().when(reports.findAll()).thenReturn(List.of());
        lenient().when(visualItems.findVisualItems(null, 12)).thenReturn(List.of());
        lenient().when(settings.findAll()).thenReturn(Map.of());
        lenient().when(activity.findAll()).thenReturn(List.of());
        lenient().when(settings.getSessionDurationDays()).thenReturn(30);
        lenient().when(database.findExistingTables()).thenReturn(List.of());
        lenient().when(activity.findRecent()).thenReturn(List.of());
        lenient().when(metrics.countUsers()).thenReturn(0L);
        lenient().when(metrics.countPinsCreatedToday()).thenReturn(0L);
        lenient().when(metrics.countLikesToday()).thenReturn(0L);
        lenient().when(metrics.countStorageBytes()).thenReturn(0L);
        lenient().when(database.tableExists(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    }

    @Test
    void shouldRejectInvalidAccountStatus() {
        assertThrows(IllegalArgumentException.class, () -> service.setAccountStatus(4L, "DELETED"));
    }

    @Test
    void shouldUpdateValidAccountStatus() {
        service.setAccountStatus(4L, "SUSPENDED");

        verify(users).setAccountStatus(4L, "SUSPENDED");
    }

    @Test
    void shouldRejectInvalidCategoryName() {
        assertThrows(IllegalArgumentException.class, () -> service.createCategory(" ", "description"));
    }

    @Test
    void shouldUpdatePlatformSettingAndLogActivity() {
        service.setPlatformSetting("image_uploads_enabled", false);

        verify(settings).setEnabled("image_uploads_enabled", false);
        verify(activity).log("DISABLE_SETTING", "image_uploads_enabled");
    }

    @Test
    void shouldRejectUnsupportedPlatformSetting() {
        assertThrows(IllegalArgumentException.class, () -> service.setPlatformSetting("unknown", true));
    }
}
