package com.lawfirm.domain.document.repository;

import com.lawfirm.domain.document.entity.DossierTemplateItem;
import com.lawfirm.infrastructure.persistence.mapper.DossierTemplateItemMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 卷宗目录项仓储。
 *
 * <p>提供卷宗目录项数据的持久化操作。
 */
@Repository
@RequiredArgsConstructor
public class DossierTemplateItemRepository {

  /** 卷宗目录项数据访问对象. */
  private final DossierTemplateItemMapper mapper;

  /**
   * 根据模板ID查询目录项。
   *
   * @param templateId 模板ID
   * @return 目录项列表
   */
  public List<DossierTemplateItem> findByTemplateId(final Long templateId) {
    return mapper.selectByTemplateId(templateId);
  }

  /**
   * 根据父ID查询子目录项。
   *
   * @param templateId 模板ID
   * @param parentId 父目录项ID
   * @return 子目录项列表
   */
  public List<DossierTemplateItem> findByParentId(final Long templateId, final Long parentId) {
    return mapper.selectByParentId(templateId, parentId);
  }

  /**
   * 根据ID获取目录项。
   *
   * @param id 目录项ID
   * @return 目录项
   */
  public DossierTemplateItem getById(final Long id) {
    return mapper.selectById(id);
  }

  /**
   * 保存目录项。
   *
   * @param item 目录项
   */
  public void save(final DossierTemplateItem item) {
    mapper.insert(item);
  }

  /**
   * 更新目录项。
   *
   * @param item 目录项
   */
  public void updateById(final DossierTemplateItem item) {
    mapper.updateById(item);
  }

  /**
   * 删除目录项。
   *
   * @param id 目录项ID
   */
  public void removeById(final Long id) {
    mapper.deleteById(id);
  }
}
