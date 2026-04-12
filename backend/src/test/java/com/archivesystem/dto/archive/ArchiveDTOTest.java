package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveDTOTest {

    @Test
    void testBuilder() {
        LocalDate archiveDate = LocalDate.of(2026, 1, 15);
        LocalDate documentDate = LocalDate.of(2025, 12, 20);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 10, 30);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("custom", "value");
        List<DigitalFileDTO> files = new ArrayList<>();

        ArchiveDTO dto = ArchiveDTO.builder()
                .id(1L)
                .archiveNo("ARCH-2026-001")
                .fondsId(100L)
                .fondsNo("FD-001")
                .fondsName("测试全宗")
                .categoryId(10L)
                .categoryCode("CAT-001")
                .categoryName("民事案件")
                .archiveType("LITIGATION")
                .title("合同纠纷档案")
                .fileNo("FILE-001")
                .responsibility("李律师")
                .archiveDate(archiveDate)
                .documentDate(documentDate)
                .pageCount(100)
                .piecesCount(5)
                .retentionPeriod("PERMANENT")
                .retentionPeriodName("永久")
                .retentionExpireDate(null)
                .securityLevel("NORMAL")
                .securityExpireDate(null)
                .sourceType("LAW_FIRM")
                .sourceSystem("律所系统")
                .sourceId("SRC-001")
                .sourceNo("NO-001")
                .caseNo("CASE-001")
                .caseName("张三诉李四")
                .clientName("张三")
                .lawyerName("李律师")
                .caseCloseDate(documentDate)
                .hasElectronic(true)
                .storageLocation("档案室A区")
                .totalFileSize(10240000L)
                .fileCount(5)
                .status("STORED")
                .statusName("已入库")
                .receivedAt(createdAt)
                .receivedByName("管理员")
                .catalogedAt(createdAt)
                .catalogedByName("编目员")
                .archivedAt(createdAt)
                .archivedByName("归档员")
                .keywords("合同,纠纷")
                .archiveAbstract("案件摘要")
                .remarks("备注")
                .extraData(extraData)
                .files(files)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("ARCH-2026-001", dto.getArchiveNo());
        assertEquals(100L, dto.getFondsId());
        assertEquals("FD-001", dto.getFondsNo());
        assertEquals("测试全宗", dto.getFondsName());
        assertEquals(10L, dto.getCategoryId());
        assertEquals("CAT-001", dto.getCategoryCode());
        assertEquals("民事案件", dto.getCategoryName());
        assertEquals("LITIGATION", dto.getArchiveType());
        assertEquals("合同纠纷档案", dto.getTitle());
        assertEquals("FILE-001", dto.getFileNo());
        assertEquals("李律师", dto.getResponsibility());
        assertEquals(archiveDate, dto.getArchiveDate());
        assertEquals(documentDate, dto.getDocumentDate());
        assertEquals(100, dto.getPageCount());
        assertEquals(5, dto.getPiecesCount());
        assertEquals("PERMANENT", dto.getRetentionPeriod());
        assertEquals("永久", dto.getRetentionPeriodName());
        assertEquals("NORMAL", dto.getSecurityLevel());
        assertEquals("LAW_FIRM", dto.getSourceType());
        assertEquals("律所系统", dto.getSourceSystem());
        assertEquals("SRC-001", dto.getSourceId());
        assertEquals("NO-001", dto.getSourceNo());
        assertEquals("CASE-001", dto.getCaseNo());
        assertEquals("张三诉李四", dto.getCaseName());
        assertEquals("张三", dto.getClientName());
        assertEquals("李律师", dto.getLawyerName());
        assertTrue(dto.getHasElectronic());
        assertEquals("档案室A区", dto.getStorageLocation());
        assertEquals(10240000L, dto.getTotalFileSize());
        assertEquals(5, dto.getFileCount());
        assertEquals("STORED", dto.getStatus());
        assertEquals("已入库", dto.getStatusName());
        assertEquals(createdAt, dto.getReceivedAt());
        assertEquals("管理员", dto.getReceivedByName());
        assertEquals("编目员", dto.getCatalogedByName());
        assertEquals("归档员", dto.getArchivedByName());
        assertEquals("合同,纠纷", dto.getKeywords());
        assertEquals("案件摘要", dto.getArchiveAbstract());
        assertEquals("备注", dto.getRemarks());
        assertEquals(extraData, dto.getExtraData());
        assertEquals(files, dto.getFiles());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(createdAt, dto.getUpdatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveDTO dto = new ArchiveDTO();

        assertNull(dto.getId());
        assertNull(dto.getArchiveNo());
        assertNull(dto.getTitle());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        LocalDateTime dateTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        Map<String, Object> extraData = new HashMap<>();
        List<DigitalFileDTO> files = new ArrayList<>();

        ArchiveDTO dto = new ArchiveDTO(
                1L, "ARCH-001",
                1L, "FD-001", "全宗名", 1L, "CAT-001", "分类名", "TYPE",
                "标题", "FILE-001", "责任者", date, date, 10, 5,
                "PERMANENT", "永久", date, "NORMAL", date,
                "LAW_FIRM", "系统", "SRC-001", "NO-001",
                "CASE-001", "案件名", "委托人", "律师", date,
                "ELECTRONIC", true, false, 1L, "位置", "BOX-001", 1000L, 3,
                "STORED", "已存储",
                dateTime, "接收人", dateTime, "编目人", dateTime, "归档人",
                "关键词", "摘要", "备注", extraData, files,
                dateTime, dateTime
        );

        assertEquals(1L, dto.getId());
        assertEquals("ARCH-001", dto.getArchiveNo());
        assertEquals("标题", dto.getTitle());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveDTO dto = new ArchiveDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(2L);
        dto.setArchiveNo("ARCH-002");
        dto.setTitle("新档案");
        dto.setStatus("PENDING");
        dto.setCreatedAt(now);

        assertEquals(2L, dto.getId());
        assertEquals("ARCH-002", dto.getArchiveNo());
        assertEquals("新档案", dto.getTitle());
        assertEquals("PENDING", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveDTO dto1 = ArchiveDTO.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .title("测试档案")
                .build();

        ArchiveDTO dto2 = ArchiveDTO.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .title("测试档案")
                .build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveDTO dto = ArchiveDTO.builder()
                .id(1L)
                .title("测试")
                .build();

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveDTO"));
    }

    @Test
    void testFilesList() {
        ArchiveDTO dto = new ArchiveDTO();
        List<DigitalFileDTO> files = new ArrayList<>();
        files.add(DigitalFileDTO.builder().id(1L).fileName("file1.pdf").build());
        files.add(DigitalFileDTO.builder().id(2L).fileName("file2.pdf").build());

        dto.setFiles(files);

        assertEquals(2, dto.getFiles().size());
        assertEquals("file1.pdf", dto.getFiles().get(0).getFileName());
    }
}
