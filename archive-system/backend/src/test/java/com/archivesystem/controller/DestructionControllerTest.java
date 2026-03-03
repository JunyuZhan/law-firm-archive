package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.service.DestructionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DestructionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DestructionService destructionService;

    @InjectMocks
    private DestructionController destructionController;

    private ObjectMapper objectMapper;
    private DestructionRecord testRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(destructionController).build();
        objectMapper = new ObjectMapper();

        testRecord = new DestructionRecord();
        testRecord.setId(1L);
        testRecord.setArchiveId(100L);
        testRecord.setDestructionReason("保管期限已到");
        testRecord.setStatus("PENDING");
    }

    @Test
    void testApply() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("archiveId", 100L);
        params.put("destructionReason", "保管期限已到");
        params.put("destructionMethod", "焚烧");

        when(destructionService.apply(anyLong(), anyString(), anyString())).thenReturn(testRecord);

        mockMvc.perform(post("/destructions/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("销毁申请已提交"));

        verify(destructionService).apply(100L, "保管期限已到", "焚烧");
    }

    @Test
    void testBatchApply() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("archiveIds", Arrays.asList(100L, 101L, 102L));
        params.put("destructionReason", "保管期限已到");
        params.put("destructionMethod", "焚烧");

        List<DestructionRecord> records = Arrays.asList(testRecord);
        when(destructionService.batchApply(anyList(), anyString(), anyString())).thenReturn(records);

        mockMvc.perform(post("/destructions/batch-apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("批量销毁申请已提交"));

        verify(destructionService).batchApply(anyList(), eq("保管期限已到"), eq("焚烧"));
    }

    @Test
    void testGetById() throws Exception {
        when(destructionService.getById(1L)).thenReturn(testRecord);

        mockMvc.perform(get("/destructions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(destructionService).getById(1L);
    }

    @Test
    void testGetList() throws Exception {
        PageResult<DestructionRecord> pageResult = new PageResult<>(1L, 20L, 1L, 1L, Collections.singletonList(testRecord));
        when(destructionService.getList(anyString(), anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/destructions")
                        .param("status", "PENDING")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).getList("PENDING", 1, 20);
    }

    @Test
    void testGetList_DefaultParams() throws Exception {
        PageResult<DestructionRecord> pageResult = new PageResult<>(1L, 20L, 0L, 0L, Collections.emptyList());
        when(destructionService.getList(isNull(), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/destructions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).getList(null, 1, 20);
    }

    @Test
    void testGetPendingList() throws Exception {
        PageResult<DestructionRecord> pageResult = new PageResult<>(1L, 20L, 1L, 1L, Collections.singletonList(testRecord));
        when(destructionService.getPendingList(anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/destructions/pending")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).getPendingList(1, 20);
    }

    @Test
    void testGetApprovedList() throws Exception {
        PageResult<DestructionRecord> pageResult = new PageResult<>(1L, 20L, 1L, 1L, Collections.singletonList(testRecord));
        when(destructionService.getApprovedList(anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/destructions/approved")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).getApprovedList(1, 20);
    }

    @Test
    void testGetByArchiveId() throws Exception {
        List<DestructionRecord> records = Collections.singletonList(testRecord);
        when(destructionService.getByArchiveId(100L)).thenReturn(records);

        mockMvc.perform(get("/destructions/archive/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray());

        verify(destructionService).getByArchiveId(100L);
    }

    @Test
    void testApprove() throws Exception {
        doNothing().when(destructionService).approve(anyLong(), anyString());

        mockMvc.perform(put("/destructions/1/approve")
                        .param("comment", "同意销毁"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("审批通过"));

        verify(destructionService).approve(1L, "同意销毁");
    }

    @Test
    void testApprove_NoComment() throws Exception {
        doNothing().when(destructionService).approve(anyLong(), isNull());

        mockMvc.perform(put("/destructions/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).approve(1L, null);
    }

    @Test
    void testReject() throws Exception {
        doNothing().when(destructionService).reject(anyLong(), anyString());

        mockMvc.perform(put("/destructions/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"不符合销毁条件\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已拒绝"));

        verify(destructionService).reject(1L, "不符合销毁条件");
    }

    @Test
    void testExecute() throws Exception {
        doNothing().when(destructionService).execute(anyLong(), anyString());

        mockMvc.perform(put("/destructions/1/execute")
                        .param("remarks", "已完成销毁"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("销毁已执行"));

        verify(destructionService).execute(1L, "已完成销毁");
    }

    @Test
    void testExecute_NoRemarks() throws Exception {
        doNothing().when(destructionService).execute(anyLong(), isNull());

        mockMvc.perform(put("/destructions/1/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(destructionService).execute(1L, null);
    }

    @Test
    void testBatchExecute() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("ids", Arrays.asList(1L, 2L, 3L));
        params.put("remarks", "批量销毁完成");

        doNothing().when(destructionService).batchExecute(anyList(), anyString());

        mockMvc.perform(put("/destructions/batch-execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("批量销毁已执行"));

        verify(destructionService).batchExecute(anyList(), eq("批量销毁完成"));
    }
}
