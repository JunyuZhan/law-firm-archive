package com.archivesystem.elasticsearch;

import com.archivesystem.document.ArchiveDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 档案ES仓库
 * @author junyuzhan
 */
@Repository
public interface ArchiveSearchRepository extends ElasticsearchRepository<ArchiveDocument, Long> {

    /**
     * 按档案号查找
     */
    ArchiveDocument findByArchiveNo(String archiveNo);

    /**
     * 按全宗ID查找
     */
    List<ArchiveDocument> findByFondsId(Long fondsId);

    /**
     * 按分类ID查找
     */
    List<ArchiveDocument> findByCategoryId(Long categoryId);

    /**
     * 按档案类型查找
     */
    List<ArchiveDocument> findByArchiveType(String archiveType);

    /**
     * 按状态查找
     */
    List<ArchiveDocument> findByStatus(String status);

    /**
     * 删除指定档案
     */
    void deleteByArchiveNo(String archiveNo);
}
