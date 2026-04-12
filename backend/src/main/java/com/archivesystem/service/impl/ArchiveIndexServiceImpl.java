package com.archivesystem.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.archivesystem.document.ArchiveDocument;
import com.archivesystem.dto.archive.ArchiveSearchRequest;
import com.archivesystem.dto.archive.ArchiveSearchResult;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.elasticsearch.ArchiveSearchRepository;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ArchiveIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 档案索引服务实现
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveIndexServiceImpl implements ArchiveIndexService {

    private final ArchiveSearchRepository archiveSearchRepository;
    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final ElasticsearchClient elasticsearchClient;

    private static final String INDEX_NAME = "archives";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 【安全】允许搜索的字段白名单
    private static final Set<String> ALLOWED_SEARCH_FIELDS = Set.of(
            "title", "archiveNo", "caseNo", "caseName", "clientName", 
            "lawyerName", "keywords", "archiveAbstract", "fileContent", "remarks"
    );
    
    // 【安全】允许排序的字段白名单
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "receivedAt", "archiveDate", "createdAt", "updatedAt", 
            "retentionExpireDate", "archiveNo", "title"
    );

    private static final List<String> DEFAULT_SEARCH_FIELDS = List.of(
            "title", "archiveNo", "caseNo", "caseName", "clientName", "keywords", "archiveAbstract", "remarks"
    );

    @Override
    @Async
    public void indexArchive(Long archiveId) {
        try {
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive == null || Boolean.TRUE.equals(archive.getDeleted())) {
                log.warn("档案不存在或已删除，跳过索引: archiveId={}", archiveId);
                return;
            }

            ArchiveDocument document = convertToDocument(archive);
            archiveSearchRepository.save(document);
            log.info("档案索引成功: archiveId={}, archiveNo={}", archiveId, archive.getArchiveNo());
        } catch (Exception e) {
            log.error("档案索引失败: archiveId={}", archiveId, e);
        }
    }

    @Override
    @Async
    public void indexArchives(List<Long> archiveIds) {
        log.info("批量索引档案: count={}", archiveIds.size());
        for (Long archiveId : archiveIds) {
            indexArchive(archiveId);
        }
    }

    @Override
    public void deleteIndex(Long archiveId) {
        try {
            archiveSearchRepository.deleteById(archiveId);
            log.info("删除档案索引: archiveId={}", archiveId);
        } catch (Exception e) {
            log.error("删除档案索引失败: archiveId={}", archiveId, e);
        }
    }

    @Override
    @Async
    public void rebuildAllIndexes() {
        log.info("开始重建所有档案索引");
        try {
            // 删除所有索引
            archiveSearchRepository.deleteAll();

            // 批量查询并索引
            int pageNum = 1;
            int pageSize = 100;
            int total = 0;

            while (true) {
                List<Archive> archives = archiveMapper.selectPageForIndex((pageNum - 1) * pageSize, pageSize);
                if (archives.isEmpty()) {
                    break;
                }

                List<Long> archiveIds = archives.stream()
                        .map(Archive::getId)
                        .filter(Objects::nonNull)
                        .toList();
                Map<Long, List<DigitalFile>> fileMap = loadFilesByArchiveIds(archiveIds);

                List<ArchiveDocument> documents = archives.stream()
                        .map(archive -> convertToDocument(archive, fileMap.getOrDefault(archive.getId(), Collections.emptyList())))
                        .collect(Collectors.toList());

                archiveSearchRepository.saveAll(documents);
                total += documents.size();
                log.info("索引进度: {}条", total);
                pageNum++;
            }

            log.info("重建索引完成: 共{}条", total);
        } catch (Exception e) {
            log.error("重建索引失败", e);
        }
    }

    @Override
    public ArchiveSearchResult search(ArchiveSearchRequest request) {
        try {
            // 构建查询
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            // 关键词搜索
            if (StringUtils.hasText(request.getKeyword())) {
                List<String> searchFields = request.getSearchFields();
                if (searchFields == null || searchFields.isEmpty()) {
                    searchFields = new ArrayList<>(DEFAULT_SEARCH_FIELDS);
                } else {
                    // 【安全】过滤搜索字段，只保留白名单中的字段
                    searchFields = searchFields.stream()
                            .filter(ALLOWED_SEARCH_FIELDS::contains)
                            .collect(Collectors.toList());
                    if (searchFields.isEmpty()) {
                        searchFields = Arrays.asList("title", "archiveNo", "caseNo", "caseName");
                    }
                }
                if (Boolean.TRUE.equals(request.getIncludeFileContent()) && !searchFields.contains("fileContent")) {
                    searchFields = new ArrayList<>(searchFields);
                    searchFields.add("fileContent");
                }

                List<Query> shouldQueries = new ArrayList<>();
                for (String field : searchFields) {
                    shouldQueries.add(Query.of(q -> q
                            .match(m -> m.field(field).query(request.getKeyword()).boost(getFieldBoost(field)))));
                }

                boolBuilder.must(Query.of(q -> q.bool(b -> b.should(shouldQueries).minimumShouldMatch("1"))));
            }

            // 过滤条件
            addFilters(boolBuilder, request);

            // 构建高亮
            Highlight highlight = null;
            if (Boolean.TRUE.equals(request.getHighlight())) {
                highlight = buildHighlight(request);
            }

            // 构建聚合
            Map<String, Aggregation> aggregations = null;
            if (Boolean.TRUE.equals(request.getAggregation())) {
                aggregations = buildAggregations();
            }

            // 执行搜索
            final Highlight finalHighlight = highlight;
            final Map<String, Aggregation> finalAggregations = aggregations;
            
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(Query.of(q -> q.bool(boolBuilder.build())))
                    .from(request.getFrom())
                    .size(request.getPageSize());

            // 排序 - 【安全】白名单校验
            String sortField = "receivedAt"; // 默认排序字段
            if (StringUtils.hasText(request.getSortField()) && ALLOWED_SORT_FIELDS.contains(request.getSortField())) {
                sortField = request.getSortField();
            }
            // 【安全】sortOrder 只允许 asc 或 desc
            SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
            final String finalSortField = sortField;
            searchBuilder.sort(s -> s.field(f -> f.field(finalSortField).order(sortOrder)));

            if (finalHighlight != null) {
                searchBuilder.highlight(finalHighlight);
            }
            if (finalAggregations != null) {
                searchBuilder.aggregations(finalAggregations);
            }

            SearchResponse<ArchiveDocument> response = elasticsearchClient.search(
                    searchBuilder.build(), ArchiveDocument.class);

            // 转换结果
            return convertSearchResponse(response, request);

        } catch (Exception e) {
            log.error("搜索失败: keyword={}", request.getKeyword(), e);
            return ArchiveSearchResult.builder()
                    .hits(Collections.emptyList())
                    .total(0)
                    .pageNum(request.getPageNum())
                    .pageSize(request.getPageSize())
                    .totalPages(0)
                    .build();
        }
    }

    @Override
    public Map<String, Object> getAggregations() {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .aggregations(buildAggregations()));

            SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

            Map<String, Object> result = new HashMap<>();
            if (response.aggregations() != null) {
                response.aggregations().forEach((name, agg) -> {
                    if (agg.isSterms()) {
                        List<Map<String, Object>> buckets = agg.sterms().buckets().array().stream()
                                .map(b -> {
                                    Map<String, Object> bucket = new HashMap<>();
                                    bucket.put("key", b.key().stringValue());
                                    bucket.put("count", b.docCount());
                                    return bucket;
                                })
                                .collect(Collectors.toList());
                        result.put(name, buckets);
                    }
                });
            }
            return result;
        } catch (Exception e) {
            log.error("获取聚合统计失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async
    public void updateFileContent(Long archiveId, String content) {
        try {
            Optional<ArchiveDocument> optional = archiveSearchRepository.findById(archiveId);
            if (optional.isPresent()) {
                ArchiveDocument document = optional.get();
                document.setFileContent(content);
                archiveSearchRepository.save(document);
                log.info("更新文件内容索引成功: archiveId={}", archiveId);
            }
        } catch (Exception e) {
            log.error("更新文件内容索引失败: archiveId={}", archiveId, e);
        }
    }

    /**
     * 转换为ES文档
     */
    private ArchiveDocument convertToDocument(Archive archive) {
        return convertToDocument(archive, null);
    }

    private ArchiveDocument convertToDocument(Archive archive, List<DigitalFile> files) {
        ArchiveDocument document = ArchiveDocument.builder()
                .id(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .title(archive.getTitle())
                .fondsId(archive.getFondsId())
                .fondsNo(archive.getFondsNo())
                .categoryId(archive.getCategoryId())
                .categoryCode(archive.getCategoryCode())
                .archiveType(archive.getArchiveType())
                .responsibility(archive.getResponsibility())
                .caseNo(archive.getCaseNo())
                .caseName(archive.getCaseName())
                .clientName(archive.getClientName())
                .lawyerName(archive.getLawyerName())
                .keywords(archive.getKeywords())
                .archiveAbstract(archive.getArchiveAbstract())
                .remarks(archive.getRemarks())
                .retentionPeriod(archive.getRetentionPeriod())
                .securityLevel(archive.getSecurityLevel())
                .sourceType(archive.getSourceType())
                .status(archive.getStatus())
                .archiveDate(archive.getArchiveDate())
                .documentDate(archive.getDocumentDate())
                .receivedAt(archive.getReceivedAt())
                .createdAt(archive.getCreatedAt())
                .updatedAt(archive.getUpdatedAt())
                .fileCount(archive.getFileCount())
                .build();

        // 设置归档年份
        if (archive.getArchiveDate() != null) {
            document.setArchiveYear(archive.getArchiveDate().getYear());
        } else if (archive.getReceivedAt() != null) {
            document.setArchiveYear(archive.getReceivedAt().getYear());
        }

        // 获取文件名列表
        if (files == null) {
            files = digitalFileMapper.selectByArchiveId(archive.getId());
        }
        if (files != null && !files.isEmpty()) {
            document.setFileNames(files.stream()
                    .map(DigitalFile::getFileName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        return document;
    }

    /**
     * 添加过滤条件
     */
    private void addFilters(BoolQuery.Builder builder, ArchiveSearchRequest request) {
        addCurrentUserScope(builder);
        if (request.getFondsId() != null) {
            builder.filter(Query.of(q -> q.term(t -> t.field("fondsId").value(request.getFondsId()))));
        }
        if (request.getCategoryId() != null) {
            builder.filter(Query.of(q -> q.term(t -> t.field("categoryId").value(request.getCategoryId()))));
        }
        if (StringUtils.hasText(request.getArchiveType())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("archiveType").value(request.getArchiveType()))));
        }
        if (StringUtils.hasText(request.getRetentionPeriod())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("retentionPeriod").value(request.getRetentionPeriod()))));
        }
        if (StringUtils.hasText(request.getSecurityLevel())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("securityLevel").value(request.getSecurityLevel()))));
        }
        if (StringUtils.hasText(request.getStatus())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("status").value(request.getStatus()))));
        }
        if (StringUtils.hasText(request.getSourceType())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("sourceType").value(request.getSourceType()))));
        }
        if (StringUtils.hasText(request.getLawyerName())) {
            builder.filter(Query.of(q -> q.term(t -> t.field("lawyerName").value(request.getLawyerName()))));
        }
        if (request.getArchiveYear() != null) {
            builder.filter(Query.of(q -> q.term(t -> t.field("archiveYear").value(request.getArchiveYear()))));
        }
        if (request.getArchiveDateStart() != null) {
            builder.filter(Query.of(q -> q.range(r -> r.field("archiveDate")
                    .gte(co.elastic.clients.json.JsonData.of(request.getArchiveDateStart().toString())))));
        }
        if (request.getArchiveDateEnd() != null) {
            builder.filter(Query.of(q -> q.range(r -> r.field("archiveDate")
                    .lte(co.elastic.clients.json.JsonData.of(request.getArchiveDateEnd().toString())))));
        }
    }

    private void addCurrentUserScope(BoolQuery.Builder builder) {
        if (!SecurityUtils.isAuthenticated()
                || SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER")) {
            return;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRealName = SecurityUtils.getCurrentRealName();
        if (StringUtils.hasText(currentRealName)) {
            builder.filter(Query.of(q -> q.term(t -> t.field("lawyerName").value(currentRealName))));
        } else if (currentUserId != null) {
            // ES 文档目前未索引 createdBy/receivedBy，仅在无姓名时回退为空过滤，避免默认放开全库
            builder.filter(Query.of(q -> q.term(t -> t.field("id").value(-1))));
        }

        builder.filter(Query.of(q -> q.bool(b -> b
                .should(s -> s.term(t -> t.field("securityLevel").value("PUBLIC")))
                .should(s -> s.term(t -> t.field("securityLevel").value("INTERNAL")))
                .minimumShouldMatch("1"))));
    }

    /**
     * 构建高亮配置
     */
    private Highlight buildHighlight(ArchiveSearchRequest request) {
        Map<String, HighlightField> fields = new HashMap<>();
        fields.put("title", HighlightField.of(h -> h.numberOfFragments(1).fragmentSize(200)));
        fields.put("caseName", HighlightField.of(h -> h.numberOfFragments(1).fragmentSize(200)));
        fields.put("keywords", HighlightField.of(h -> h.numberOfFragments(1).fragmentSize(200)));
        fields.put("archiveAbstract", HighlightField.of(h -> h.numberOfFragments(2).fragmentSize(150)));
        if (Boolean.TRUE.equals(request.getIncludeFileContent())) {
            fields.put("fileContent", HighlightField.of(h -> h.numberOfFragments(2).fragmentSize(80)));
        }

        return Highlight.of(h -> h
                .preTags("<em class=\"highlight\">")
                .postTags("</em>")
                .fields(fields));
    }

    private Map<Long, List<DigitalFile>> loadFilesByArchiveIds(List<Long> archiveIds) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return digitalFileMapper.selectByArchiveIds(archiveIds).stream()
                .filter(file -> file.getArchiveId() != null)
                .collect(Collectors.groupingBy(DigitalFile::getArchiveId));
    }

    /**
     * 构建聚合
     */
    private Map<String, Aggregation> buildAggregations() {
        Map<String, Aggregation> aggregations = new HashMap<>();
        aggregations.put("byArchiveType", Aggregation.of(a -> a.terms(t -> t.field("archiveType").size(10))));
        aggregations.put("byYear", Aggregation.of(a -> a.terms(t -> t.field("archiveYear").size(20))));
        aggregations.put("byStatus", Aggregation.of(a -> a.terms(t -> t.field("status").size(10))));
        aggregations.put("byRetentionPeriod", Aggregation.of(a -> a.terms(t -> t.field("retentionPeriod").size(10))));
        aggregations.put("byFonds", Aggregation.of(a -> a.terms(t -> t.field("fondsNo").size(20))));
        return aggregations;
    }

    /**
     * 获取字段权重
     */
    private float getFieldBoost(String field) {
        return switch (field) {
            case "title" -> 3.0f;
            case "archiveNo", "caseNo" -> 2.5f;
            case "caseName", "clientName" -> 2.0f;
            case "keywords" -> 1.5f;
            default -> 1.0f;
        };
    }

    /**
     * 转换搜索响应
     */
    private ArchiveSearchResult convertSearchResponse(SearchResponse<ArchiveDocument> response, 
            ArchiveSearchRequest request) {
        List<ArchiveSearchResult.ArchiveSearchHit> hits = response.hits().hits().stream()
                .map(this::convertHit)
                .collect(Collectors.toList());

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        int totalPages = (int) Math.ceil((double) total / request.getPageSize());

        ArchiveSearchResult.ArchiveSearchResultBuilder resultBuilder = ArchiveSearchResult.builder()
                .hits(hits)
                .total(total)
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .totalPages(totalPages)
                .took(response.took());

        // 聚合结果
        if (Boolean.TRUE.equals(request.getAggregation()) && response.aggregations() != null) {
            resultBuilder.aggregations(convertAggregations(response));
        }

        return resultBuilder.build();
    }

    /**
     * 转换单条命中结果
     */
    private ArchiveSearchResult.ArchiveSearchHit convertHit(Hit<ArchiveDocument> hit) {
        ArchiveDocument doc = hit.source();
        if (doc == null) {
            return null;
        }

        ArchiveSearchResult.ArchiveSearchHit.ArchiveSearchHitBuilder builder = 
                ArchiveSearchResult.ArchiveSearchHit.builder()
                .id(doc.getId())
                .archiveNo(doc.getArchiveNo())
                .title(doc.getTitle())
                .fondsNo(doc.getFondsNo())
                .categoryCode(doc.getCategoryCode())
                .archiveType(doc.getArchiveType())
                .caseNo(doc.getCaseNo())
                .caseName(doc.getCaseName())
                .clientName(doc.getClientName())
                .lawyerName(doc.getLawyerName())
                .retentionPeriod(doc.getRetentionPeriod())
                .securityLevel(doc.getSecurityLevel())
                .status(doc.getStatus())
                .fileCount(doc.getFileCount())
                .score(hit.score() != null ? hit.score().floatValue() : 0f);

        if (doc.getReceivedAt() != null) {
            builder.receivedAt(doc.getReceivedAt().format(DATE_TIME_FORMATTER));
        }

        // 高亮内容
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
            builder.highlights(hit.highlight());
        }

        return builder.build();
    }

    /**
     * 转换聚合结果
     */
    private ArchiveSearchResult.Aggregations convertAggregations(SearchResponse<ArchiveDocument> response) {
        ArchiveSearchResult.Aggregations.AggregationsBuilder builder = 
                ArchiveSearchResult.Aggregations.builder();

        if (response.aggregations() != null) {
            response.aggregations().forEach((name, agg) -> {
                if (agg.isSterms()) {
                    List<ArchiveSearchResult.BucketItem> buckets = agg.sterms().buckets().array().stream()
                            .map(b -> ArchiveSearchResult.BucketItem.builder()
                                    .key(b.key().stringValue())
                                    .count(b.docCount())
                                    .build())
                            .collect(Collectors.toList());

                    switch (name) {
                        case "byArchiveType" -> builder.byArchiveType(buckets);
                        case "byYear" -> builder.byYear(buckets);
                        case "byStatus" -> builder.byStatus(buckets);
                        case "byRetentionPeriod" -> builder.byRetentionPeriod(buckets);
                        case "byFonds" -> builder.byFonds(buckets);
                    }
                }
            });
        }

        return builder.build();
    }
}
