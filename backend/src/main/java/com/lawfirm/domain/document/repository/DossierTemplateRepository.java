package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.infrastructure.persistence.mapper.DossierTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 卷宗目录模板仓储
 */
@Repository
public class DossierTemplateRepository extends AbstractRepository<DossierTemplateMapper, DossierTemplate> {

    /**
     * 根据案件类型查询默认模板
     */
    public DossierTemplate findDefaultByCaseType(String caseType) {
        return baseMapper.selectDefaultByCaseType(caseType);
    }

    /**
     * 查询所有模板
     */
    public List<DossierTemplate> findAllTemplates() {
        return baseMapper.selectAllTemplates();
    }
}

