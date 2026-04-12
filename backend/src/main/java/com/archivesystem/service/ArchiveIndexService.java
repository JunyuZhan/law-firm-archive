package com.archivesystem.service;

import com.archivesystem.document.ArchiveDocument;
import com.archivesystem.dto.archive.ArchiveSearchRequest;
import com.archivesystem.dto.archive.ArchiveSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 档案索引服务接口
 * 负责ES索引的同步和搜索
 * @author junyuzhan
 */
public interface ArchiveIndexService {

    /**
     * 索引单个档案
     * @param archiveId 档案ID
     */
    void indexArchive(Long archiveId);

    /**
     * 批量索引档案
     * @param archiveIds 档案ID列表
     */
    void indexArchives(List<Long> archiveIds);

    /**
     * 删除档案索引
     * @param archiveId 档案ID
     */
    void deleteIndex(Long archiveId);

    /**
     * 全量重建索引
     */
    void rebuildAllIndexes();

    /**
     * 全文搜索
     * @param request 搜索请求
     * @return 搜索结果
     */
    ArchiveSearchResult search(ArchiveSearchRequest request);

    /**
     * 获取聚合统计
     * @return 统计结果（按类型、年份等）
     */
    Map<String, Object> getAggregations();

    /**
     * 更新文件内容索引（OCR内容）
     * @param archiveId 档案ID
     * @param content 文件内容
     */
    void updateFileContent(Long archiveId, String content);
}
