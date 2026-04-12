package com.archivesystem.controller;

import com.archivesystem.service.ReportService;
import com.archivesystem.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private StatisticsController statisticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statisticsController).build();
    }

    @Test
    void testGetOverview() throws Exception {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalArchives", 1000L);
        overview.put("totalFiles", 5000L);
        overview.put("totalBorrows", 200L);

        when(statisticsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalArchives").value(1000));
    }

    @Test
    void testCountByType() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> typeCount = new HashMap<>();
        typeCount.put("type", "DOCUMENT");
        typeCount.put("count", 500L);
        result.add(typeCount);

        when(statisticsService.countByArchiveType()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].type").value("DOCUMENT"));
    }

    @Test
    void testCountByRetention() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> retentionCount = new HashMap<>();
        retentionCount.put("retention", "PERMANENT");
        retentionCount.put("count", 300L);
        result.add(retentionCount);

        when(statisticsService.countByRetentionPeriod()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-retention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].retention").value("PERMANENT"));
    }

    @Test
    void testCountByStatus() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusCount = new HashMap<>();
        statusCount.put("status", "NORMAL");
        statusCount.put("count", 800L);
        result.add(statusCount);

        when(statisticsService.countByStatus()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].status").value("NORMAL"));
    }

    @Test
    void testGetTrend_WithYear() throws Exception {
        List<Map<String, Object>> trend = new ArrayList<>();
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("month", "2026-01");
        monthData.put("count", 50L);
        trend.add(monthData);

        when(statisticsService.countByMonth(2026)).thenReturn(trend);

        mockMvc.perform(get("/statistics/trend")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].month").value("2026-01"));
    }

    @Test
    void testGetTrend_DefaultYear() throws Exception {
        List<Map<String, Object>> trend = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        when(statisticsService.countByMonth(currentYear)).thenReturn(trend);

        mockMvc.perform(get("/statistics/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(statisticsService).countByMonth(currentYear);
    }

    @Test
    void testGetBorrowStats() throws Exception {
        Map<String, Object> borrowStats = new HashMap<>();
        borrowStats.put("totalBorrows", 200L);
        borrowStats.put("activeBorrows", 50L);
        borrowStats.put("overdueBorrows", 5L);

        when(statisticsService.getBorrowStatistics()).thenReturn(borrowStats);

        mockMvc.perform(get("/statistics/borrow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalBorrows").value(200));
    }

    @Test
    void testGetStorageStats() throws Exception {
        Map<String, Object> storageStats = new HashMap<>();
        storageStats.put("totalSize", 1024000L);
        storageStats.put("usedSize", 512000L);

        when(statisticsService.getStorageStatistics()).thenReturn(storageStats);

        mockMvc.perform(get("/statistics/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalSize").value(1024000));
    }

    @Test
    void testGetScanBatchStats() throws Exception {
        List<Map<String, Object>> result = List.of(Map.of(
                "scanBatchNo", "SCAN-20260330-01",
                "fileCount", 5L,
                "archiveCount", 2L
        ));

        when(statisticsService.getScanBatchStatistics("SCAN-20260330")).thenReturn(result);

        mockMvc.perform(get("/statistics/scan-batches")
                        .param("keyword", "SCAN-20260330"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].scanBatchNo").value("SCAN-20260330-01"));
    }

    @Test
    void testExportOverview() throws Exception {
        doNothing().when(reportService).exportOverviewReport(any(), any());

        mockMvc.perform(get("/statistics/export/overview")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(reportService).exportOverviewReport(eq(2026), any());
    }

    @Test
    void testExportArchiveList() throws Exception {
        doNothing().when(reportService).exportArchiveList(any(), any());

        mockMvc.perform(get("/statistics/export/archives"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(reportService).exportArchiveList(any(), any());
    }

    @Test
    void testExportBorrowReport() throws Exception {
        doNothing().when(reportService).exportBorrowReport(any(), any(), any());

        mockMvc.perform(get("/statistics/export/borrow")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(reportService).exportBorrowReport(any(), any(), any());
    }

    @Test
    void testExportOperationLog() throws Exception {
        doNothing().when(reportService).exportOperationLogReport(any(), any(), any());

        mockMvc.perform(get("/statistics/export/operation-log")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(reportService).exportOperationLogReport(any(), any(), any());
    }
}
