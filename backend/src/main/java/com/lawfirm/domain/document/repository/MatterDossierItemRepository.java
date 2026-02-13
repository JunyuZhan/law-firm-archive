package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import com.lawfirm.infrastructure.persistence.mapper.MatterDossierItemMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 项目卷宗目录仓储。
 *
 * <p>提供项目卷宗目录数据的持久化操作。
 */
@Repository
public class MatterDossierItemRepository
    extends AbstractRepository<MatterDossierItemMapper, MatterDossierItem> {

  /**
   * 根据项目ID查询目录项。
   *
   * @param matterId 案件ID
   * @return 目录项列表
   */
  public List<MatterDossierItem> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 根据父ID查询子目录项。
   *
   * @param matterId 案件ID
   * @param parentId 父目录项ID
   * @return 子目录项列表
   */
  public List<MatterDossierItem> findByParentId(final Long matterId, final Long parentId) {
    return baseMapper.selectByParentId(matterId, parentId);
  }

  /**
   * 更新目录项文件数量。
   *
   * @param itemId 目录项ID
   * @param count 文件数量
   */
  public void updateDocumentCount(final Long itemId, final Integer count) {
    baseMapper.updateDocumentCount(itemId, count);
  }

  /**
   * 检查项目是否已初始化卷宗目录。
   *
   * @param matterId 案件ID
   * @return 是否已初始化
   */
  public boolean hasDossierItems(final Long matterId) {
    return baseMapper.countByMatterId(matterId) > 0;
  }
}
