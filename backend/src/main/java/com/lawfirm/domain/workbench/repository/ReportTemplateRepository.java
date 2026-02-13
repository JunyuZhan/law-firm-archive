package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.infrastructure.persistence.mapper.ReportTemplateMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 报表模板 Repository */
@Repository
public class ReportTemplateRepository
    extends AbstractRepository<ReportTemplateMapper, ReportTemplate> {

  /**
   * 根据模板编号查询。
   *
   * @param templateNo 模板编号
   * @return 报表模板，如果不存在则返回空
   */
  public Optional<ReportTemplate> findByTemplateNo(final String templateNo) {
    return Optional.ofNullable(baseMapper.selectByTemplateNo(templateNo));
  }

  /**
   * 检查模板名称是否存在。
   *
   * @param templateName 模板名称
   * @param excludeId 排除的ID（用于更新时检查）
   * @return 如果存在返回true，否则返回false
   */
  public boolean existsByTemplateName(final String templateName, final Long excludeId) {
    return baseMapper.countByTemplateName(templateName, excludeId != null ? excludeId : 0L) > 0;
  }
}
