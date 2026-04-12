package com.archivesystem.document;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveDocumentTest {

    @Test
    void testBuilder() {
        LocalDate archiveDate = LocalDate.of(2026, 1, 15);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 10, 30);
        List<String> fileNames = Arrays.asList("file1.pdf", "file2.docx");

        ArchiveDocument doc = ArchiveDocument.builder()
                .id(1L)
                .archiveNo("ARCH-2026-001")
                .title("合同纠纷案件档案")
                .fondsId(100L)
                .fondsNo("FD-001")
                .categoryId(10L)
                .categoryCode("CAT-001")
                .archiveType("LITIGATION")
                .responsibility("李律师")
                .caseNo("CASE-2026-001")
                .caseName("张三诉李四合同纠纷")
                .clientName("张三")
                .lawyerName("李律师")
                .keywords("合同,纠纷,民事")
                .archiveAbstract("这是一个合同纠纷案件")
                .remarks("备注信息")
                .fileContent("OCR提取的文本内容")
                .retentionPeriod("PERMANENT")
                .securityLevel("NORMAL")
                .sourceType("LAW_FIRM")
                .status("STORED")
                .archiveDate(archiveDate)
                .documentDate(archiveDate)
                .receivedAt(createdAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .fileCount(5)
                .archiveYear(2026)
                .fileNames(fileNames)
                .build();

        assertEquals(1L, doc.getId());
        assertEquals("ARCH-2026-001", doc.getArchiveNo());
        assertEquals("合同纠纷案件档案", doc.getTitle());
        assertEquals(100L, doc.getFondsId());
        assertEquals("FD-001", doc.getFondsNo());
        assertEquals(10L, doc.getCategoryId());
        assertEquals("CAT-001", doc.getCategoryCode());
        assertEquals("LITIGATION", doc.getArchiveType());
        assertEquals("李律师", doc.getResponsibility());
        assertEquals("CASE-2026-001", doc.getCaseNo());
        assertEquals("张三诉李四合同纠纷", doc.getCaseName());
        assertEquals("张三", doc.getClientName());
        assertEquals("李律师", doc.getLawyerName());
        assertEquals("合同,纠纷,民事", doc.getKeywords());
        assertEquals("这是一个合同纠纷案件", doc.getArchiveAbstract());
        assertEquals("备注信息", doc.getRemarks());
        assertEquals("OCR提取的文本内容", doc.getFileContent());
        assertEquals("PERMANENT", doc.getRetentionPeriod());
        assertEquals("NORMAL", doc.getSecurityLevel());
        assertEquals("LAW_FIRM", doc.getSourceType());
        assertEquals("STORED", doc.getStatus());
        assertEquals(archiveDate, doc.getArchiveDate());
        assertEquals(archiveDate, doc.getDocumentDate());
        assertEquals(createdAt, doc.getReceivedAt());
        assertEquals(createdAt, doc.getCreatedAt());
        assertEquals(createdAt, doc.getUpdatedAt());
        assertEquals(5, doc.getFileCount());
        assertEquals(2026, doc.getArchiveYear());
        assertEquals(fileNames, doc.getFileNames());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveDocument doc = new ArchiveDocument();

        assertNull(doc.getId());
        assertNull(doc.getArchiveNo());
        assertNull(doc.getTitle());
        assertNull(doc.getFondsId());
        assertNull(doc.getCategoryId());
        assertNull(doc.getArchiveType());
        assertNull(doc.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 1, 12, 0);
        List<String> files = Arrays.asList("test.pdf");

        ArchiveDocument doc = new ArchiveDocument(
                1L, "ARCH-001", "标题", 1L, "FD-001", 1L, "CAT-001",
                "TYPE", "责任者", "CASE-001", "案件名", "委托人",
                "律师", "关键词", "摘要", "备注", "文件内容",
                "PERMANENT", "SECRET", "EXTERNAL", "ACTIVE",
                date, date, dateTime, dateTime, dateTime,
                10, 2026, files
        );

        assertEquals(1L, doc.getId());
        assertEquals("ARCH-001", doc.getArchiveNo());
        assertEquals("标题", doc.getTitle());
        assertEquals("TYPE", doc.getArchiveType());
        assertEquals(10, doc.getFileCount());
        assertEquals(files, doc.getFileNames());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveDocument doc = new ArchiveDocument();
        LocalDate archiveDate = LocalDate.of(2026, 3, 1);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 9, 0);

        doc.setId(2L);
        doc.setArchiveNo("ARCH-002");
        doc.setTitle("新档案");
        doc.setFondsId(200L);
        doc.setFondsNo("FD-002");
        doc.setCategoryId(20L);
        doc.setCategoryCode("CAT-002");
        doc.setArchiveType("CONSULTATION");
        doc.setResponsibility("王律师");
        doc.setCaseNo("CASE-002");
        doc.setCaseName("咨询案件");
        doc.setClientName("王五");
        doc.setLawyerName("王律师");
        doc.setKeywords("咨询");
        doc.setArchiveAbstract("咨询摘要");
        doc.setRemarks("新备注");
        doc.setFileContent("新文件内容");
        doc.setRetentionPeriod("TEN_YEARS");
        doc.setSecurityLevel("CONFIDENTIAL");
        doc.setSourceType("COURT");
        doc.setStatus("PENDING");
        doc.setArchiveDate(archiveDate);
        doc.setDocumentDate(archiveDate);
        doc.setReceivedAt(createdAt);
        doc.setCreatedAt(createdAt);
        doc.setUpdatedAt(createdAt);
        doc.setFileCount(3);
        doc.setArchiveYear(2026);
        doc.setFileNames(Arrays.asList("doc1.pdf", "doc2.pdf", "doc3.pdf"));

        assertEquals(2L, doc.getId());
        assertEquals("ARCH-002", doc.getArchiveNo());
        assertEquals("新档案", doc.getTitle());
        assertEquals(200L, doc.getFondsId());
        assertEquals("FD-002", doc.getFondsNo());
        assertEquals(20L, doc.getCategoryId());
        assertEquals("CAT-002", doc.getCategoryCode());
        assertEquals("CONSULTATION", doc.getArchiveType());
        assertEquals("王律师", doc.getResponsibility());
        assertEquals("CASE-002", doc.getCaseNo());
        assertEquals("咨询案件", doc.getCaseName());
        assertEquals("王五", doc.getClientName());
        assertEquals("王律师", doc.getLawyerName());
        assertEquals("咨询", doc.getKeywords());
        assertEquals("咨询摘要", doc.getArchiveAbstract());
        assertEquals("新备注", doc.getRemarks());
        assertEquals("新文件内容", doc.getFileContent());
        assertEquals("TEN_YEARS", doc.getRetentionPeriod());
        assertEquals("CONFIDENTIAL", doc.getSecurityLevel());
        assertEquals("COURT", doc.getSourceType());
        assertEquals("PENDING", doc.getStatus());
        assertEquals(archiveDate, doc.getArchiveDate());
        assertEquals(archiveDate, doc.getDocumentDate());
        assertEquals(createdAt, doc.getReceivedAt());
        assertEquals(createdAt, doc.getCreatedAt());
        assertEquals(createdAt, doc.getUpdatedAt());
        assertEquals(3, doc.getFileCount());
        assertEquals(2026, doc.getArchiveYear());
        assertEquals(3, doc.getFileNames().size());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveDocument doc1 = ArchiveDocument.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .title("测试档案")
                .build();

        ArchiveDocument doc2 = ArchiveDocument.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .title("测试档案")
                .build();

        assertEquals(doc1, doc2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    @Test
    void testNotEquals() {
        ArchiveDocument doc1 = ArchiveDocument.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .build();

        ArchiveDocument doc2 = ArchiveDocument.builder()
                .id(2L)
                .archiveNo("ARCH-002")
                .build();

        assertNotEquals(doc1, doc2);
    }

    @Test
    void testToString() {
        ArchiveDocument doc = ArchiveDocument.builder()
                .id(1L)
                .title("测试档案")
                .archiveNo("ARCH-001")
                .build();

        String str = doc.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveDocument"));
    }

    @Test
    void testFileNamesList() {
        ArchiveDocument doc = new ArchiveDocument();
        List<String> fileNames = Arrays.asList("合同.pdf", "证据1.jpg", "证据2.jpg", "判决书.pdf");

        doc.setFileNames(fileNames);

        assertEquals(4, doc.getFileNames().size());
        assertTrue(doc.getFileNames().contains("合同.pdf"));
        assertTrue(doc.getFileNames().contains("判决书.pdf"));
    }
}
