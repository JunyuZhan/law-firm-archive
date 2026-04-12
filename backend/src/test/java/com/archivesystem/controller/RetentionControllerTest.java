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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
        when(retentionService.findExpiringArchives(90)).thenReturn(List.of(archive));

        mockMvc.perform(get("/retention/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(1));
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
    void testExecuteDestruction_InvalidApproverId() throws Exception {
        mockMvc.perform(put("/retention/1/destruction/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approverId", "abc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("approverId 格式错误"));
    }

    @Test
    void testExecuteDestruction_Success() throws Exception {
        doNothing().when(retentionService).executeDestruction(1L, 2L, "done");

        mockMvc.perform(put("/retention/1/destruction/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approverId", 2, "remarks", "done"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("档案已销毁"));

        verify(retentionService).executeDestruction(1L, 2L, "done");
    }
}
