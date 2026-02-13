package com.archivesystem.dto.archive;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSearchResultTest {

    @Test
    void testBuilder() {
        List<ArchiveSearchResult.ArchiveSearchHit> hits = new ArrayList<>();
        ArchiveSearchResult.Aggregations aggregations = ArchiveSearchResult.Aggregations.builder().build();

        ArchiveSearchResult result = ArchiveSearchResult.builder()
                .hits(hits)
                .total(100L)
                .pageNum(1)
                .pageSize(20)
                .totalPages(5)
                .took(50L)
                .aggregations(aggregations)
                .build();

        assertEquals(hits, result.getHits());
        assertEquals(100L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(20, result.getPageSize());
        assertEquals(5, result.getTotalPages());
        assertEquals(50L, result.getTook());
        assertNotNull(result.getAggregations());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveSearchResult result = new ArchiveSearchResult();

        assertNull(result.getHits());
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getPageNum());
    }

    @Test
    void testAllArgsConstructor() {
        List<ArchiveSearchResult.ArchiveSearchHit> hits = new ArrayList<>();
        ArchiveSearchResult.Aggregations aggs = new ArchiveSearchResult.Aggregations();

        ArchiveSearchResult result = new ArchiveSearchResult(
                hits, 50L, 2, 10, 5, 100L, aggs
        );

        assertEquals(50L, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void testArchiveSearchHit() {
        Map<String, List<String>> highlights = new HashMap<>();
        highlights.put("title", Arrays.asList("<em>合同</em>纠纷"));

        ArchiveSearchResult.ArchiveSearchHit hit = ArchiveSearchResult.ArchiveSearchHit.builder()
                .id(1L)
                .archiveNo("ARCH-001")
                .title("合同纠纷案件")
                .fondsNo("FD-001")
                .categoryCode("CAT-001")
                .archiveType("LITIGATION")
                .caseNo("CASE-001")
                .caseName("张三诉李四")
                .clientName("张三")
                .lawyerName("李律师")
                .retentionPeriod("PERMANENT")
                .securityLevel("NORMAL")
                .status("STORED")
                .fileCount(5)
                .receivedAt("2026-01-15T10:30:00")
                .score(1.5f)
                .highlights(highlights)
                .build();

        assertEquals(1L, hit.getId());
        assertEquals("ARCH-001", hit.getArchiveNo());
        assertEquals("合同纠纷案件", hit.getTitle());
        assertEquals("FD-001", hit.getFondsNo());
        assertEquals("CAT-001", hit.getCategoryCode());
        assertEquals("LITIGATION", hit.getArchiveType());
        assertEquals("CASE-001", hit.getCaseNo());
        assertEquals("张三诉李四", hit.getCaseName());
        assertEquals("张三", hit.getClientName());
        assertEquals("李律师", hit.getLawyerName());
        assertEquals("PERMANENT", hit.getRetentionPeriod());
        assertEquals("NORMAL", hit.getSecurityLevel());
        assertEquals("STORED", hit.getStatus());
        assertEquals(5, hit.getFileCount());
        assertEquals("2026-01-15T10:30:00", hit.getReceivedAt());
        assertEquals(1.5f, hit.getScore());
        assertNotNull(hit.getHighlights());
    }

    @Test
    void testArchiveSearchHit_NoArgsConstructor() {
        ArchiveSearchResult.ArchiveSearchHit hit = new ArchiveSearchResult.ArchiveSearchHit();

        assertNull(hit.getId());
        assertNull(hit.getTitle());
    }

    @Test
    void testArchiveSearchHit_AllArgsConstructor() {
        Map<String, List<String>> highlights = new HashMap<>();

        ArchiveSearchResult.ArchiveSearchHit hit = new ArchiveSearchResult.ArchiveSearchHit(
                1L, "ARCH-001", "标题", "FD-001", "CAT-001",
                "TYPE", "CASE-001", "案件名", "委托人", "律师",
                "PERMANENT", "NORMAL", "STORED", 3,
                "2026-01-01", 2.0f, highlights
        );

        assertEquals(1L, hit.getId());
        assertEquals("ARCH-001", hit.getArchiveNo());
    }

    @Test
    void testAggregations() {
        List<ArchiveSearchResult.BucketItem> byType = Arrays.asList(
                ArchiveSearchResult.BucketItem.builder().key("LITIGATION").count(10).build()
        );
        List<ArchiveSearchResult.BucketItem> byYear = Arrays.asList(
                ArchiveSearchResult.BucketItem.builder().key("2026").count(50).build()
        );

        ArchiveSearchResult.Aggregations aggs = ArchiveSearchResult.Aggregations.builder()
                .byArchiveType(byType)
                .byYear(byYear)
                .byFonds(new ArrayList<>())
                .byStatus(new ArrayList<>())
                .byRetentionPeriod(new ArrayList<>())
                .build();

        assertEquals(1, aggs.getByArchiveType().size());
        assertEquals(1, aggs.getByYear().size());
        assertEquals(0, aggs.getByFonds().size());
    }

    @Test
    void testAggregations_NoArgsConstructor() {
        ArchiveSearchResult.Aggregations aggs = new ArchiveSearchResult.Aggregations();

        assertNull(aggs.getByArchiveType());
        assertNull(aggs.getByYear());
    }

    @Test
    void testBucketItem() {
        ArchiveSearchResult.BucketItem item = ArchiveSearchResult.BucketItem.builder()
                .key("LITIGATION")
                .count(100)
                .build();

        assertEquals("LITIGATION", item.getKey());
        assertEquals(100, item.getCount());
    }

    @Test
    void testBucketItem_SettersAndGetters() {
        ArchiveSearchResult.BucketItem item = new ArchiveSearchResult.BucketItem();

        item.setKey("TEST");
        item.setCount(50);

        assertEquals("TEST", item.getKey());
        assertEquals(50, item.getCount());
    }

    @Test
    void testEqualsAndHashCode() {
        ArchiveSearchResult result1 = ArchiveSearchResult.builder()
                .total(100L)
                .pageNum(1)
                .build();

        ArchiveSearchResult result2 = ArchiveSearchResult.builder()
                .total(100L)
                .pageNum(1)
                .build();

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testToString() {
        ArchiveSearchResult result = ArchiveSearchResult.builder()
                .total(10L)
                .build();

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveSearchResult"));
    }
}
