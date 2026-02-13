package com.archivesystem.controller;

import com.archivesystem.entity.DigitalFile;
import com.archivesystem.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileController fileController;

    private DigitalFile testFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();

        testFile = new DigitalFile();
        testFile.setId(1L);
        testFile.setFileName("abc123.pdf");
        testFile.setOriginalName("test-document.pdf");
        testFile.setFileExtension("pdf");
        testFile.setMimeType("application/pdf");
        testFile.setFileSize(1024L);
        testFile.setHashValue("sha256hash");
        testFile.setUploadAt(LocalDateTime.now());
    }

    @Test
    void testUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        when(fileStorageService.upload(any(), isNull(), eq("DOCUMENT"))).thenReturn(testFile);

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.originalName").value("test-document.pdf"));
    }

    @Test
    void testUpload_NoCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        when(fileStorageService.upload(any(), isNull(), isNull())).thenReturn(testFile);

        mockMvc.perform(multipart("/files/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testUploadBatch() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "test2.pdf", "application/pdf", "content2".getBytes());

        DigitalFile file1Result = new DigitalFile();
        file1Result.setId(1L);
        file1Result.setFileName("file1.pdf");
        file1Result.setOriginalName("test1.pdf");
        file1Result.setFileSize(8L);
        file1Result.setMimeType("application/pdf");

        DigitalFile file2Result = new DigitalFile();
        file2Result.setId(2L);
        file2Result.setFileName("file2.pdf");
        file2Result.setOriginalName("test2.pdf");
        file2Result.setFileSize(8L);
        file2Result.setMimeType("application/pdf");

        when(fileStorageService.upload(any(), isNull(), anyString()))
                .thenReturn(file1Result)
                .thenReturn(file2Result);

        mockMvc.perform(multipart("/files/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("上传完成"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testUploadBatch_PartialFailure() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "test2.pdf", "application/pdf", "content2".getBytes());

        DigitalFile file1Result = new DigitalFile();
        file1Result.setId(1L);
        file1Result.setFileName("file1.pdf");
        file1Result.setOriginalName("test1.pdf");
        file1Result.setFileSize(8L);
        file1Result.setMimeType("application/pdf");

        when(fileStorageService.upload(any(), isNull(), anyString()))
                .thenReturn(file1Result)
                .thenThrow(new RuntimeException("上传失败"));

        mockMvc.perform(multipart("/files/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testGetDownloadUrl() throws Exception {
        String downloadUrl = "https://minio.example.com/bucket/file.pdf?token=abc";

        when(fileStorageService.getDownloadUrl(1L)).thenReturn(downloadUrl);

        mockMvc.perform(get("/files/1/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(downloadUrl));
    }

    @Test
    void testGetPreviewUrl() throws Exception {
        String previewUrl = "https://minio.example.com/bucket/file.pdf?preview=true";

        when(fileStorageService.getPreviewUrl(1L)).thenReturn(previewUrl);

        mockMvc.perform(get("/files/1/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(previewUrl));
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(fileStorageService).delete(1L);

        mockMvc.perform(delete("/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(fileStorageService).delete(1L);
    }
}
