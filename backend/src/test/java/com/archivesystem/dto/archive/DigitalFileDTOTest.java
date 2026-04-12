package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class DigitalFileDTOTest {

    @Test
    void testBuilder() {
        LocalDateTime uploadAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 0);

        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(1L)
                .archiveId(100L)
                .fileNo("FILE-001")
                .fileName("document.pdf")
                .originalName("原始文件名.pdf")
                .fileExtension("pdf")
                .mimeType("application/pdf")
                .fileSize(1024000L)
                .fileSizeFormatted("1 MB")
                .formatName("PDF")
                .isLongTermFormat(true)
                .hashAlgorithm("SHA-256")
                .hashValue("abc123def456")
                .hasPreview(true)
                .previewUrl("https://example.com/preview/1")
                .thumbnailUrl("https://example.com/thumb/1")
                .downloadUrl("https://example.com/download/1")
                .fileCategory("EVIDENCE")
                .fileCategoryName("证据材料")
                .sortOrder(1)
                .description("文件描述")
                .volumeNo(1)
                .sectionType("EVIDENCE")
                .documentNo("E-001")
                .pageStart(3)
                .pageEnd(15)
                .versionLabel("原件")
                .ocrStatus("COMPLETED")
                .uploadAt(uploadAt)
                .uploadByName("张三")
                .createdAt(createdAt)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getArchiveId());
        assertEquals("FILE-001", dto.getFileNo());
        assertEquals("document.pdf", dto.getFileName());
        assertEquals("原始文件名.pdf", dto.getOriginalName());
        assertEquals("pdf", dto.getFileExtension());
        assertEquals("application/pdf", dto.getMimeType());
        assertEquals(1024000L, dto.getFileSize());
        assertEquals("1 MB", dto.getFileSizeFormatted());
        assertEquals("PDF", dto.getFormatName());
        assertTrue(dto.getIsLongTermFormat());
        assertEquals("SHA-256", dto.getHashAlgorithm());
        assertEquals("abc123def456", dto.getHashValue());
        assertTrue(dto.getHasPreview());
        assertEquals("https://example.com/preview/1", dto.getPreviewUrl());
        assertEquals("https://example.com/thumb/1", dto.getThumbnailUrl());
        assertEquals("https://example.com/download/1", dto.getDownloadUrl());
        assertEquals("EVIDENCE", dto.getFileCategory());
        assertEquals("证据材料", dto.getFileCategoryName());
        assertEquals(1, dto.getSortOrder());
        assertEquals("文件描述", dto.getDescription());
        assertEquals(1, dto.getVolumeNo());
        assertEquals("EVIDENCE", dto.getSectionType());
        assertEquals("E-001", dto.getDocumentNo());
        assertEquals(3, dto.getPageStart());
        assertEquals(15, dto.getPageEnd());
        assertEquals("原件", dto.getVersionLabel());
        assertEquals("COMPLETED", dto.getOcrStatus());
        assertEquals(uploadAt, dto.getUploadAt());
        assertEquals("张三", dto.getUploadByName());
        assertEquals(createdAt, dto.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        DigitalFileDTO dto = new DigitalFileDTO();

        assertNull(dto.getId());
        assertNull(dto.getFileName());
        assertNull(dto.getFileSize());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        DigitalFileDTO dto = new DigitalFileDTO();
        dto.setId(1L);
        dto.setArchiveId(100L);
        dto.setFileNo("FILE-001");
        dto.setFileName("test.pdf");
        dto.setOriginalName("原始.pdf");
        dto.setFileExtension("pdf");
        dto.setMimeType("application/pdf");
        dto.setFileSize(1000L);
        dto.setFileSizeFormatted("1 KB");
        dto.setFormatName("PDF");
        dto.setIsLongTermFormat(true);
        dto.setHashAlgorithm("MD5");
        dto.setHashValue("hash123");
        dto.setHasPreview(true);
        dto.setPreviewUrl("preview");
        dto.setThumbnailUrl("thumb");
        dto.setDownloadUrl("download");
        dto.setFileCategory("CAT");
        dto.setFileCategoryName("分类");
        dto.setSortOrder(1);
        dto.setDescription("描述");
        dto.setVolumeNo(2);
        dto.setSectionType("MAIN");
        dto.setDocumentNo("M-002");
        dto.setPageStart(16);
        dto.setPageEnd(24);
        dto.setVersionLabel("扫描件");
        dto.setOcrStatus("DONE");
        dto.setUploadAt(now);
        dto.setUploadByName("用户");
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getArchiveId());
        assertEquals("FILE-001", dto.getFileNo());
        assertEquals("test.pdf", dto.getFileName());
        assertEquals(2, dto.getVolumeNo());
        assertEquals("MAIN", dto.getSectionType());
        assertEquals("M-002", dto.getDocumentNo());
        assertEquals(16, dto.getPageStart());
        assertEquals(24, dto.getPageEnd());
        assertEquals("扫描件", dto.getVersionLabel());
    }

    @Test
    void testSettersAndGetters() {
        DigitalFileDTO dto = new DigitalFileDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(2L);
        dto.setArchiveId(200L);
        dto.setFileNo("FILE-002");
        dto.setFileName("image.jpg");
        dto.setOriginalName("图片.jpg");
        dto.setFileExtension("jpg");
        dto.setMimeType("image/jpeg");
        dto.setFileSize(500000L);
        dto.setFileSizeFormatted("500 KB");
        dto.setFormatName("JPEG");
        dto.setIsLongTermFormat(false);
        dto.setHashAlgorithm("MD5");
        dto.setHashValue("md5hash");
        dto.setHasPreview(false);
        dto.setPreviewUrl(null);
        dto.setThumbnailUrl("thumb-url");
        dto.setDownloadUrl("download-url");
        dto.setFileCategory("IMAGE");
        dto.setFileCategoryName("图片");
        dto.setSortOrder(2);
        dto.setDescription("图片描述");
        dto.setVolumeNo(3);
        dto.setSectionType("ATTACHMENT");
        dto.setDocumentNo("A-009");
        dto.setPageStart(1);
        dto.setPageEnd(2);
        dto.setVersionLabel("副本");
        dto.setOcrStatus("PENDING");
        dto.setUploadAt(now);
        dto.setUploadByName("李四");
        dto.setCreatedAt(now);

        assertEquals(2L, dto.getId());
        assertEquals(200L, dto.getArchiveId());
        assertEquals("FILE-002", dto.getFileNo());
        assertEquals("image.jpg", dto.getFileName());
        assertEquals("图片.jpg", dto.getOriginalName());
        assertEquals("jpg", dto.getFileExtension());
        assertEquals("image/jpeg", dto.getMimeType());
        assertEquals(500000L, dto.getFileSize());
        assertEquals("500 KB", dto.getFileSizeFormatted());
        assertEquals("JPEG", dto.getFormatName());
        assertFalse(dto.getIsLongTermFormat());
        assertEquals("MD5", dto.getHashAlgorithm());
        assertEquals("md5hash", dto.getHashValue());
        assertFalse(dto.getHasPreview());
        assertNull(dto.getPreviewUrl());
        assertEquals("thumb-url", dto.getThumbnailUrl());
        assertEquals("download-url", dto.getDownloadUrl());
        assertEquals("IMAGE", dto.getFileCategory());
        assertEquals("图片", dto.getFileCategoryName());
        assertEquals(2, dto.getSortOrder());
        assertEquals("图片描述", dto.getDescription());
        assertEquals(3, dto.getVolumeNo());
        assertEquals("ATTACHMENT", dto.getSectionType());
        assertEquals("A-009", dto.getDocumentNo());
        assertEquals(1, dto.getPageStart());
        assertEquals(2, dto.getPageEnd());
        assertEquals("副本", dto.getVersionLabel());
        assertEquals("PENDING", dto.getOcrStatus());
        assertEquals(now, dto.getUploadAt());
        assertEquals("李四", dto.getUploadByName());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        DigitalFileDTO dto1 = DigitalFileDTO.builder()
                .id(1L)
                .fileName("test.pdf")
                .build();

        DigitalFileDTO dto2 = DigitalFileDTO.builder()
                .id(1L)
                .fileName("test.pdf")
                .build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToString() {
        DigitalFileDTO dto = DigitalFileDTO.builder()
                .id(1L)
                .fileName("test.pdf")
                .build();

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("DigitalFileDTO"));
    }
}
