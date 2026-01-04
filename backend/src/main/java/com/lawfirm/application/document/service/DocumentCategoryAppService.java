package com.lawfirm.application.document.service;

import com.lawfirm.application.document.dto.DocumentCategoryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档分类应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCategoryAppService {

    private final DocumentCategoryRepository categoryRepository;
    private final DocumentMapper documentMapper;

    /**
     * 获取分类树
     */
    public List<DocumentCategoryDTO> getCategoryTree() {
        List<DocumentCategory> allCategories = categoryRepository.findAll();
        return buildTree(allCategories, 0L);
    }

    /**
     * 获取子分类
     */
    public List<DocumentCategoryDTO> getChildren(Long parentId) {
        List<DocumentCategory> children = categoryRepository.findByParentId(parentId);
        return children.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建分类
     */
    @Transactional
    public DocumentCategoryDTO createCategory(String name, Long parentId, String description) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("分类名称不能为空");
        }

        // 获取同级最大排序号
        List<DocumentCategory> siblings = categoryRepository.findByParentId(parentId != null ? parentId : 0L);
        int maxSort = siblings.stream().mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0).max().orElse(0);

        DocumentCategory category = DocumentCategory.builder()
                .name(name)
                .parentId(parentId != null ? parentId : 0L)
                .sortOrder(maxSort + 1)
                .description(description)
                .build();

        categoryRepository.save(category);
        log.info("文档分类创建成功: {}", name);
        return toDTO(category);
    }

    /**
     * 更新分类
     */
    @Transactional
    public DocumentCategoryDTO updateCategory(Long id, String name, String description, Integer sortOrder) {
        DocumentCategory category = categoryRepository.getByIdOrThrow(id, "分类不存在");

        if (StringUtils.hasText(name)) {
            category.setName(name);
        }
        if (description != null) {
            category.setDescription(description);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }

        categoryRepository.updateById(category);
        log.info("文档分类更新成功: {}", category.getName());
        return toDTO(category);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id) {
        DocumentCategory category = categoryRepository.getByIdOrThrow(id, "分类不存在");

        // 检查是否有子分类
        List<DocumentCategory> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }

        // 检查是否有关联文档
        int documentCount = documentMapper.countByCategoryId(id);
        if (documentCount > 0) {
            throw new BusinessException("该分类下存在文档，无法删除");
        }

        categoryRepository.removeById(id);
        log.info("文档分类删除成功: {}", category.getName());
    }

    /**
     * 构建树形结构
     */
    private List<DocumentCategoryDTO> buildTree(List<DocumentCategory> categories, Long parentId) {
        Map<Long, List<DocumentCategory>> grouped = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));

        return buildTreeRecursive(grouped, parentId);
    }

    private List<DocumentCategoryDTO> buildTreeRecursive(Map<Long, List<DocumentCategory>> grouped, Long parentId) {
        List<DocumentCategory> children = grouped.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }

        return children.stream()
                .map(c -> {
                    DocumentCategoryDTO dto = toDTO(c);
                    dto.setChildren(buildTreeRecursive(grouped, c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Entity 转 DTO
     */
    private DocumentCategoryDTO toDTO(DocumentCategory category) {
        DocumentCategoryDTO dto = new DocumentCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setSortOrder(category.getSortOrder());
        dto.setDescription(category.getDescription());
        return dto;
    }
}
