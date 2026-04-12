package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.Category;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.CategoryMapper;
import com.archivesystem.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public Category create(Category category) {
        // 检查分类代码是否已存在
        Category existing = categoryMapper.selectByCategoryCode(category.getCategoryCode());
        if (existing != null) {
            throw new BusinessException("分类代码已存在: " + category.getCategoryCode());
        }
        
        // 设置层级和全路径
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
            category.setLevel(parent.getLevel() + 1);
            category.setFullPath(parent.getFullPath() + "/" + category.getCategoryName());
        } else {
            category.setParentId(null);
            category.setLevel(1);
            category.setFullPath(category.getCategoryName());
        }
        
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        categoryMapper.insert(category);
        log.info("创建分类: id={}, code={}", category.getId(), category.getCategoryCode());
        return category;
    }

    @Override
    @Transactional
    public Category update(Long id, Category category) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw NotFoundException.of("分类", id);
        }
        
        // 如果修改了分类代码，检查是否重复
        if (!existing.getCategoryCode().equals(category.getCategoryCode())) {
            Category byCode = categoryMapper.selectByCategoryCode(category.getCategoryCode());
            if (byCode != null && !byCode.getId().equals(id)) {
                throw new BusinessException("分类代码已存在: " + category.getCategoryCode());
            }
        }
        
        existing.setCategoryCode(category.getCategoryCode());
        existing.setCategoryName(category.getCategoryName());
        existing.setArchiveType(category.getArchiveType());
        existing.setDescription(category.getDescription());
        existing.setRetentionPeriod(category.getRetentionPeriod());
        existing.setSortOrder(category.getSortOrder());
        
        // 更新全路径
        updateFullPath(existing);
        
        categoryMapper.updateById(existing);
        return existing;
    }

    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw NotFoundException.of("分类", id);
        }
        return category;
    }

    @Override
    public List<Category> getTree() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getDeleted, false)
               .orderByAsc(Category::getLevel)
               .orderByAsc(Category::getSortOrder);
        
        List<Category> allCategories = categoryMapper.selectList(wrapper);
        return buildTree(allCategories, null);
    }

    @Override
    public List<Category> getTreeByArchiveType(String archiveType) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getDeleted, false);
        
        if (StringUtils.hasText(archiveType)) {
            wrapper.eq(Category::getArchiveType, archiveType);
        }
        
        wrapper.orderByAsc(Category::getLevel)
               .orderByAsc(Category::getSortOrder);
        
        List<Category> allCategories = categoryMapper.selectList(wrapper);
        return buildTree(allCategories, null);
    }

    @Override
    public List<Category> getChildren(Long parentId) {
        return categoryMapper.selectByParentId(parentId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return;
        }
        
        // 检查是否有子分类
        List<Category> children = categoryMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        
        // 检查是否有关联档案
        long archiveCount = countArchives(id);
        if (archiveCount > 0) {
            throw new BusinessException("该分类下存在 " + archiveCount + " 个档案，无法删除");
        }
        
        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }

    @Override
    @Transactional
    public void move(Long id, Long newParentId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw NotFoundException.of("分类", id);
        }
        
        // 不能移动到自己或自己的子分类下
        if (newParentId != null && (newParentId.equals(id) || isDescendant(id, newParentId))) {
            throw new BusinessException("不能移动到自己或子分类下");
        }
        
        category.setParentId(newParentId);
        
        if (newParentId != null && newParentId > 0) {
            Category newParent = categoryMapper.selectById(newParentId);
            if (newParent != null) {
                category.setLevel(newParent.getLevel() + 1);
            }
        } else {
            category.setLevel(1);
        }
        
        updateFullPath(category);
        categoryMapper.updateById(category);
        
        // 更新所有子分类的层级和路径
        updateChildrenPath(category);
        
        log.info("移动分类: id={}, newParentId={}", id, newParentId);
    }

    @Override
    public long countArchives(Long categoryId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getCategoryId, categoryId)
               .eq(Archive::getDeleted, false);
        return archiveMapper.selectCount(wrapper);
    }

    /**
     * 构建分类树.
     */
    private List<Category> buildTree(List<Category> categories, Long parentId) {
        List<Category> tree = new ArrayList<>();
        
        for (Category category : categories) {
            Long pid = category.getParentId();
            if ((parentId == null && pid == null) || 
                (parentId != null && parentId.equals(pid))) {
                List<Category> children = buildTree(categories, category.getId());
                category.setChildren(children);
                tree.add(category);
            }
        }
        
        return tree;
    }

    /**
     * 更新分类的全路径.
     */
    private void updateFullPath(Category category) {
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                category.setFullPath(parent.getFullPath() + "/" + category.getCategoryName());
            }
        } else {
            category.setFullPath(category.getCategoryName());
        }
    }

    /**
     * 递归更新子分类的层级和路径.
     */
    private void updateChildrenPath(Category parent) {
        List<Category> children = categoryMapper.selectByParentId(parent.getId());
        for (Category child : children) {
            child.setLevel(parent.getLevel() + 1);
            child.setFullPath(parent.getFullPath() + "/" + child.getCategoryName());
            categoryMapper.updateById(child);
            updateChildrenPath(child);
        }
    }

    /**
     * 检查是否是子孙分类.
     */
    private boolean isDescendant(Long ancestorId, Long checkId) {
        Category check = categoryMapper.selectById(checkId);
        while (check != null && check.getParentId() != null) {
            if (check.getParentId().equals(ancestorId)) {
                return true;
            }
            check = categoryMapper.selectById(check.getParentId());
        }
        return false;
    }
}
