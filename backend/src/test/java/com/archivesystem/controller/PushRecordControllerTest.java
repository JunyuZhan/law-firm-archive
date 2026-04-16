package com.archivesystem.controller;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.GlobalExceptionHandler;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.service.AlertService;
import com.archivesystem.service.PushRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class PushRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PushRecordService pushRecordService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private PushRecordController pushRecordController;

    private PushRecord testRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pushRecordController)
                .setControllerAdvice(new GlobalExceptionHandler(alertService))
                .build();

        testRecord = new PushRecord();
        testRecord.setId(1L);
        testRecord.setTitle("测试推送");
        testRecord.setPushStatus(PushRecord.STATUS_FAILED);
        testRecord.setPushedAt(LocalDateTime.now());
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(pushRecordService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/push-records/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("推送记录不存在"));
    }

    @Test
    void testRetry_Success() throws Exception {
        doNothing().when(pushRecordService).retry(1L);

        mockMvc.perform(post("/push-records/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已重置为待处理状态"));
    }

    @Test
    void testRetry_InvalidStatus() throws Exception {
        doThrow(new BusinessException("400", "只有失败状态的推送记录才能重试"))
                .when(pushRecordService).retry(2L);

        mockMvc.perform(post("/push-records/2/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("只有失败状态的推送记录才能重试"));
    }

    @Test
    void testList_Success() throws Exception {
        IPage<PushRecord> page = new Page<>(1, 20);
        page.setRecords(List.of(testRecord));
        page.setTotal(1);
        when(pushRecordService.page(
                ArgumentMatchers.<Page<PushRecord>>any(),
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(page);

        mockMvc.perform(get("/push-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testStatistics_Success() throws Exception {
        when(pushRecordService.getStatistics()).thenReturn(Map.of("total", 1L));

        mockMvc.perform(get("/push-records/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
