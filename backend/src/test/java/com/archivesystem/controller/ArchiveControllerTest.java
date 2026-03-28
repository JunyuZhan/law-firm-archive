package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.config.MetricsConfig;
import com.archivesystem.dto.archive.*;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.service.AccessLogService;
import com.archivesystem.service.ArchiveIndexService;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArchiveControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ArchiveIndexService archiveIndexService;

    @Mock
    private MetricsConfig metricsConfig;

    @Mock
    private AccessLogService accessLogService;

    @InjectMocks
    private ArchiveController archiveController;

    private ObjectMapper objectMapper;
    private ArchiveDTO testArchiveDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(archiveController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testArchiveDTO = ArchiveDTO.builder()
                .id(1L)
                .archiveNo("ARC-20260213-0001")
                .title("测试档案")
                .archiveType("DOCUMENT")
                .status("RECEIVED")
                .fondsId(1L)
                .fondsNo("QZ001")
                .build();
    }

    @Test
    void testCreate_Success() throws Exception {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("新档案");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);
        request.setRetentionPeriod("PERMANENT"); // 必填字段

        when(archiveService.create(any(ArchiveCreateRequest.class))).thenReturn(testArchiveDTO);

        mockMvc.perform(post("/archives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveNo").value("ARC-20260213-0001"));
    }

    @Test
    void testUpdate_Success() throws Exception {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("更新后的档案");
        request.setArchiveType("DOCUMENT");
        request.setRetentionPeriod("PERMANENT"); // 必填字段

        when(archiveService.update(eq(1L), any(ArchiveCreateRequest.class))).thenReturn(testArchiveDTO);

        mockMvc.perform(put("/archives/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(archiveService.getById(1L)).thenReturn(testArchiveDTO);

        mockMvc.perform(get("/archives/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testGetByArchiveNo_Success() throws Exception {
        when(archiveService.getByArchiveNo("ARC-20260213-0001")).thenReturn(testArchiveDTO);

        mockMvc.perform(get("/archives/no/ARC-20260213-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.archiveNo").value("ARC-20260213-0001"));
    }

    @Test
    void testQuery_Success() throws Exception {
        PageResult<ArchiveDTO> pageResult = PageResult.of(1L, 10L, 1L, Arrays.asList(testArchiveDTO));

        when(archiveService.query(any(ArchiveQueryRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/archives")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testQuery_WithKeyword() throws Exception {
        PageResult<ArchiveDTO> pageResult = PageResult.of(1L, 10L, 1L, Arrays.asList(testArchiveDTO));

        when(archiveService.query(any(ArchiveQueryRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/archives")
                        .param("keyword", "测试")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(archiveService).delete(1L);

        mockMvc.perform(delete("/archives/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(archiveService).delete(1L);
    }

    @Test
    void testUpdateStatus_Success() throws Exception {
        doNothing().when(archiveService).updateStatus(1L, "STORED");

        mockMvc.perform(put("/archives/1/status")
                        .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testUploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setArchiveId(1L);
        digitalFile.setFileName("test.pdf");
        digitalFile.setOriginalName("test.pdf");
        digitalFile.setFileSize(12L);
        digitalFile.setMimeType("application/pdf");

        when(fileStorageService.upload(any(), eq(1L), isNull())).thenReturn(digitalFile);

        mockMvc.perform(multipart("/archives/1/files")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"));
    }

    @Test
    void testUploadFile_WithCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setArchiveId(1L);
        digitalFile.setFileName("test.pdf");

        when(fileStorageService.upload(any(), eq(1L), eq("CONTRACT"))).thenReturn(digitalFile);

        mockMvc.perform(multipart("/archives/1/files")
                        .file(file)
                        .param("fileCategory", "CONTRACT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetDownloadUrl_Success() throws Exception {
        when(fileStorageService.getDownloadUrl(1L))
                .thenReturn("http://localhost:9000/archives/file.pdf?token=xxx");

        mockMvc.perform(get("/archives/files/1/download-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    void testGetPreviewUrl_Success() throws Exception {
        when(fileStorageService.getPreviewUrl(1L))
                .thenReturn("http://localhost:9000/archives/preview.pdf?token=xxx");

        mockMvc.perform(get("/archives/files/1/preview-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        doNothing().when(fileStorageService).delete(1L);

        mockMvc.perform(delete("/archives/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(fileStorageService).delete(1L);
    }

    @Test
    void testSearch_Success() throws Exception {
        ArchiveSearchResult searchResult = ArchiveSearchResult.builder()
                .hits(Collections.emptyList())
                .total(0)
                .pageNum(1)
                .pageSize(20)
                .totalPages(0)
                .took(10L)
                .build();

        when(archiveIndexService.search(any(ArchiveSearchRequest.class))).thenReturn(searchResult);
        doNothing().when(metricsConfig).recordSearchRequest();
        doNothing().when(accessLogService).logSearch(anyString(), anyInt(), anyLong(), anyString());

        mockMvc.perform(get("/archives/search")
                        .param("keyword", "测试")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testSearch_WithFilters() throws Exception {
        ArchiveSearchResult searchResult = ArchiveSearchResult.builder()
                .hits(Collections.emptyList())
                .total(0)
                .pageNum(1)
                .pageSize(20)
                .totalPages(0)
                .build();

        when(archiveIndexService.search(any(ArchiveSearchRequest.class))).thenReturn(searchResult);
        doNothing().when(metricsConfig).recordSearchRequest();
        doNothing().when(accessLogService).logSearch(anyString(), anyInt(), anyLong(), anyString());

        mockMvc.perform(get("/archives/search")
                        .param("keyword", "测试")
                        .param("archiveType", "DOCUMENT")
                        .param("fondsId", "1")
                        .param("status", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetSearchAggregations_Success() throws Exception {
        Map<String, Object> aggregations = new HashMap<>();
        aggregations.put("byType", Arrays.asList(Map.of("key", "DOCUMENT", "count", 10)));

        when(archiveIndexService.getAggregations()).thenReturn(aggregations);

        mockMvc.perform(get("/archives/search/aggregations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testRebuildIndex_Success() throws Exception {
        doNothing().when(archiveIndexService).rebuildAllIndexes();

        mockMvc.perform(post("/archives/search/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(archiveIndexService).rebuildAllIndexes();
    }

    @Test
    void testIndexArchive_Success() throws Exception {
        doNothing().when(archiveIndexService).indexArchive(1L);

        mockMvc.perform(post("/archives/1/index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(archiveIndexService).indexArchive(1L);
    }
}
