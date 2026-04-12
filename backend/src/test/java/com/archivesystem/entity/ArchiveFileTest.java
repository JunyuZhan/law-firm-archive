package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveFileTest {

    @Test
    void testBuilder() {
        ArchiveFile file = ArchiveFile.builder()
                .archiveId(100L)
                .fileName("合同文件.pdf")
                .originalFileName("劳动合同-张三-2026.pdf")
                .fileType("application/pdf")
                .fileSize(1024000L)
                .storagePath("archives/2026/01/abc123.pdf")
                .category(ArchiveFile.CATEGORY_DOCUMENT)
                .sortOrder(1)
                .description("劳动合同正文")
                .sourceUrl("https://source.com/files/contract.pdf")
                .fileMd5("d41d8cd98f00b204e9800998ecf8427e")
                .build();

        assertEquals(100L, file.getArchiveId());
        assertEquals("合同文件.pdf", file.getFileName());
        assertEquals("劳动合同-张三-2026.pdf", file.getOriginalFileName());
        assertEquals("application/pdf", file.getFileType());
        assertEquals(1024000L, file.getFileSize());
        assertEquals("archives/2026/01/abc123.pdf", file.getStoragePath());
        assertEquals(ArchiveFile.CATEGORY_DOCUMENT, file.getCategory());
        assertEquals(1, file.getSortOrder());
        assertEquals("劳动合同正文", file.getDescription());
        assertEquals("https://source.com/files/contract.pdf", file.getSourceUrl());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", file.getFileMd5());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveFile file = new ArchiveFile();

        assertNull(file.getArchiveId());
        assertNull(file.getFileName());
    }

    @Test
    void testCategoryConstants() {
        assertEquals("COVER", ArchiveFile.CATEGORY_COVER);
        assertEquals("CATALOG", ArchiveFile.CATEGORY_CATALOG);
        assertEquals("DOCUMENT", ArchiveFile.CATEGORY_DOCUMENT);
        assertEquals("ATTACHMENT", ArchiveFile.CATEGORY_ATTACHMENT);
    }

    @Test
    void testSettersAndGetters() {
        ArchiveFile file = new ArchiveFile();

        file.setArchiveId(200L);
        file.setFileName("附件.docx");
        file.setOriginalFileName("补充协议.docx");
        file.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        file.setFileSize(512000L);
        file.setStoragePath("archives/2026/02/def456.docx");
        file.setCategory(ArchiveFile.CATEGORY_ATTACHMENT);
        file.setSortOrder(2);

        assertEquals(200L, file.getArchiveId());
        assertEquals("附件.docx", file.getFileName());
        assertEquals("补充协议.docx", file.getOriginalFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", file.getFileType());
        assertEquals(512000L, file.getFileSize());
        assertEquals("archives/2026/02/def456.docx", file.getStoragePath());
        assertEquals(ArchiveFile.CATEGORY_ATTACHMENT, file.getCategory());
        assertEquals(2, file.getSortOrder());
    }

    @Test
    void testCoverFile() {
        ArchiveFile cover = ArchiveFile.builder()
                .fileName("封面.jpg")
                .fileType("image/jpeg")
                .category(ArchiveFile.CATEGORY_COVER)
                .sortOrder(0)
                .build();

        assertEquals(ArchiveFile.CATEGORY_COVER, cover.getCategory());
        assertEquals(0, cover.getSortOrder());
    }

    @Test
    void testCatalogFile() {
        ArchiveFile catalog = ArchiveFile.builder()
                .fileName("目录.pdf")
                .fileType("application/pdf")
                .category(ArchiveFile.CATEGORY_CATALOG)
                .build();

        assertEquals(ArchiveFile.CATEGORY_CATALOG, catalog.getCategory());
    }

    @Test
    void testToString() {
        ArchiveFile file = ArchiveFile.builder()
                .fileName("test.pdf")
                .build();

        String str = file.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveFile"));
    }
}
