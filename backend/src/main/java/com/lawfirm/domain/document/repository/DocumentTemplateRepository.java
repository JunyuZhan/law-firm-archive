package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.infrastructure.persistence.mapper.DocumentTemplateMapper;
import org.springframework.stereotype.Repository;

/**
 * 文档模板仓储
 */
@Repository
public class DocumentTemplateRepository extends AbstractRepository<DocumentTemplateMapper, DocumentTemplate> {

    /**
     * 增加使用次数
     */
    public void incrementUseCount(Long id) {
        baseMapper.incrementUseCount(id);
    }
}
