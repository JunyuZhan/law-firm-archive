package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.AppraisalRecord;
import com.archivesystem.service.AppraisalService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppraisalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppraisalService appraisalService;

    @InjectMocks
    private AppraisalController appraisalController;

    private ObjectMapper objectMapper;
    private AppraisalRecord testRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appraisalController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testRecord = AppraisalRecord.builder()
                .archiveId(100L)
                .appraisalType(AppraisalRecord.TYPE_VALUE)
                .originalValue("10年")
                .newValue("30年")
                .appraisalReason("重新鉴定")
                .appraisalOpinion("建议延长")
                .status(AppraisalRecord.STATUS_PENDING)
                .appraiserName("张三")
                .appraisedAt(LocalDateTime.now())
                .approvalComment("内部审批意见")
                .build();
        testRecord.setId(1L);
        testRecord.setAppraiserId(11L);
        testRecord.setApproverId(22L);
        testRecord.setCreatedBy(33L);
        testRecord.setDeleted(false);
    }

    @Test
    void testCreate() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("archiveId", 100L);
        payload.put("appraisalType", AppraisalRecord.TYPE_VALUE);
        payload.put("originalValue", "10年");
        payload.put("newValue", "30年");
        payload.put("appraisalReason", "重新鉴定");

        when(appraisalService.create(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testRecord);

        mockMvc.perform(post("/appraisals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("鉴定申请提交成功"))
                .andExpect(jsonPath("$.data.archiveId").value(100))
                .andExpect(jsonPath("$.data.appraisalType").value(AppraisalRecord.TYPE_VALUE))
                .andExpect(jsonPath("$.data.status").value(AppraisalRecord.STATUS_PENDING))
                .andExpect(jsonPath("$.data.appraisalOpinion").doesNotExist())
                .andExpect(jsonPath("$.data.approvalComment").doesNotExist())
                .andExpect(jsonPath("$.data.appraiserId").doesNotExist())
                .andExpect(jsonPath("$.data.approverId").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist());
    }

    @Test
    void testGetById() throws Exception {
        when(appraisalService.getById(1L)).thenReturn(testRecord);

        mockMvc.perform(get("/appraisals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.appraisalOpinion").doesNotExist())
                .andExpect(jsonPath("$.data.approvalComment").value("内部审批意见"))
                .andExpect(jsonPath("$.data.appraiserId").doesNotExist())
                .andExpect(jsonPath("$.data.approverId").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    @Test
    void testGetList() throws Exception {
        when(appraisalService.getList(eq("VALUE"), eq("PENDING"), eq(1), eq(20)))
                .thenReturn(PageResult.of(1, 20, 1, Collections.singletonList(testRecord)));

        mockMvc.perform(get("/appraisals")
                        .param("type", "VALUE")
                        .param("status", "PENDING")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].approvalComment").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].appraisalOpinion").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].appraiserId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].createdBy").doesNotExist());
    }

    @Test
    void testGetPendingList() throws Exception {
        when(appraisalService.getPendingList(anyInt(), anyInt()))
                .thenReturn(PageResult.of(1, 20, 1, Collections.singletonList(testRecord)));

        mockMvc.perform(get("/appraisals/pending")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].approvalComment").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].appraisalOpinion").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].approverId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].deleted").doesNotExist());
    }

    @Test
    void testGetByArchiveId_ShouldHideApprovalComment() throws Exception {
        when(appraisalService.getByArchiveId(100L)).thenReturn(List.of(testRecord));

        mockMvc.perform(get("/appraisals/archive/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].archiveId").value(100))
                .andExpect(jsonPath("$.data[0].appraisalOpinion").doesNotExist())
                .andExpect(jsonPath("$.data[0].approvalComment").doesNotExist());
    }

    @Test
    void testApprove() throws Exception {
        doNothing().when(appraisalService).approve(1L, "同意");

        mockMvc.perform(put("/appraisals/1/approve").param("comment", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("审批通过"));

        verify(appraisalService).approve(1L, "同意");
    }

    @Test
    void testReject() throws Exception {
        doNothing().when(appraisalService).reject(1L, "不同意");

        mockMvc.perform(put("/appraisals/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"不同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已拒绝"));

        verify(appraisalService).reject(1L, "不同意");
    }
}
