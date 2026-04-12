package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.ForbiddenException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.security.OutboundUrlValidator;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private BorrowApplicationMapper borrowApplicationMapper;

    @Mock
    private ConfigService configService;

    @Mock
    private DocumentConversionService documentConversionService;

    @Mock
    private OutboundUrlValidator outboundUrlValidator;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private DigitalFile testFile;

    @BeforeEach
    void setUp() {
        // 通过 ConfigService mock 配置值
        lenient().when(configService.getIntValue(eq("system.upload.max.size"), any()))
            .thenReturn(104857600);
        lenient().when(configService.getValue(eq("system.upload.allowed.types"), anyString()))
            .thenReturn("pdf,doc,docx,jpg,jpeg,png");

        testFile = new DigitalFile();
        testFile.setId(1L);
        testFile.setArchiveId(100L);
        testFile.setFileName("test.pdf");
        testFile.setOriginalName("test.pdf");
        testFile.setFileExtension("pdf");
        testFile.setStoragePath("archives/2026/02/13/test.pdf");
        testFile.setMimeType("application/pdf");
    }

    @Test
    void testGetDownloadUrl_Success() {
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/download/test.pdf");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            String result = fileStorageService.getDownloadUrl(1L);

            assertNotNull(result);
            assertTrue(result.contains("test.pdf"));
        }
    }

    @Test
    void testGetDownloadUrl_FileNotFound() {
        when(digitalFileMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> fileStorageService.getDownloadUrl(999L));
    }

    @Test
    void testGetPreviewUrl_Pdf() {
        testFile.setFileExtension("pdf");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/preview/test.pdf");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            String result = fileStorageService.getPreviewUrl(1L);

            assertNotNull(result);
        }
    }

    @Test
    void testGetPreviewUrl_Image() {
        testFile.setFileExtension("jpg");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/preview/test.jpg");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            String result = fileStorageService.getPreviewUrl(1L);

            assertNotNull(result);
        }
    }

    @Test
    void testGetPreviewUrl_WithPreviewPath() {
        testFile.setPreviewPath("previews/test_preview.pdf");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(eq("previews/test_preview.pdf"), anyInt()))
            .thenReturn("http://minio/preview/test_preview.pdf");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            String result = fileStorageService.getPreviewUrl(1L);

            assertNotNull(result);
            assertTrue(result.contains("preview"));
        }
    }

    @Test
    void testGetPreviewUrl_UnsupportedType() {
        testFile.setFileExtension("doc");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            String result = fileStorageService.getPreviewUrl(1L);

            assertNull(result);
        }
    }

    @Test
    void testGetPreviewUrl_FileNotFound() {
        when(digitalFileMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> fileStorageService.getPreviewUrl(999L));
    }

    @Test
    void testDelete_Success() {
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        doNothing().when(minioService).delete(anyString());
        when(digitalFileMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> fileStorageService.delete(1L));

        verify(minioService).delete(testFile.getStoragePath());
        verify(digitalFileMapper).deleteById(1L);
    }

    @Test
    void testDelete_WithPreviewAndThumbnail() {
        testFile.setPreviewPath("previews/preview.pdf");
        testFile.setThumbnailPath("thumbnails/thumb.jpg");
        
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        doNothing().when(minioService).delete(anyString());
        when(digitalFileMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> fileStorageService.delete(1L));

        verify(minioService).delete(testFile.getStoragePath());
        verify(minioService).delete(testFile.getPreviewPath());
        verify(minioService).delete(testFile.getThumbnailPath());
    }

    @Test
    void testDelete_FileNotFound() {
        when(digitalFileMapper.selectById(999L)).thenReturn(null);

        // 不应该抛出异常，直接返回
        assertDoesNotThrow(() -> fileStorageService.delete(999L));
        
        verify(minioService, never()).delete(anyString());
    }

    @Test
    void testDelete_MinioException() {
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        doThrow(new RuntimeException("MinIO错误")).when(minioService).delete(anyString());

        assertThrows(BusinessException.class, () -> fileStorageService.delete(1L));
    }

    @Test
    void testDelete_StoredArchiveForbidden() {
        Archive archive = Archive.builder().status(Archive.STATUS_STORED).build();
        archive.setId(100L);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(archiveMapper.selectById(100L)).thenReturn(archive);

        assertThrows(BusinessException.class, () -> fileStorageService.delete(1L));

        verify(minioService, never()).delete(anyString());
        verify(digitalFileMapper, never()).deleteById(anyLong());
    }

    @Test
    void testUpload_EmptyFile() throws IOException {
        when(mockFile.isEmpty()).thenReturn(true);

        assertThrows(BusinessException.class, () -> 
            fileStorageService.upload(mockFile, 100L, "MAIN"));
    }

    @Test
    void testUpload_NullFile() {
        assertThrows(BusinessException.class, () -> 
            fileStorageService.upload(null, 100L, "MAIN"));
    }

    @Test
    void testUpload_FileTooLarge() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(200000000L); // 超过100MB限制

        assertThrows(BusinessException.class, () -> 
            fileStorageService.upload(mockFile, 100L, "MAIN"));
    }

    @Test
    void testUpload_UnsupportedType() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("test.exe");

        assertThrows(BusinessException.class, () -> 
            fileStorageService.upload(mockFile, 100L, "MAIN"));
    }

    @Test
    void testUpload_SyncsArchiveFileStats() throws Exception {
        Archive archive = Archive.builder().build();
        archive.setId(100L);

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getBytes()).thenReturn("%PDF-1.4 test".getBytes());
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("%PDF-1.4 test".getBytes()));
        when(minioService.getBucketName()).thenReturn("archive");
        doNothing().when(minioService).upload(anyString(), any(), anyLong(), anyString());
        when(documentConversionService.needsConversion("pdf")).thenReturn(false);
        when(documentConversionService.isLongTermFormat("pdf")).thenReturn(true);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(digitalFileMapper.countByArchiveId(100L)).thenReturn(Map.of("count", 1, "total_size", 1024L));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            DigitalFile result = fileStorageService.upload(mockFile, 100L, "MAIN");

            assertNotNull(result);
        }

        verify(digitalFileMapper).insert(any(DigitalFile.class));
        verify(archiveMapper).updateById(argThat(updated ->
                updated.getId().equals(100L)
                        && Integer.valueOf(1).equals(updated.getFileCount())
                        && Long.valueOf(1024L).equals(updated.getTotalFileSize())
                        && Boolean.TRUE.equals(updated.getHasElectronic())));
    }

    @Test
    void testGetPreviewUrl_AllImageTypes() {
        String[] imageTypes = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
        
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(true);

            for (String type : imageTypes) {
                testFile.setFileExtension(type);
                testFile.setPreviewPath(null);

                lenient().when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
                lenient().when(minioService.getPresignedUrl(anyString(), anyInt()))
                    .thenReturn("http://minio/file." + type);

                String result = fileStorageService.getPreviewUrl(1L);

                assertNotNull(result, "应该支持预览类型: " + type);
            }
        }
    }

    @Test
    void testGetDownloadUrl_ForbiddenForUnrelatedUser() {
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(9L);

            assertThrows(ForbiddenException.class, () -> fileStorageService.getDownloadUrl(1L));
        }
    }

    @Test
    void testGetDownloadUrl_AllowedForBorrowApplicant() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .applicantId(9L)
                .applicantDept("诉讼部")
                .status(BorrowApplication.STATUS_APPROVED)
                .borrowType(BorrowApplication.TYPE_DOWNLOAD)
                .build();
        Archive archive = Archive.builder()
                .securityLevel(Archive.SECURITY_INTERNAL)
                .build();
        archive.setId(100L);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of(application));
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/download/test.pdf");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(9L);
            securityUtils.when(SecurityUtils::getCurrentDepartment).thenReturn("诉讼部");

            assertNotNull(fileStorageService.getDownloadUrl(1L));
        }
    }

    @Test
    void testGetPreviewUrl_AllowedForOnlineBorrowApplicant() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .applicantId(9L)
                .applicantDept("诉讼部")
                .status(BorrowApplication.STATUS_APPROVED)
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .build();
        Archive archive = Archive.builder()
                .securityLevel(Archive.SECURITY_CONFIDENTIAL)
                .build();
        archive.setId(100L);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of(application));
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/preview/test.pdf");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(9L);
            securityUtils.when(SecurityUtils::getCurrentDepartment).thenReturn("诉讼部");

            assertNotNull(fileStorageService.getPreviewUrl(1L));
        }
    }

    @Test
    void testGetDownloadUrl_RejectsOnlineBorrowApplicant() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .applicantId(9L)
                .applicantDept("诉讼部")
                .status(BorrowApplication.STATUS_APPROVED)
                .borrowType(BorrowApplication.TYPE_ONLINE)
                .build();
        Archive archive = Archive.builder()
                .securityLevel(Archive.SECURITY_INTERNAL)
                .build();
        archive.setId(100L);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of(application));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(9L);
            securityUtils.when(SecurityUtils::getCurrentDepartment).thenReturn("诉讼部");

            assertThrows(ForbiddenException.class, () -> fileStorageService.getDownloadUrl(1L));
        }
    }

    @Test
    void testGetPreviewUrl_RejectsReturnedBorrowApplicant() {
        BorrowApplication application = BorrowApplication.builder()
                .archiveId(100L)
                .applicantId(9L)
                .applicantDept("诉讼部")
                .status(BorrowApplication.STATUS_RETURNED)
                .borrowType(BorrowApplication.TYPE_DOWNLOAD)
                .build();
        Archive archive = Archive.builder()
                .securityLevel(Archive.SECURITY_INTERNAL)
                .build();
        archive.setId(100L);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(archiveMapper.selectById(100L)).thenReturn(archive);
        when(borrowApplicationMapper.selectByArchiveId(100L)).thenReturn(java.util.List.of(application));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER"))
                    .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(9L);
            securityUtils.when(SecurityUtils::getCurrentDepartment).thenReturn("诉讼部");

            assertThrows(ForbiddenException.class, () -> fileStorageService.getPreviewUrl(1L));
        }
    }
}
