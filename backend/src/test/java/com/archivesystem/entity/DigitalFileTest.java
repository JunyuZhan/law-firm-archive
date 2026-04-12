package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class DigitalFileTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        DigitalFile file = DigitalFile.builder()
                .id(1L)
                .archiveId(100L)
                .fileNo("FILE-001")
                .fileName("document.pdf")
                .originalName("原始文件名.pdf")
                .fileExtension("pdf")
                .mimeType("application/pdf")
                .fileSize(1024000L)
                .storagePath("/archives/2026/01/file.pdf")
                .storageBucket("archive-files")
                .formatName("PDF")
                .formatVersion("1.7")
                .isLongTermFormat(true)
                .convertedPath("/converted/file.pdf")
                .hashAlgorithm("SHA256")
                .hashValue("abc123def456")
                .ocrStatus(DigitalFile.OCR_COMPLETED)
                .ocrContent("OCR识别内容")
                .hasPreview(true)
                .previewPath("/preview/file.pdf")
                .thumbnailPath("/thumb/file.jpg")
                .fileCategory(DigitalFile.CATEGORY_MAIN)
                .sortOrder(1)
                .description("文件描述")
                .volumeNo(2)
                .sectionType(DigitalFile.SECTION_EVIDENCE)
                .documentNo("DOC-08")
                .pageStart(15)
                .pageEnd(28)
                .versionLabel("v2")
                .sourceUrl("https://source.example.com/file.pdf")
                .uploadAt(now)
                .uploadBy(1L)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        assertEquals(1L, file.getId());
        assertEquals(100L, file.getArchiveId());
        assertEquals("FILE-001", file.getFileNo());
        assertEquals("document.pdf", file.getFileName());
        assertEquals("原始文件名.pdf", file.getOriginalName());
        assertEquals("pdf", file.getFileExtension());
        assertEquals("application/pdf", file.getMimeType());
        assertEquals(1024000L, file.getFileSize());
        assertEquals("/archives/2026/01/file.pdf", file.getStoragePath());
        assertEquals("archive-files", file.getStorageBucket());
        assertEquals("PDF", file.getFormatName());
        assertEquals("1.7", file.getFormatVersion());
        assertTrue(file.getIsLongTermFormat());
        assertEquals("/converted/file.pdf", file.getConvertedPath());
        assertEquals("SHA256", file.getHashAlgorithm());
        assertEquals("abc123def456", file.getHashValue());
        assertEquals(DigitalFile.OCR_COMPLETED, file.getOcrStatus());
        assertEquals("OCR识别内容", file.getOcrContent());
        assertTrue(file.getHasPreview());
        assertEquals("/preview/file.pdf", file.getPreviewPath());
        assertEquals("/thumb/file.jpg", file.getThumbnailPath());
        assertEquals(DigitalFile.CATEGORY_MAIN, file.getFileCategory());
        assertEquals(1, file.getSortOrder());
        assertEquals("文件描述", file.getDescription());
        assertEquals(2, file.getVolumeNo());
        assertEquals(DigitalFile.SECTION_EVIDENCE, file.getSectionType());
        assertEquals("DOC-08", file.getDocumentNo());
        assertEquals(15, file.getPageStart());
        assertEquals(28, file.getPageEnd());
        assertEquals("v2", file.getVersionLabel());
        assertEquals("https://source.example.com/file.pdf", file.getSourceUrl());
        assertFalse(file.getDeleted());
    }

    @Test
    void testDefaultValues() {
        DigitalFile file = DigitalFile.builder().build();

        assertFalse(file.getIsLongTermFormat());
        assertEquals("SHA256", file.getHashAlgorithm());
        assertFalse(file.getHasPreview());
        assertEquals(DigitalFile.CATEGORY_MAIN, file.getFileCategory());
        assertEquals(0, file.getSortOrder());
        assertFalse(file.getDeleted());
    }

    @Test
    void testNoArgsConstructor() {
        DigitalFile file = new DigitalFile();

        assertNull(file.getId());
        assertNull(file.getFileName());
    }

    @Test
    void testCategoryConstants() {
        assertEquals("MAIN", DigitalFile.CATEGORY_MAIN);
        assertEquals("ATTACHMENT", DigitalFile.CATEGORY_ATTACHMENT);
        assertEquals("COVER", DigitalFile.CATEGORY_COVER);
        assertEquals("CATALOG", DigitalFile.CATEGORY_CATALOG);
        assertEquals("EVIDENCE", DigitalFile.SECTION_EVIDENCE);
    }

    @Test
    void testOcrStatusConstants() {
        assertEquals("NONE", DigitalFile.OCR_NONE);
        assertEquals("PENDING", DigitalFile.OCR_PENDING);
        assertEquals("COMPLETED", DigitalFile.OCR_COMPLETED);
        assertEquals("FAILED", DigitalFile.OCR_FAILED);
    }

    @Test
    void testSettersAndGetters() {
        DigitalFile file = new DigitalFile();

        file.setId(2L);
        file.setFileName("image.jpg");
        file.setFileSize(500000L);
        file.setOcrStatus(DigitalFile.OCR_PENDING);

        assertEquals(2L, file.getId());
        assertEquals("image.jpg", file.getFileName());
        assertEquals(500000L, file.getFileSize());
        assertEquals(DigitalFile.OCR_PENDING, file.getOcrStatus());
    }

    @Test
    void testToString() {
        DigitalFile file = DigitalFile.builder()
                .id(1L)
                .fileName("test.pdf")
                .build();

        String str = file.toString();
        assertNotNull(str);
        assertTrue(str.contains("DigitalFile"));
    }

    @Test
    void testEqualsAndHashCode() {
        DigitalFile file1 = new DigitalFile();
        file1.setId(1L);
        file1.setFileName("test.pdf");

        DigitalFile file2 = new DigitalFile();
        file2.setId(1L);
        file2.setFileName("test.pdf");

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
    }
}
