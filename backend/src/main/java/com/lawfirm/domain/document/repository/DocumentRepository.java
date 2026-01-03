package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档仓储
 */
@Repository
public class DocumentRepository extends AbstractRepository<DocumentMapper, Document> {

    /**
     * 查询文档所有版本
     */
    public List<Document> findAllVersions(Long docId) {
        return baseMapper.selectAllVersions(docId);
    }
}
