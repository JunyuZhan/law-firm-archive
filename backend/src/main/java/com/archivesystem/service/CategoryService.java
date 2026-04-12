package com.archivesystem.service;

import com.archivesystem.entity.Category;

import java.util.List;

/**
 * 分类服务接口.
 * @author junyuzhan
 */
public interface CategoryService {

    /**
     * 创建分类.
     */
    Category create(Category category);

    /**
     * 更新分类.
     */
    Category update(Long id, Category category);

    /**
     * 获取分类详情.
     */
    Category getById(Long id);

    /**
     * 获取分类树（全部）.
     */
    List<Category> getTree();

    /**
     * 获取指定档案类型的分类树.
     */
    List<Category> getTreeByArchiveType(String archiveType);

    /**
     * 获取子分类.
     */
    List<Category> getChildren(Long parentId);

    /**
     * 删除分类.
     */
    void delete(Long id);

    /**
     * 移动分类.
     */
    void move(Long id, Long newParentId);

    /**
     * 统计分类下的档案数量.
     */
    long countArchives(Long categoryId);
}
