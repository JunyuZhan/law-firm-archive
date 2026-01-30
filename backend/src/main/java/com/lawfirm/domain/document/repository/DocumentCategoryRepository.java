package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.infrastructure.persistence.mapper.DocumentCategoryMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 文档分类仓储。
 *
 * <p>提供文档分类数据的持久化操作。
 */
@Repository
public class DocumentCategoryRepository
    extends AbstractRepository<DocumentCategoryMapper, DocumentCategory> {

  /**
   * 查询子分类。
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  public List<DocumentCategory> findByParentId(final Long parentId) {
    return baseMapper.selectByParentId(parentId);
  }

  /**
   * 查询所有分类。
   *
   * @return 所有分类列表
   */
  public List<DocumentCategory> findAll() {
    return baseMapper.selectAllCategories();
  }
}
