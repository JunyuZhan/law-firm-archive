package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.service.impl.OperationLogServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    private OperationLog testLog;

    @BeforeEach
    void setUp() {
        testLog = OperationLog.builder()
                .id(1L)
                .objectType("ARCHIVE")
                .objectId("ARC-20260213-0001")
                .archiveId(100L)
                .operationType(OperationLog.OP_CREATE)
                .operationDesc("创建档案")
                .operatorId(1L)
                .operatorName("测试用户")
                .operatorIp("192.168.1.1")
                .operatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testLog_OperationLog() {
        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        assertDoesNotThrow(() -> operationLogService.log(testLog));

        verify(operationLogMapper).insert(any(OperationLog.class));
    }

    @Test
    void testLog_WithoutOperatedAt() {
        OperationLog logWithoutTime = OperationLog.builder()
                .objectType("ARCHIVE")
                .objectId("123")
                .operationType(OperationLog.OP_VIEW)
                .build();

        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        assertDoesNotThrow(() -> operationLogService.log(logWithoutTime));

        verify(operationLogMapper).insert(argThat(log -> log.getOperatedAt() != null));
    }

    @Test
    void testLog_SimpleParams() {
        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        assertDoesNotThrow(() -> 
            operationLogService.log("ARCHIVE", "123", 100L, OperationLog.OP_VIEW, "查看档案"));

        verify(operationLogMapper).insert(argThat(log -> 
            "ARCHIVE".equals(log.getObjectType()) &&
            "123".equals(log.getObjectId()) &&
            OperationLog.OP_VIEW.equals(log.getOperationType())));
    }

    @Test
    void testLog_WithDetail() {
        Map<String, Object> detail = new HashMap<>();
        detail.put("oldValue", "旧值");
        detail.put("newValue", "新值");

        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        assertDoesNotThrow(() -> 
            operationLogService.log("ARCHIVE", "123", 100L, OperationLog.OP_UPDATE, "更新档案", detail));

        verify(operationLogMapper).insert(argThat(log -> 
            log.getOperationDetail() != null &&
            log.getOperationDetail().containsKey("oldValue")));
    }

    @Test
    void testQuery_NoFilters() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            null, null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testQuery_WithKeyword() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            "创建", null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testQuery_WithObjectType() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            null, "ARCHIVE", null, null, null, null, 1, 10);

        assertNotNull(result);
    }

    @Test
    void testQuery_WithOperationType() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            null, null, OperationLog.OP_CREATE, null, null, null, 1, 10);

        assertNotNull(result);
    }

    @Test
    void testQuery_WithOperatorId() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            null, null, null, 1L, null, null, 1, 10);

        assertNotNull(result);
    }

    @Test
    void testQuery_WithDateRange() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            null, null, null, null, 
            LocalDate.now().minusDays(7), LocalDate.now(), 
            1, 10);

        assertNotNull(result);
    }

    @Test
    void testQuery_AllFilters() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            "档案", "ARCHIVE", OperationLog.OP_CREATE, 1L, 
            LocalDate.now().minusDays(7), LocalDate.now(), 
            1, 10);

        assertNotNull(result);
    }

    @Test
    void testGetByArchiveId() {
        when(operationLogMapper.selectByArchiveId(100L)).thenReturn(Arrays.asList(testLog));

        List<OperationLog> result = operationLogService.getByArchiveId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ARCHIVE", result.get(0).getObjectType());
    }

    @Test
    void testGetByArchiveId_Empty() {
        when(operationLogMapper.selectByArchiveId(999L)).thenReturn(Arrays.asList());

        List<OperationLog> result = operationLogService.getByArchiveId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetByObject() {
        when(operationLogMapper.selectByObject("ARCHIVE", "123")).thenReturn(Arrays.asList(testLog));

        List<OperationLog> result = operationLogService.getByObject("ARCHIVE", "123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOperationStatistics_NoDateFilter() {
        when(operationLogMapper.selectCount(any())).thenReturn(10L);

        Map<String, Long> result = operationLogService.getOperationStatistics(null, null);

        assertNotNull(result);
        // 检查所有操作类型都有统计
        assertTrue(result.containsKey(OperationLog.OP_CREATE));
        assertTrue(result.containsKey(OperationLog.OP_UPDATE));
        assertTrue(result.containsKey(OperationLog.OP_DELETE));
        assertTrue(result.containsKey(OperationLog.OP_VIEW));
        assertTrue(result.containsKey(OperationLog.OP_DOWNLOAD));
    }

    @Test
    void testGetOperationStatistics_WithDateRange() {
        when(operationLogMapper.selectCount(any())).thenReturn(5L);

        Map<String, Long> result = operationLogService.getOperationStatistics(
            LocalDate.now().minusDays(30), LocalDate.now());

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testExportLogs_NoFilters() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(testLog));

        byte[] result = operationLogService.exportLogs(null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String content = new String(result);
        assertTrue(content.contains("ID,对象类型"));
        assertTrue(content.contains("ARCHIVE"));
    }

    @Test
    void testExportLogs_WithFilters() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(testLog));

        byte[] result = operationLogService.exportLogs(
            "ARCHIVE", OperationLog.OP_CREATE, 1L, 
            LocalDate.now().minusDays(7), LocalDate.now());

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testExportLogs_EmptyResult() {
        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList());

        byte[] result = operationLogService.exportLogs(null, null, null, null, null);

        assertNotNull(result);
        // 即使没有数据，也应该有 CSV header
        String content = new String(result);
        assertTrue(content.contains("ID,对象类型"));
    }

    @Test
    void testExportLogs_WithSpecialCharacters() {
        OperationLog logWithQuotes = OperationLog.builder()
                .id(2L)
                .objectType("ARCHIVE")
                .objectId("123")
                .operationType(OperationLog.OP_UPDATE)
                .operationDesc("更新字段\"title\"的值")
                .operatorId(1L)
                .operatorName("测试用户")
                .operatedAt(LocalDateTime.now())
                .build();

        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(logWithQuotes));

        byte[] result = operationLogService.exportLogs(null, null, null, null, null);

        assertNotNull(result);
        String content = new String(result);
        // 双引号应该被转义
        assertTrue(content.contains("\"\""));
    }

    @Test
    void testExportLogs_WithNullFields() {
        OperationLog logWithNulls = OperationLog.builder()
                .id(3L)
                .objectType(null)
                .objectId(null)
                .operationType(null)
                .operationDesc(null)
                .operatorId(null)
                .operatorName(null)
                .operatorIp(null)
                .operatedAt(null)
                .build();

        when(operationLogMapper.selectList(any())).thenReturn(Arrays.asList(logWithNulls));

        byte[] result = operationLogService.exportLogs(null, null, null, null, null);

        assertNotNull(result);
        // 不应该抛出 NPE
    }

    @Test
    void testQuery_EmptyResult() {
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList());
        page.setTotal(0);

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<OperationLog> result = operationLogService.query(
            "不存在的关键词", null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void testGetOperationStatistics_ZeroCounts() {
        when(operationLogMapper.selectCount(any())).thenReturn(0L);

        Map<String, Long> result = operationLogService.getOperationStatistics(null, null);

        assertNotNull(result);
        for (Long count : result.values()) {
            assertEquals(0L, count);
        }
    }
}
