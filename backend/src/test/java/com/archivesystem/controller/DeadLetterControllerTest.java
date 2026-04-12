package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.DeadLetterRecord;
import com.archivesystem.service.DeadLetterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeadLetterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeadLetterService deadLetterService;

    @InjectMocks
    private DeadLetterController deadLetterController;

    private ObjectMapper objectMapper;
    private DeadLetterRecord testRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deadLetterController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testRecord = new DeadLetterRecord();
        testRecord.setId(1L);
        testRecord.setMessageId("msg-001");
        testRecord.setQueueName("archive.receive.queue");
        testRecord.setMessageBody("{\"archiveId\":1}");
        testRecord.setStatus(DeadLetterRecord.STATUS_PENDING);
        testRecord.setRetryCount(0);
        testRecord.setMaxRetries(3);
        testRecord.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testList() throws Exception {
        when(deadLetterService.getList(any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 20, 1, Collections.singletonList(testRecord)));

        mockMvc.perform(get("/dead-letters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testListWithFilters() throws Exception {
        when(deadLetterService.getList(eq("PENDING"), eq("archive.receive.queue"), eq(1), eq(10)))
                .thenReturn(PageResult.of(1, 10, 1, Collections.singletonList(testRecord)));

        mockMvc.perform(get("/dead-letters")
                        .param("status", "PENDING")
                        .param("queueName", "archive.receive.queue")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById() throws Exception {
        when(deadLetterService.getById(1L)).thenReturn(testRecord);

        mockMvc.perform(get("/dead-letters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testGetStatistics() throws Exception {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("PENDING", 5);
        stats.put("SUCCESS", 10);
        stats.put("FAILED", 2);
        
        when(deadLetterService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/dead-letters/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.PENDING").value(5))
                .andExpect(jsonPath("$.data.SUCCESS").value(10));
    }

    @Test
    void testRetry() throws Exception {
        when(deadLetterService.retry(1L)).thenReturn(true);

        mockMvc.perform(post("/dead-letters/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testRetryFailed() throws Exception {
        when(deadLetterService.retry(1L)).thenReturn(false);

        mockMvc.perform(post("/dead-letters/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RETRY_FAILED"));
    }

    @Test
    void testBatchRetry() throws Exception {
        when(deadLetterService.batchRetry(any(Long[].class))).thenReturn(3);

        mockMvc.perform(post("/dead-letters/batch-retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void testIgnore() throws Exception {
        doNothing().when(deadLetterService).ignore(1L, "测试忽略");

        mockMvc.perform(post("/dead-letters/1/ignore")
                        .param("remark", "测试忽略"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(deadLetterService).ignore(1L, "测试忽略");
    }

    @Test
    void testBatchIgnore() throws Exception {
        when(deadLetterService.batchIgnore(any(Long[].class), eq("批量忽略"))).thenReturn(2);

        mockMvc.perform(post("/dead-letters/batch-ignore")
                        .param("remark", "批量忽略")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void testTriggerAutoRetry() throws Exception {
        when(deadLetterService.autoRetry(10)).thenReturn(5);

        mockMvc.perform(post("/dead-letters/auto-retry")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(5));
    }
}
