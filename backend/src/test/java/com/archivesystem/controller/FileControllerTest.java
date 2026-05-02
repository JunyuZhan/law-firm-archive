package com.archivesystem.controller;

import com.archivesystem.dto.file.FilePreviewInfo;
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
/**
 * @author junyuzhan
 */

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

        when(fileStorageService.upload(any(), isNull(), eq("DOCUMENT"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(testFile);

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.originalName").value("test-document.pdf"))
                .andExpect(jsonPath("$.data.hashValue").doesNotExist())
                .andExpect(jsonPath("$.data.mimeType").doesNotExist())
                .andExpect(jsonPath("$.data.fileSize").doesNotExist());
    }

    @Test
    void testUpload_NoCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        when(fileStorageService.upload(any(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(testFile);

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

        when(fileStorageService.upload(any(), isNull(), anyString(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(file1Result)
                .thenReturn(file2Result);

        mockMvc.perform(multipart("/files/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("上传完成，成功 2 个"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].scanBatchNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].volumeNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].mimeType").doesNotExist())
                .andExpect(jsonPath("$.data[0].fileSize").doesNotExist());
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

        when(fileStorageService.upload(any(), isNull(), anyString(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(file1Result)
                .thenThrow(new RuntimeException("上传失败"));

        mockMvc.perform(multipart("/files/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("上传完成，成功 1 个，失败 1 个"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testUploadBatch_AllFailure() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test1.pdf", "application/pdf", "content1".getBytes());

        when(fileStorageService.upload(any(), isNull(), anyString(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("上传失败"));

        mockMvc.perform(multipart("/files/upload/batch")
                        .file(file1)
                        .param("fileCategory", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("批量上传失败，未成功上传任何文件，共失败 1 个"));
    }

    @Test
    void testGetDownloadUrl() throws Exception {
        String downloadUrl = "https://minio.example.com/bucket/file.pdf?token=abc";

        when(fileStorageService.getDownloadUrl(1L)).thenReturn(downloadUrl);

        mockMvc.perform(get("/files/1/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").value(downloadUrl));
    }

    @Test
    void testGetPreviewUrl() throws Exception {
        String previewUrl = "https://minio.example.com/bucket/file.pdf?preview=true";

        when(fileStorageService.getPreviewUrl(1L)).thenReturn(previewUrl);

        mockMvc.perform(get("/files/1/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").value(previewUrl));
    }

    @Test
    void testGetPreviewInfo() throws Exception {
        FilePreviewInfo previewInfo = FilePreviewInfo.builder()
                .url("https://minio.example.com/bucket/file.pdf?preview=true")
                .previewType("pdf")
                .isConverted(true)
                .build();

        when(fileStorageService.getPreviewInfo(1L)).thenReturn(previewInfo);

        mockMvc.perform(get("/files/1/preview-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/bucket/file.pdf?preview=true"))
                .andExpect(jsonPath("$.data.previewType").value("pdf"))
                .andExpect(jsonPath("$.data.isConverted").value(true))
                .andExpect(jsonPath("$.data.originalExtension").doesNotExist());
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
