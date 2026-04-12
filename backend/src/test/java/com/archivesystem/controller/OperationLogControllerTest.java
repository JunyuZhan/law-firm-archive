package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class OperationLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private OperationLogController operationLogController;

    private OperationLog testLog;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operationLogController).build();

        testLog = new OperationLog();
        testLog.setId(1L);
        testLog.setObjectType("ARCHIVE");
        testLog.setObjectId("ARC-001");
        testLog.setOperationType("CREATE");
        testLog.setOperatorId(1L);
        testLog.setOperatorName("admin");
        testLog.setOperatedAt(LocalDateTime.now());
        testLog.setOperationDesc("创建档案");
    }

    @Test
    void testQuery() throws Exception {
        PageResult<OperationLog> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testLog));

        when(operationLogService.query(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/operation-logs")
                        .param("keyword", "创建")
                        .param("objectType", "ARCHIVE")
                        .param("operationType", "CREATE")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].operationType").value("CREATE"));
    }

    @Test
    void testQuery_WithDateRange() throws Exception {
        PageResult<OperationLog> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testLog));

        when(operationLogService.query(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/operation-logs")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(operationLogService).query(any(), any(), any(), any(),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)), eq(1), eq(20));
    }

    @Test
    void testGetByArchive() throws Exception {
        when(operationLogService.getByArchiveId(1L)).thenReturn(Arrays.asList(testLog));

        mockMvc.perform(get("/operation-logs/archive/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].objectType").value("ARCHIVE"));
    }

    @Test
    void testGetByObject() throws Exception {
        when(operationLogService.getByObject("ARCHIVE", "ARC-001")).thenReturn(Arrays.asList(testLog));

        mockMvc.perform(get("/operation-logs/object/ARCHIVE/ARC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].objectId").value("ARC-001"));
    }

    @Test
    void testGetStatistics() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("CREATE", 100L);
        stats.put("UPDATE", 50L);
        stats.put("DELETE", 10L);

        when(operationLogService.getOperationStatistics(any(), any())).thenReturn(stats);

        mockMvc.perform(get("/operation-logs/statistics")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.CREATE").value(100));
    }

    @Test
    void testGetStatistics_NoDateRange() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL", 200L);

        when(operationLogService.getOperationStatistics(isNull(), isNull())).thenReturn(stats);

        mockMvc.perform(get("/operation-logs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testExportLogs() throws Exception {
        byte[] csvData = "id,objectType,operationType\n1,ARCHIVE,CREATE".getBytes();

        when(operationLogService.exportLogs(any(), any(), any(), any(), any())).thenReturn(csvData);

        mockMvc.perform(get("/operation-logs/export")
                        .param("objectType", "ARCHIVE")
                        .param("operationType", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void testExportLogs_WithDateRange() throws Exception {
        byte[] csvData = "id,objectType,operationType\n1,ARCHIVE,CREATE".getBytes();

        when(operationLogService.exportLogs(any(), any(), any(), any(), any())).thenReturn(csvData);

        mockMvc.perform(get("/operation-logs/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));

        verify(operationLogService).exportLogs(isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void testExportLogs_AllFilters() throws Exception {
        byte[] csvData = "data".getBytes();

        when(operationLogService.exportLogs(eq("ARCHIVE"), eq("CREATE"), eq(1L), any(), any()))
                .thenReturn(csvData);

        mockMvc.perform(get("/operation-logs/export")
                        .param("objectType", "ARCHIVE")
                        .param("operationType", "CREATE")
                        .param("operatorId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
    }
}
