package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSearchRequestTest {

    @Test
    void testDefaultValues() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();

        assertTrue(request.getHighlight());
        assertFalse(request.getAggregation());
        assertEquals("receivedAt", request.getSortField());
        assertEquals("desc", request.getSortOrder());
        assertEquals(1, request.getPageNum());
        assertEquals(20, request.getPageSize());
    }

    @Test
    void testGetFrom_FirstPage() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setPageNum(1);
        request.setPageSize(20);

        assertEquals(0, request.getFrom());
    }

    @Test
    void testGetFrom_SecondPage() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setPageNum(2);
        request.setPageSize(20);

        assertEquals(20, request.getFrom());
    }

    @Test
    void testGetFrom_ThirdPage() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setPageNum(3);
        request.setPageSize(10);

        assertEquals(20, request.getFrom());
    }

    @Test
    void testSettersAndGetters() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        request.setKeyword("合同纠纷");
        request.setSearchFields(Arrays.asList("title", "keywords", "fileContent"));
        request.setFondsId(1L);
        request.setCategoryId(10L);
        request.setArchiveType("LITIGATION");
        request.setRetentionPeriod("PERMANENT");
        request.setSecurityLevel("NORMAL");
        request.setStatus("STORED");
        request.setSourceType("LAW_FIRM");
        request.setLawyerName("李律师");
        request.setArchiveDateStart(startDate);
        request.setArchiveDateEnd(endDate);
        request.setArchiveYear(2026);
        request.setHighlight(false);
        request.setAggregation(true);
        request.setSortField("archiveDate");
        request.setSortOrder("asc");
        request.setPageNum(5);
        request.setPageSize(50);

        assertEquals("合同纠纷", request.getKeyword());
        assertEquals(3, request.getSearchFields().size());
        assertTrue(request.getSearchFields().contains("title"));
        assertEquals(1L, request.getFondsId());
        assertEquals(10L, request.getCategoryId());
        assertEquals("LITIGATION", request.getArchiveType());
        assertEquals("PERMANENT", request.getRetentionPeriod());
        assertEquals("NORMAL", request.getSecurityLevel());
        assertEquals("STORED", request.getStatus());
        assertEquals("LAW_FIRM", request.getSourceType());
        assertEquals("李律师", request.getLawyerName());
        assertEquals(startDate, request.getArchiveDateStart());
        assertEquals(endDate, request.getArchiveDateEnd());
        assertEquals(2026, request.getArchiveYear());
        assertFalse(request.getHighlight());
        assertTrue(request.getAggregation());
        assertEquals("archiveDate", request.getSortField());
        assertEquals("asc", request.getSortOrder());
        assertEquals(5, request.getPageNum());
        assertEquals(50, request.getPageSize());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveSearchRequest request1 = new ArchiveSearchRequest();
        request1.setKeyword("测试");
        request1.setPageNum(1);

        ArchiveSearchRequest request2 = new ArchiveSearchRequest();
        request2.setKeyword("测试");
        request2.setPageNum(1);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setKeyword("搜索关键词");

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveSearchRequest"));
    }
}
