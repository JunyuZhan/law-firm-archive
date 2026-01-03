package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.infrastructure.persistence.mapper.DocumentCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档分类仓储
 */
@Repository
public class DocumentCategoryRepository extends AbstractRepository<DocumentCategoryMapper, DocumentCategory> {

    /**
     * 查询子分类
     */
    public List<DocumentCategory> findByParentId(Long parentId) {
        return baseMapper.selectByParentId(parentId);
    }

    /**
     * 查询所有分类
     */
    public List<DocumentCategory> findAll() {
        return baseMapper.selectAllCategories();
    }
}
