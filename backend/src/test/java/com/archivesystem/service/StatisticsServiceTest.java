package com.archivesystem.service;

import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private BorrowApplicationMapper borrowMapper;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetOverview() {
        when(archiveMapper.selectCount(any())).thenReturn(100L, 10L);
        when(digitalFileMapper.selectCount(any())).thenReturn(500L);
        when(borrowMapper.selectCount(any())).thenReturn(5L, 3L);

        Map<String, Object> result = statisticsService.getOverview();

        assertNotNull(result);
        assertEquals(100L, result.get("totalArchives"));
        assertEquals(10L, result.get("monthlyNew"));
        assertEquals(500L, result.get("totalFiles"));
        assertEquals(5L, result.get("borrowing"));
        assertEquals(3L, result.get("pendingApproval"));
    }

    @Test
    void testCountByArchiveType() {
        when(archiveMapper.countByArchiveType()).thenReturn(List.of(
                countRow("type", "DOCUMENT", 20L),
                countRow("type", "SCIENCE", 3L)
        ));

        List<Map<String, Object>> result = statisticsService.countByArchiveType();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 检查是否包含所有类型
        boolean hasDocument = result.stream()
                .anyMatch(m -> "DOCUMENT".equals(m.get("type")));
        assertTrue(hasDocument);
        
        // 每个类型都应该有 count
        for (Map<String, Object> item : result) {
            assertNotNull(item.get("type"));
            assertNotNull(item.get("name"));
            assertNotNull(item.get("count"));
        }
    }

    @Test
    void testCountByRetentionPeriod() {
        when(archiveMapper.countByRetentionPeriod()).thenReturn(List.of(
                countRow("period", "PERMANENT", 15L),
                countRow("period", "Y30", 2L)
        ));

        List<Map<String, Object>> result = statisticsService.countByRetentionPeriod();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 检查是否包含永久保管
        boolean hasPermanent = result.stream()
                .anyMatch(m -> "PERMANENT".equals(m.get("period")));
        assertTrue(hasPermanent);
        
        // 每个保管期限都应该有 count
        for (Map<String, Object> item : result) {
            assertNotNull(item.get("period"));
            assertNotNull(item.get("name"));
            assertNotNull(item.get("count"));
        }
    }

    @Test
    void testCountByStatus() {
        when(archiveMapper.countByStatus()).thenReturn(List.of(
                countRow("status", "RECEIVED", 25L),
                countRow("status", "STORED", 8L)
        ));

        List<Map<String, Object>> result = statisticsService.countByStatus();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 检查是否包含已接收状态
        boolean hasReceived = result.stream()
                .anyMatch(m -> "RECEIVED".equals(m.get("status")));
        assertTrue(hasReceived);
        
        // 检查是否包含已归档状态
        boolean hasStored = result.stream()
                .anyMatch(m -> "STORED".equals(m.get("status")));
        assertTrue(hasStored);
    }

    @Test
    void testCountByMonth() {
        when(archiveMapper.countByMonth(2026)).thenReturn(List.of(
                countRow("month", 1, 8L),
                countRow("month", 2, 5L)
        ));

        List<Map<String, Object>> result = statisticsService.countByMonth(2026);

        assertNotNull(result);
        assertEquals(12, result.size()); // 12个月
        
        // 验证月份从1到12
        for (int i = 0; i < 12; i++) {
            Map<String, Object> item = result.get(i);
            assertEquals(i + 1, item.get("month"));
            assertNotNull(item.get("monthName"));
            assertNotNull(item.get("count"));
        }
    }

    @Test
    void testGetBorrowStatistics() {
        when(borrowMapper.selectCount(any())).thenReturn(50L, 10L, 2L);

        Map<String, Object> result = statisticsService.getBorrowStatistics();

        assertNotNull(result);
        assertEquals(50L, result.get("totalBorrows"));
        assertEquals(10L, result.get("monthlyBorrows"));
        assertEquals(2L, result.get("overdue"));
    }

    @Test
    void testGetStorageStatistics() {
        when(digitalFileMapper.selectStorageSummary()).thenReturn(Map.of(
                "totalSize", 3L * 1024 * 1024,
                "fileCount", 3L
        ));

        Map<String, Object> result = statisticsService.getStorageStatistics();

        assertEquals(3L * 1024 * 1024, result.get("totalSize"));
        assertEquals(3L, result.get("fileCount"));
        assertEquals("3.00 MB", result.get("totalSizeFormatted"));
    }

    @Test
    void testGetStorageStatistics_EmptyFiles() {
        when(digitalFileMapper.selectStorageSummary()).thenReturn(new HashMap<>());

        Map<String, Object> result = statisticsService.getStorageStatistics();

        assertEquals(0L, result.get("totalSize"));
        assertEquals(0L, result.get("fileCount"));
        assertEquals("0 B", result.get("totalSizeFormatted"));
    }

    @Test
    void testCountByArchiveType_ZeroCounts() {
        when(archiveMapper.countByArchiveType()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = statisticsService.countByArchiveType();

        assertNotNull(result);
        for (Map<String, Object> item : result) {
            assertEquals(0L, item.get("count"));
        }
    }

    @Test
    void testGetOverview_AllZero() {
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(digitalFileMapper.selectCount(any())).thenReturn(0L);
        when(borrowMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> result = statisticsService.getOverview();

        assertNotNull(result);
        assertEquals(0L, result.get("totalArchives"));
        assertEquals(0L, result.get("totalFiles"));
    }

    @Test
    void testCountByMonth_LeapYear() {
        when(archiveMapper.countByMonth(2024)).thenReturn(List.of(
                countRow("month", 2, 5L)
        ));

        List<Map<String, Object>> result = statisticsService.countByMonth(2024);

        assertNotNull(result);
        assertEquals(12, result.size());
        
        // 验证2月份数据
        Map<String, Object> february = result.get(1);
        assertEquals(2, february.get("month"));
    }

    @Test
    void testStorageStatistics_LargeFiles() {
        when(digitalFileMapper.selectStorageSummary()).thenReturn(Map.of(
                "totalSize", 5L * 1024 * 1024 * 1024,
                "fileCount", 1L
        ));

        Map<String, Object> result = statisticsService.getStorageStatistics();

        assertEquals("5.00 GB", result.get("totalSizeFormatted"));
    }

    @Test
    void testStorageStatistics_SmallFiles() {
        when(digitalFileMapper.selectStorageSummary()).thenReturn(Map.of(
                "totalSize", 512L,
                "fileCount", 1L
        ));

        Map<String, Object> result = statisticsService.getStorageStatistics();

        assertEquals("512 B", result.get("totalSizeFormatted"));
    }

    @Test
    void testGetScanBatchStatistics() {
        when(digitalFileMapper.selectScanBatchSummaries("SCAN-20260330")).thenReturn(List.of(
                Map.of(
                        "scanBatchNo", "SCAN-20260330-01",
                        "fileCount", 8L,
                        "archiveCount", 3L,
                        "scannedFileCount", 8L,
                        "passedCount", 6L,
                        "pendingCount", 1L,
                        "failedCount", 1L,
                        "latestScanTime", "2026-03-30 09:30:00"
                )
        ));

        List<Map<String, Object>> result = statisticsService.getScanBatchStatistics("SCAN-20260330");

        assertEquals(1, result.size());
        assertEquals("SCAN-20260330-01", result.get(0).get("scanBatchNo"));
        assertEquals(8L, result.get(0).get("fileCount"));
        assertEquals(3L, result.get(0).get("archiveCount"));
        assertEquals(7L, result.get(0).get("reviewedCount"));
        assertEquals(100D, result.get(0).get("scanCoverageRate"));
        assertEquals(87.5D, result.get(0).get("reviewCompletionRate"));
        assertEquals(85.71428571428571D, result.get(0).get("passRate"));
    }

    private Map<String, Object> countRow(String key, Object value, long count) {
        Map<String, Object> row = new HashMap<>();
        row.put(key, value);
        row.put("count", count);
        return row;
    }
}
