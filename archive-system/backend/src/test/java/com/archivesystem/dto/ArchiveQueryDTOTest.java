package com.archivesystem.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveQueryDTOTest {

    @Test
    void testDefaultValues() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();

        assertEquals(1, dto.getPageNum());
        assertEquals(20, dto.getPageSize());
    }

    @Test
    void testSearchProperties() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();

        dto.setArchiveNo("ARCH-001");
        dto.setArchiveName("测试档案");
        dto.setArchiveType("LITIGATION");
        dto.setCategory("CIVIL");
        dto.setSourceType("LAW_FIRM");
        dto.setSourceNo("SRC-001");
        dto.setClientName("张三");
        dto.setResponsiblePerson("李律师");

        assertEquals("ARCH-001", dto.getArchiveNo());
        assertEquals("测试档案", dto.getArchiveName());
        assertEquals("LITIGATION", dto.getArchiveType());
        assertEquals("CIVIL", dto.getCategory());
        assertEquals("LAW_FIRM", dto.getSourceType());
        assertEquals("SRC-001", dto.getSourceNo());
        assertEquals("张三", dto.getClientName());
        assertEquals("李律师", dto.getResponsiblePerson());
    }

    @Test
    void testFilterProperties() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();

        dto.setLocationId(10L);
        dto.setStatus("STORED");
        dto.setRetentionPeriod("PERMANENT");
        dto.setKeyword("搜索关键词");

        assertEquals(10L, dto.getLocationId());
        assertEquals("STORED", dto.getStatus());
        assertEquals("PERMANENT", dto.getRetentionPeriod());
        assertEquals("搜索关键词", dto.getKeyword());
    }

    @Test
    void testDateRangeProperties() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();
        LocalDate caseCloseDateFrom = LocalDate.of(2025, 1, 1);
        LocalDate caseCloseDateTo = LocalDate.of(2026, 12, 31);
        LocalDate receivedAtFrom = LocalDate.of(2025, 6, 1);
        LocalDate receivedAtTo = LocalDate.of(2026, 6, 30);

        dto.setCaseCloseDateFrom(caseCloseDateFrom);
        dto.setCaseCloseDateTo(caseCloseDateTo);
        dto.setReceivedAtFrom(receivedAtFrom);
        dto.setReceivedAtTo(receivedAtTo);

        assertEquals(caseCloseDateFrom, dto.getCaseCloseDateFrom());
        assertEquals(caseCloseDateTo, dto.getCaseCloseDateTo());
        assertEquals(receivedAtFrom, dto.getReceivedAtFrom());
        assertEquals(receivedAtTo, dto.getReceivedAtTo());
    }

    @Test
    void testPaginationProperties() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();

        dto.setPageNum(5);
        dto.setPageSize(50);
        dto.setSortField("archiveNo");
        dto.setSortOrder("asc");

        assertEquals(5, dto.getPageNum());
        assertEquals(50, dto.getPageSize());
        assertEquals("archiveNo", dto.getSortField());
        assertEquals("asc", dto.getSortOrder());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveQueryDTO dto1 = new ArchiveQueryDTO();
        dto1.setArchiveNo("ARCH-001");
        dto1.setPageNum(1);

        ArchiveQueryDTO dto2 = new ArchiveQueryDTO();
        dto2.setArchiveNo("ARCH-001");
        dto2.setPageNum(1);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();
        dto.setArchiveName("测试查询");

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveQueryDTO"));
    }

    @Test
    void testSortOrderDesc() {
        ArchiveQueryDTO dto = new ArchiveQueryDTO();

        dto.setSortField("createdAt");
        dto.setSortOrder("desc");

        assertEquals("createdAt", dto.getSortField());
        assertEquals("desc", dto.getSortOrder());
    }
}
