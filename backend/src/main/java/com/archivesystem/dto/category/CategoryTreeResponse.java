package com.archivesystem.dto.category;

import com.archivesystem.entity.Category;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 分类树摘要响应 DTO.
 */
@Value
@Builder
public class CategoryTreeResponse {

    Long id;
    Long parentId;
    String categoryCode;
    String categoryName;
    List<CategoryTreeResponse> children;

    public static CategoryTreeResponse from(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryTreeResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .children(category.getChildren() == null
                        ? null
                        : category.getChildren().stream().map(CategoryTreeResponse::from).toList())
                .build();
    }
}
