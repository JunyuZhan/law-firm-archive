package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.infrastructure.persistence.mapper.DossierTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 卷宗目录模板仓储。
 *
 * <p>提供卷宗目录模板数据的持久化操作。
 */
@Repository
public class DossierTemplateRepository
    extends AbstractRepository<DossierTemplateMapper, DossierTemplate> {

  /**
   * 根据案件类型查询默认模板。
   *
   * @param caseType 案件类型
   * @return 默认模板
   */
  public DossierTemplate findDefaultByCaseType(final String caseType) {
    return baseMapper.selectDefaultByCaseType(caseType);
  }

  /**
   * 查询所有模板。
   *
   * @return 所有模板列表
   */
  public List<DossierTemplate> findAllTemplates() {
    return baseMapper.selectAllTemplates();
  }
}
