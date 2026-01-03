package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.infrastructure.persistence.mapper.ReportTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 报表模板 Repository
 */
@Repository
public class ReportTemplateRepository extends AbstractRepository<ReportTemplateMapper, ReportTemplate> {

    /**
     * 根据模板编号查询
     */
    public Optional<ReportTemplate> findByTemplateNo(String templateNo) {
        return Optional.ofNullable(baseMapper.selectByTemplateNo(templateNo));
    }

    /**
     * 检查模板名称是否存在
     */
    public boolean existsByTemplateName(String templateName, Long excludeId) {
        return baseMapper.countByTemplateName(templateName, excludeId != null ? excludeId : 0L) > 0;
    }
}
