package com.archivesystem.controller;

import com.archivesystem.entity.Archive;
import com.archivesystem.service.RetentionService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class RetentionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RetentionService retentionService;

    @InjectMocks
    private RetentionController retentionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(retentionController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetExpiringArchives() throws Exception {
        Archive archive = new Archive();
        archive.setId(1L);
        archive.setArchiveNo("ARC-001");
        archive.setTitle("测试档案");
        archive.setCallbackUrl("https://internal.example/callback");
        when(retentionService.findExpiringArchives(90)).thenReturn(List.of(archive));

        mockMvc.perform(get("/retention/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].archiveNo").value("ARC-001"))
                .andExpect(jsonPath("$.data[0].title").value("测试档案"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].callbackUrl").doesNotExist());
    }

    @Test
    void testGetExpiredArchives() throws Exception {
        Archive archive = new Archive();
        archive.setId(2L);
        archive.setArchiveNo("ARC-002");
        archive.setTitle("已到期档案");
        archive.setStatus(Archive.STATUS_APPRAISAL);
        archive.setRetentionPeriod("Y10");
        when(retentionService.findExpiredArchives()).thenReturn(List.of(archive));

        mockMvc.perform(get("/retention/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].archiveNo").value("ARC-002"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].callbackUrl").doesNotExist());
    }

    @Test
    void testExtendRetention_MissingParams() throws Exception {
        mockMvc.perform(put("/retention/1/extend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("请求参数不能为空"));
    }

    @Test
    void testExtendRetention_MissingRetentionPeriod() throws Exception {
        mockMvc.perform(put("/retention/1/extend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("新保管期限不能为空"));
    }

    @Test
    void testApplyForDestruction_MissingReason() throws Exception {
        mockMvc.perform(post("/retention/1/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("销毁原因不能为空"));
    }

    @Test
    void testExecuteDestruction_Success() throws Exception {
        doNothing().when(retentionService).executeDestruction(1L, "done");

        mockMvc.perform(put("/retention/1/destruction/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("remarks", "done"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("档案已销毁"));

        verify(retentionService).executeDestruction(1L, "done");
    }
}
