package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveTest {

    @Test
    void testBuilder() {
        LocalDate archiveDate = LocalDate.of(2026, 1, 15);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 1, 10, 10, 30);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("key", "value");

        Archive archive = Archive.builder()
                .archiveNo("ARCH-2026-001")
                .fondsId(1L)
                .fondsNo("FD-001")
                .categoryId(10L)
                .categoryCode("CAT-001")
                .archiveType(Archive.TYPE_DOCUMENT)
                .title("测试档案")
                .fileNo("FILE-001")
                .responsibility("李律师")
                .archiveDate(archiveDate)
                .documentDate(archiveDate)
                .pageCount(100)
                .piecesCount(5)
                .retentionPeriod("PERMANENT")
                .retentionExpireDate(null)
                .securityLevel(Archive.SECURITY_INTERNAL)
                .sourceType(Archive.SOURCE_LAW_FIRM)
                .sourceSystem("律所系统")
                .sourceId("SRC-001")
                .sourceNo("NO-001")
                .callbackUrl("https://callback.example.com")
                .caseNo("CASE-001")
                .caseName("张三诉李四")
                .clientName("张三")
                .lawyerName("李律师")
                .caseCloseDate(archiveDate)
                .hasElectronic(true)
                .storageLocation("档案室A区")
                .totalFileSize(10240000L)
                .fileCount(5)
                .status(Archive.STATUS_STORED)
                .receivedAt(receivedAt)
                .receivedBy(1L)
                .catalogedAt(receivedAt)
                .catalogedBy(2L)
                .archivedAt(receivedAt)
                .archivedBy(3L)
                .keywords("合同,纠纷")
                .archiveAbstract("案件摘要")
                .remarks("备注")
                .extraData(extraData)
                .build();

        assertEquals("ARCH-2026-001", archive.getArchiveNo());
        assertEquals(1L, archive.getFondsId());
        assertEquals("FD-001", archive.getFondsNo());
        assertEquals(10L, archive.getCategoryId());
        assertEquals("CAT-001", archive.getCategoryCode());
        assertEquals(Archive.TYPE_DOCUMENT, archive.getArchiveType());
        assertEquals("测试档案", archive.getTitle());
        assertEquals("FILE-001", archive.getFileNo());
        assertEquals("李律师", archive.getResponsibility());
        assertEquals(archiveDate, archive.getArchiveDate());
        assertEquals(100, archive.getPageCount());
        assertEquals(5, archive.getPiecesCount());
        assertEquals("PERMANENT", archive.getRetentionPeriod());
        assertEquals(Archive.SECURITY_INTERNAL, archive.getSecurityLevel());
        assertEquals(Archive.SOURCE_LAW_FIRM, archive.getSourceType());
        assertEquals("律所系统", archive.getSourceSystem());
        assertEquals("SRC-001", archive.getSourceId());
        assertEquals("https://callback.example.com", archive.getCallbackUrl());
        assertEquals("CASE-001", archive.getCaseNo());
        assertEquals("张三诉李四", archive.getCaseName());
        assertEquals("张三", archive.getClientName());
        assertEquals("李律师", archive.getLawyerName());
        assertTrue(archive.getHasElectronic());
        assertEquals("档案室A区", archive.getStorageLocation());
        assertEquals(10240000L, archive.getTotalFileSize());
        assertEquals(5, archive.getFileCount());
        assertEquals(Archive.STATUS_STORED, archive.getStatus());
        assertEquals(receivedAt, archive.getReceivedAt());
        assertEquals(1L, archive.getReceivedBy());
        assertEquals("合同,纠纷", archive.getKeywords());
        assertEquals("案件摘要", archive.getArchiveAbstract());
        assertEquals("备注", archive.getRemarks());
        assertEquals(extraData, archive.getExtraData());
    }

    @Test
    void testDefaultValues() {
        Archive archive = Archive.builder().build();

        assertEquals(1, archive.getPiecesCount());
        assertEquals(Archive.SECURITY_INTERNAL, archive.getSecurityLevel());
        assertTrue(archive.getHasElectronic());
        assertEquals(0L, archive.getTotalFileSize());
        assertEquals(0, archive.getFileCount());
        assertEquals(Archive.STATUS_RECEIVED, archive.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        Archive archive = new Archive();

        assertNull(archive.getArchiveNo());
        assertNull(archive.getTitle());
    }

    @Test
    void testStatusConstants() {
        assertEquals("DRAFT", Archive.STATUS_DRAFT);
        assertEquals("RECEIVED", Archive.STATUS_RECEIVED);
        assertEquals("CATALOGING", Archive.STATUS_CATALOGING);
        assertEquals("STORED", Archive.STATUS_STORED);
        assertEquals("BORROWED", Archive.STATUS_BORROWED);
        assertEquals("APPRAISAL", Archive.STATUS_APPRAISAL);
        assertEquals("DESTROYED", Archive.STATUS_DESTROYED);
        assertEquals("PROCESSING", Archive.STATUS_PROCESSING);
        assertEquals("PARTIAL", Archive.STATUS_PARTIAL);
        assertEquals("FAILED", Archive.STATUS_FAILED);
    }

    @Test
    void testSourceTypeConstants() {
        assertEquals("LAW_FIRM", Archive.SOURCE_LAW_FIRM);
        assertEquals("MANUAL", Archive.SOURCE_MANUAL);
        assertEquals("IMPORT", Archive.SOURCE_IMPORT);
        assertEquals("TRANSFER", Archive.SOURCE_TRANSFER);
    }

    @Test
    void testArchiveTypeConstants() {
        assertEquals("DOCUMENT", Archive.TYPE_DOCUMENT);
        assertEquals("SCIENCE", Archive.TYPE_SCIENCE);
        assertEquals("ACCOUNTING", Archive.TYPE_ACCOUNTING);
        assertEquals("PERSONNEL", Archive.TYPE_PERSONNEL);
        assertEquals("SPECIAL", Archive.TYPE_SPECIAL);
        assertEquals("AUDIOVISUAL", Archive.TYPE_AUDIOVISUAL);
    }

    @Test
    void testSecurityLevelConstants() {
        assertEquals("PUBLIC", Archive.SECURITY_PUBLIC);
        assertEquals("INTERNAL", Archive.SECURITY_INTERNAL);
        assertEquals("SECRET", Archive.SECURITY_SECRET);
        assertEquals("CONFIDENTIAL", Archive.SECURITY_CONFIDENTIAL);
    }

    @Test
    void testSettersAndGetters() {
        Archive archive = new Archive();

        archive.setArchiveNo("ARCH-002");
        archive.setTitle("新档案");
        archive.setStatus(Archive.STATUS_DRAFT);

        assertEquals("ARCH-002", archive.getArchiveNo());
        assertEquals("新档案", archive.getTitle());
        assertEquals(Archive.STATUS_DRAFT, archive.getStatus());
    }

    @Test
    void testToString() {
        Archive archive = Archive.builder()
                .archiveNo("ARCH-001")
                .title("测试")
                .build();

        String str = archive.toString();
        assertNotNull(str);
        assertTrue(str.contains("Archive"));
    }
}
