package com.archivesystem.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.archivesystem.document.ArchiveDocument;
import com.archivesystem.dto.archive.ArchiveSearchRequest;
import com.archivesystem.dto.archive.ArchiveSearchResult;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.elasticsearch.ArchiveSearchRepository;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.impl.ArchiveIndexServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchiveIndexServiceTest {

    @Mock
    private ArchiveSearchRepository archiveSearchRepository;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ArchiveIndexServiceImpl archiveIndexService;

    private Archive testArchive;
    private ArchiveDocument testDocument;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setTitle("测试档案");
        testArchive.setFondsId(1L);
        testArchive.setFondsNo("QZ001");
        testArchive.setCategoryId(1L);
        testArchive.setCategoryCode("CAT001");
        testArchive.setArchiveType("DOCUMENT");
        testArchive.setCaseNo("CASE001");
        testArchive.setCaseName("测试案件");
        testArchive.setClientName("测试客户");
        testArchive.setLawyerName("测试律师");
        testArchive.setKeywords("测试,档案");
        testArchive.setStatus(Archive.STATUS_RECEIVED);
        testArchive.setRetentionPeriod("PERMANENT");
        testArchive.setSecurityLevel("INTERNAL");
        testArchive.setSourceType("LAW_FIRM");
        testArchive.setArchiveDate(LocalDate.of(2026, 2, 13));
        testArchive.setReceivedAt(LocalDateTime.now());
        testArchive.setCreatedAt(LocalDateTime.now());
        testArchive.setFileCount(0);
        testArchive.setDeleted(false);

        testDocument = ArchiveDocument.builder()
                .id(1L)
                .archiveNo("ARC-20260213-0001")
                .title("测试档案")
                .fondsId(1L)
                .fondsNo("QZ001")
                .categoryId(1L)
                .categoryCode("CAT001")
                .archiveType("DOCUMENT")
                .status(Archive.STATUS_RECEIVED)
                .archiveYear(2026)
                .build();
    }

    @Test
    void testIndexArchive_Success() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        assertDoesNotThrow(() -> archiveIndexService.indexArchive(1L));

        verify(archiveSearchRepository).save(any(ArchiveDocument.class));
    }

    @Test
    void testIndexArchive_ArchiveNotFound() {
        when(archiveMapper.selectById(999L)).thenReturn(null);

        archiveIndexService.indexArchive(999L);

        verify(archiveSearchRepository, never()).save(any(ArchiveDocument.class));
    }

    @Test
    void testIndexArchive_ArchiveDeleted() {
        testArchive.setDeleted(true);
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);

        archiveIndexService.indexArchive(1L);

        verify(archiveSearchRepository, never()).save(any(ArchiveDocument.class));
    }

    @Test
    void testIndexArchive_WithFiles() {
        DigitalFile file1 = new DigitalFile();
        file1.setId(1L);
        file1.setFileName("test1.pdf");

        DigitalFile file2 = new DigitalFile();
        file2.setId(2L);
        file2.setFileName("test2.pdf");

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Arrays.asList(file1, file2));
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        archiveIndexService.indexArchive(1L);

        verify(archiveSearchRepository).save(argThat(doc -> 
            doc.getFileNames() != null && doc.getFileNames().size() == 2
        ));
    }

    @Test
    void testIndexArchive_ExceptionHandled() {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());
        when(archiveSearchRepository.save(any(ArchiveDocument.class)))
                .thenThrow(new RuntimeException("ES异常"));

        assertDoesNotThrow(() -> archiveIndexService.indexArchive(1L));
    }

    @Test
    void testIndexArchives_BatchIndex() {
        Archive archive2 = new Archive();
        archive2.setId(2L);
        archive2.setArchiveNo("ARC-20260213-0002");
        archive2.setTitle("测试档案2");
        archive2.setFondsId(1L);
        archive2.setFondsNo("QZ001");
        archive2.setArchiveType("DOCUMENT");
        archive2.setStatus(Archive.STATUS_RECEIVED);
        archive2.setReceivedAt(LocalDateTime.now());
        archive2.setDeleted(false);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.selectById(2L)).thenReturn(archive2);
        when(digitalFileMapper.selectByArchiveId(anyLong())).thenReturn(Collections.emptyList());
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        archiveIndexService.indexArchives(Arrays.asList(1L, 2L));

        verify(archiveSearchRepository, times(2)).save(any(ArchiveDocument.class));
    }

    @Test
    void testDeleteIndex_Success() {
        doNothing().when(archiveSearchRepository).deleteById(1L);

        assertDoesNotThrow(() -> archiveIndexService.deleteIndex(1L));

        verify(archiveSearchRepository).deleteById(1L);
    }

    @Test
    void testDeleteIndex_ExceptionHandled() {
        doThrow(new RuntimeException("删除失败")).when(archiveSearchRepository).deleteById(1L);

        assertDoesNotThrow(() -> archiveIndexService.deleteIndex(1L));
    }

    @Test
    void testRebuildAllIndexes_Success() {
        List<Archive> archives = Arrays.asList(testArchive);

        when(archiveMapper.selectPageForIndex(0, 100)).thenReturn(archives);
        when(archiveMapper.selectPageForIndex(100, 100)).thenReturn(Collections.emptyList());
        when(digitalFileMapper.selectByArchiveId(anyLong())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> archiveIndexService.rebuildAllIndexes());

        verify(archiveSearchRepository).deleteAll();
        verify(archiveSearchRepository).saveAll(anyList());
    }

    @Test
    void testRebuildAllIndexes_MultiplePages() {
        List<Archive> page1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Archive a = new Archive();
            a.setId((long) i);
            a.setArchiveNo("ARC-" + i);
            a.setTitle("档案" + i);
            a.setFondsNo("QZ001");
            a.setArchiveType("DOCUMENT");
            a.setStatus("RECEIVED");
            a.setReceivedAt(LocalDateTime.now());
            a.setDeleted(false);
            page1.add(a);
        }

        when(archiveMapper.selectPageForIndex(0, 100)).thenReturn(page1);
        when(archiveMapper.selectPageForIndex(100, 100)).thenReturn(Arrays.asList(testArchive));
        when(archiveMapper.selectPageForIndex(200, 100)).thenReturn(Collections.emptyList());
        when(digitalFileMapper.selectByArchiveId(anyLong())).thenReturn(Collections.emptyList());

        archiveIndexService.rebuildAllIndexes();

        verify(archiveSearchRepository, times(2)).saveAll(anyList());
    }

    @Test
    void testRebuildAllIndexes_ExceptionHandled() {
        doThrow(new RuntimeException("删除失败")).when(archiveSearchRepository).deleteAll();

        assertDoesNotThrow(() -> archiveIndexService.rebuildAllIndexes());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearch_WithKeyword() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setKeyword("测试");
        request.setPageNum(1);
        request.setPageSize(20);
        request.setHighlight(false);
        request.setAggregation(false);

        // Mock ES response
        SearchResponse<ArchiveDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<ArchiveDocument> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);
        
        Hit<ArchiveDocument> hit = mock(Hit.class);
        when(hit.source()).thenReturn(testDocument);
        when(hit.score()).thenReturn(1.5);
        when(hit.highlight()).thenReturn(Collections.emptyMap());

        when(mockTotal.value()).thenReturn(1L);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(Arrays.asList(hit));
        when(mockResponse.hits()).thenReturn(mockHits);
        when(mockResponse.took()).thenReturn(10L);
        when(mockResponse.aggregations()).thenReturn(null);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenReturn(mockResponse);

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getHits().size());
        assertEquals("ARC-20260213-0001", result.getHits().get(0).getArchiveNo());
    }

    @Test
    void testSearch_WithFilters() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setFondsId(1L);
        request.setCategoryId(1L);
        request.setArchiveType("DOCUMENT");
        request.setRetentionPeriod("PERMANENT");
        request.setSecurityLevel("INTERNAL");
        request.setStatus("RECEIVED");
        request.setSourceType("LAW_FIRM");
        request.setLawyerName("测试律师");
        request.setArchiveYear(2026);
        request.setArchiveDateStart(LocalDate.of(2026, 1, 1));
        request.setArchiveDateEnd(LocalDate.of(2026, 12, 31));
        request.setPageNum(1);
        request.setPageSize(20);
        request.setHighlight(false);
        request.setAggregation(false);

        // Mock ES response
        @SuppressWarnings("unchecked")
        SearchResponse<ArchiveDocument> mockResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<ArchiveDocument> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);

        when(mockTotal.value()).thenReturn(0L);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(Collections.emptyList());
        when(mockResponse.hits()).thenReturn(mockHits);
        when(mockResponse.took()).thenReturn(5L);
        when(mockResponse.aggregations()).thenReturn(null);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenReturn(mockResponse);

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void testSearch_WithSearchFields() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setKeyword("测试");
        request.setSearchFields(Arrays.asList("title", "caseName"));
        request.setPageNum(1);
        request.setPageSize(20);
        request.setHighlight(false);
        request.setAggregation(false);

        @SuppressWarnings("unchecked")
        SearchResponse<ArchiveDocument> mockResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<ArchiveDocument> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);

        when(mockTotal.value()).thenReturn(0L);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(Collections.emptyList());
        when(mockResponse.hits()).thenReturn(mockHits);
        when(mockResponse.took()).thenReturn(5L);
        when(mockResponse.aggregations()).thenReturn(null);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenReturn(mockResponse);

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
    }

    @Test
    void testSearch_WithSorting() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setSortField("title");
        request.setSortOrder("asc");
        request.setPageNum(1);
        request.setPageSize(20);
        request.setHighlight(false);
        request.setAggregation(false);

        @SuppressWarnings("unchecked")
        SearchResponse<ArchiveDocument> mockResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<ArchiveDocument> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);

        when(mockTotal.value()).thenReturn(0L);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(Collections.emptyList());
        when(mockResponse.hits()).thenReturn(mockHits);
        when(mockResponse.took()).thenReturn(5L);
        when(mockResponse.aggregations()).thenReturn(null);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenReturn(mockResponse);

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
    }

    @Test
    void testSearch_ExceptionReturnsEmptyResult() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setKeyword("测试");
        request.setPageNum(1);
        request.setPageSize(20);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenThrow(new RuntimeException("ES异常"));

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getHits().isEmpty());
    }

    @Test
    void testGetAggregations_Success() throws Exception {
        @SuppressWarnings("unchecked")
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        
        Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggs = new HashMap<>();
        
        // Mock aggregations
        when(mockResponse.aggregations()).thenReturn(aggs);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result = archiveIndexService.getAggregations();

        assertNotNull(result);
    }

    @Test
    void testGetAggregations_ExceptionReturnsEmpty() throws Exception {
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
                .thenThrow(new RuntimeException("ES异常"));

        Map<String, Object> result = archiveIndexService.getAggregations();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateFileContent_Success() {
        when(archiveSearchRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        assertDoesNotThrow(() -> archiveIndexService.updateFileContent(1L, "新的文件内容"));

        verify(archiveSearchRepository).save(argThat(doc -> 
            "新的文件内容".equals(doc.getFileContent())
        ));
    }

    @Test
    void testUpdateFileContent_DocumentNotFound() {
        when(archiveSearchRepository.findById(999L)).thenReturn(Optional.empty());

        archiveIndexService.updateFileContent(999L, "新的文件内容");

        verify(archiveSearchRepository, never()).save(any(ArchiveDocument.class));
    }

    @Test
    void testUpdateFileContent_ExceptionHandled() {
        when(archiveSearchRepository.findById(1L)).thenThrow(new RuntimeException("查询失败"));

        assertDoesNotThrow(() -> archiveIndexService.updateFileContent(1L, "新的文件内容"));
    }

    @Test
    void testIndexArchive_WithArchiveDateYear() {
        testArchive.setArchiveDate(LocalDate.of(2025, 6, 15));
        testArchive.setReceivedAt(LocalDateTime.of(2026, 2, 13, 10, 0, 0));

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        archiveIndexService.indexArchive(1L);

        verify(archiveSearchRepository).save(argThat(doc -> 
            doc.getArchiveYear() != null && doc.getArchiveYear() == 2025
        ));
    }

    @Test
    void testIndexArchive_WithReceivedAtYear() {
        testArchive.setArchiveDate(null);
        testArchive.setReceivedAt(LocalDateTime.of(2026, 2, 13, 10, 0, 0));

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(digitalFileMapper.selectByArchiveId(1L)).thenReturn(Collections.emptyList());
        when(archiveSearchRepository.save(any(ArchiveDocument.class))).thenReturn(testDocument);

        archiveIndexService.indexArchive(1L);

        verify(archiveSearchRepository).save(argThat(doc -> 
            doc.getArchiveYear() != null && doc.getArchiveYear() == 2026
        ));
    }

    @Test
    void testSearch_NullTotalHits() throws Exception {
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setPageNum(1);
        request.setPageSize(20);
        request.setHighlight(false);
        request.setAggregation(false);

        @SuppressWarnings("unchecked")
        SearchResponse<ArchiveDocument> mockResponse = mock(SearchResponse.class);
        @SuppressWarnings("unchecked")
        HitsMetadata<ArchiveDocument> mockHits = mock(HitsMetadata.class);

        when(mockHits.total()).thenReturn(null);
        when(mockHits.hits()).thenReturn(Collections.emptyList());
        when(mockResponse.hits()).thenReturn(mockHits);
        when(mockResponse.took()).thenReturn(5L);
        when(mockResponse.aggregations()).thenReturn(null);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(ArchiveDocument.class)))
                .thenReturn(mockResponse);

        ArchiveSearchResult result = archiveIndexService.search(request);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }
}
