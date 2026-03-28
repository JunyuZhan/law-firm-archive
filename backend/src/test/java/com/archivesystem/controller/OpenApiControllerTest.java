package com.archivesystem.controller;

import com.archivesystem.dto.archive.ArchiveReceiveRequest;
import com.archivesystem.dto.archive.ArchiveReceiveResponse;
import com.archivesystem.service.ArchiveService;
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
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OpenApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private OpenApiController openApiController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(openApiController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testReceive() throws Exception {
        ArchiveReceiveRequest.FileInfo fileInfo = new ArchiveReceiveRequest.FileInfo();
        fileInfo.setFileName("contract.pdf");
        fileInfo.setDownloadUrl("http://example.com/files/contract.pdf");
        fileInfo.setFileType("application/pdf");
        fileInfo.setFileCategory("CONTRACT");

        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("CASE-001");
        request.setArchiveType("CASE_FILE");
        request.setTitle("合同档案-2026");
        request.setRetentionPeriod("PERMANENT");
        request.setFiles(Arrays.asList(fileInfo));

        ArchiveReceiveResponse response = new ArchiveReceiveResponse();
        response.setArchiveId(1L);
        response.setArchiveNo("ARC-20260213-0001");
        response.setStatus("PROCESSING");
        response.setMessage("档案接收成功，正在处理中");
        response.setReceivedAt(LocalDateTime.now());

        when(archiveService.receive(any(ArchiveReceiveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/open/archive/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("档案接收成功"))
                .andExpect(jsonPath("$.data.archiveId").value(1))
                .andExpect(jsonPath("$.data.archiveNo").value("ARC-20260213-0001"));

        verify(archiveService).receive(any(ArchiveReceiveRequest.class));
    }

    @Test
    void testReceive_WithoutFiles() throws Exception {
        ArchiveReceiveRequest request = new ArchiveReceiveRequest();
        request.setSourceType("LAW_FIRM");
        request.setSourceId("CASE-002");
        request.setArchiveType("CASE_FILE");
        request.setTitle("无附件档案");
        request.setRetentionPeriod("30_YEARS");

        ArchiveReceiveResponse response = new ArchiveReceiveResponse();
        response.setArchiveId(2L);
        response.setArchiveNo("ARC-20260213-0002");
        response.setStatus("RECEIVED");
        response.setReceivedAt(LocalDateTime.now());

        when(archiveService.receive(any(ArchiveReceiveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/open/archive/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveId").value(2));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/open/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value("ok"));
    }
}
