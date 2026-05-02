package com.archivesystem.dto.category;

import com.archivesystem.entity.Category;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 分类响应DTO，避免暴露基础审计字段.
 */
@Value
@Builder
public class CategoryResponse {

    Long id;
    Long parentId;
    String categoryCode;
    String categoryName;
    String archiveType;
    Integer level;
    Integer sortOrder;
    String retentionPeriod;
    String description;
    String status;
    String fullPath;
    List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .archiveType(category.getArchiveType())
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .retentionPeriod(category.getRetentionPeriod())
                .description(category.getDescription())
                .status(category.getStatus())
                .fullPath(category.getFullPath())
                .children(category.getChildren() == null
                        ? null
                        : category.getChildren().stream().map(CategoryResponse::from).toList())
                .build();
    }
}
