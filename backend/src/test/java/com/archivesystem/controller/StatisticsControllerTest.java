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
        overview.put("borrowing", 200L);
        overview.put("monthlyNew", 30L);
        overview.put("pendingApproval", 12L);

        when(statisticsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalArchives").value(1000))
                .andExpect(jsonPath("$.data.monthlyNew").value(30))
                .andExpect(jsonPath("$.data.borrowing").value(200))
                .andExpect(jsonPath("$.data.pendingApproval").value(12))
                .andExpect(jsonPath("$.data.totalBorrows").doesNotExist());
    }

    @Test
    void testCountByType() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> typeCount = new HashMap<>();
        typeCount.put("type", "DOCUMENT");
        typeCount.put("name", "文书档案");
        typeCount.put("count", 500L);
        result.add(typeCount);

        when(statisticsService.countByArchiveType()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].type").value("DOCUMENT"))
                .andExpect(jsonPath("$.data[0].name").value("文书档案"))
                .andExpect(jsonPath("$.data[0].count").value(500));
    }

    @Test
    void testCountByRetention() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> retentionCount = new HashMap<>();
        retentionCount.put("period", "PERMANENT");
        retentionCount.put("name", "永久");
        retentionCount.put("count", 300L);
        result.add(retentionCount);

        when(statisticsService.countByRetentionPeriod()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-retention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].period").value("PERMANENT"))
                .andExpect(jsonPath("$.data[0].name").value("永久"))
                .andExpect(jsonPath("$.data[0].count").value(300))
                .andExpect(jsonPath("$.data[0].retention").doesNotExist());
    }

    @Test
    void testCountByStatus() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusCount = new HashMap<>();
        statusCount.put("status", "NORMAL");
        statusCount.put("name", "正常");
        statusCount.put("count", 800L);
        result.add(statusCount);

        when(statisticsService.countByStatus()).thenReturn(result);

        mockMvc.perform(get("/statistics/by-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.data[0].name").value("正常"))
                .andExpect(jsonPath("$.data[0].count").value(800));
    }

    @Test
    void testGetTrend_WithYear() throws Exception {
        List<Map<String, Object>> trend = new ArrayList<>();
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("month", 1);
        monthData.put("monthName", "1月");
        monthData.put("count", 50L);
        trend.add(monthData);

        when(statisticsService.countByMonth(2026)).thenReturn(trend);

        mockMvc.perform(get("/statistics/trend")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].month").value(1))
                .andExpect(jsonPath("$.data[0].monthName").value("1月"))
                .andExpect(jsonPath("$.data[0].count").value(50));
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
        borrowStats.put("monthlyBorrows", 50L);
        borrowStats.put("overdue", 5L);

        when(statisticsService.getBorrowStatistics()).thenReturn(borrowStats);

        mockMvc.perform(get("/statistics/borrow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalBorrows").value(200))
                .andExpect(jsonPath("$.data.monthlyBorrows").value(50))
                .andExpect(jsonPath("$.data.overdue").value(5))
                .andExpect(jsonPath("$.data.activeBorrows").doesNotExist())
                .andExpect(jsonPath("$.data.overdueBorrows").doesNotExist());
    }

    @Test
    void testGetStorageStats() throws Exception {
        Map<String, Object> storageStats = new HashMap<>();
        storageStats.put("totalSize", 1024000L);
        storageStats.put("totalSizeFormatted", "1000 KB");
        storageStats.put("fileCount", 128L);

        when(statisticsService.getStorageStatistics()).thenReturn(storageStats);

        mockMvc.perform(get("/statistics/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.totalSize").value(1024000))
                .andExpect(jsonPath("$.data.totalSizeFormatted").value("1000 KB"))
                .andExpect(jsonPath("$.data.fileCount").value(128))
                .andExpect(jsonPath("$.data.usedSize").doesNotExist());
    }

    @Test
    void testGetScanBatchStats() throws Exception {
        Map<String, Object> batchStats = new HashMap<>();
        batchStats.put("scanBatchNo", "SCAN-20260330-01");
        batchStats.put("fileCount", 5L);
        batchStats.put("archiveCount", 2L);
        batchStats.put("scannedFileCount", 4L);
        batchStats.put("passedCount", 3L);
        batchStats.put("pendingCount", 1L);
        batchStats.put("failedCount", 1L);
        batchStats.put("reviewedCount", 4L);
        batchStats.put("scanCoverageRate", 80D);
        batchStats.put("reviewCompletionRate", 80D);
        batchStats.put("passRate", 75D);
        batchStats.put("latestScanTime", "2026-03-30T10:00:00");
        List<Map<String, Object>> result = List.of(batchStats);

        when(statisticsService.getScanBatchStatistics("SCAN-20260330")).thenReturn(result);

        mockMvc.perform(get("/statistics/scan-batches")
                        .param("keyword", "SCAN-20260330"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].scanBatchNo").value("SCAN-20260330-01"))
                .andExpect(jsonPath("$.data[0].passRate").value(75.0))
                .andExpect(jsonPath("$.data[0].reviewedCount").value(4))
                .andExpect(jsonPath("$.data[0].latestScanTime").value("2026-03-30T10:00:00"));
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
