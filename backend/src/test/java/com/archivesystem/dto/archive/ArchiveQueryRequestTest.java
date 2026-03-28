package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveQueryRequestTest {

    @Test
    void testDefaultValues() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();

        assertEquals(1, request.getPageNum());
        assertEquals(20, request.getPageSize());
        assertEquals("createdAt", request.getSortField());
        assertEquals("desc", request.getSortOrder());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        LocalDate archiveDateStart = LocalDate.of(2025, 1, 1);
        LocalDate archiveDateEnd = LocalDate.of(2026, 12, 31);
        LocalDate createdAtStart = LocalDate.of(2025, 6, 1);
        LocalDate createdAtEnd = LocalDate.of(2026, 6, 30);

        request.setKeyword("合同纠纷");
        request.setArchiveNo("ARCH-001");
        request.setFondsId(1L);
        request.setCategoryId(10L);
        request.setArchiveType("LITIGATION");
        request.setRetentionPeriod("PERMANENT");
        request.setSecurityLevel("NORMAL");
        request.setSourceType("LAW_FIRM");
        request.setStatus("STORED");
        request.setCaseNo("CASE-001");
        request.setArchiveDateStart(archiveDateStart);
        request.setArchiveDateEnd(archiveDateEnd);
        request.setCreatedAtStart(createdAtStart);
        request.setCreatedAtEnd(createdAtEnd);
        request.setPageNum(5);
        request.setPageSize(50);
        request.setSortField("archiveNo");
        request.setSortOrder("asc");

        assertEquals("合同纠纷", request.getKeyword());
        assertEquals("ARCH-001", request.getArchiveNo());
        assertEquals(1L, request.getFondsId());
        assertEquals(10L, request.getCategoryId());
        assertEquals("LITIGATION", request.getArchiveType());
        assertEquals("PERMANENT", request.getRetentionPeriod());
        assertEquals("NORMAL", request.getSecurityLevel());
        assertEquals("LAW_FIRM", request.getSourceType());
        assertEquals("STORED", request.getStatus());
        assertEquals("CASE-001", request.getCaseNo());
        assertEquals(archiveDateStart, request.getArchiveDateStart());
        assertEquals(archiveDateEnd, request.getArchiveDateEnd());
        assertEquals(createdAtStart, request.getCreatedAtStart());
        assertEquals(createdAtEnd, request.getCreatedAtEnd());
        assertEquals(5, request.getPageNum());
        assertEquals(50, request.getPageSize());
        assertEquals("archiveNo", request.getSortField());
        assertEquals("asc", request.getSortOrder());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveQueryRequest request1 = new ArchiveQueryRequest();
        request1.setKeyword("测试");
        request1.setPageNum(1);

        ArchiveQueryRequest request2 = new ArchiveQueryRequest();
        request2.setKeyword("测试");
        request2.setPageNum(1);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();
        request.setKeyword("搜索关键词");

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveQueryRequest"));
    }

    @Test
    void testNullValues() {
        ArchiveQueryRequest request = new ArchiveQueryRequest();

        assertNull(request.getKeyword());
        assertNull(request.getArchiveNo());
        assertNull(request.getFondsId());
        assertNull(request.getCategoryId());
        assertNull(request.getArchiveType());
    }
}
