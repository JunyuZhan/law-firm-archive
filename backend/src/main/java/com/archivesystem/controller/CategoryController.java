package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.category.CategoryTreeResponse;
import com.archivesystem.dto.category.CategoryResponse;
import com.archivesystem.dto.category.CategoryStatisticsResponse;
import com.archivesystem.entity.Category;
import com.archivesystem.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器.
 * @author junyuzhan
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "档案分类CRUD接口")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "创建分类")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<CategoryTreeResponse> create(@Valid @RequestBody Category category) {
        Category created = categoryService.create(category);
        return Result.success("创建成功", CategoryTreeResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<CategoryTreeResponse> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        Category updated = categoryService.update(id, category);
        return Result.success("更新成功", CategoryTreeResponse.from(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    @PreAuthorize("isAuthenticated()")
    public Result<CategoryResponse> getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.success(CategoryResponse.from(category));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CategoryResponse>> getTree(
            @RequestParam(required = false) String archiveType) {
        List<Category> tree;
        if (archiveType != null && !archiveType.isEmpty()) {
            tree = categoryService.getTreeByArchiveType(archiveType);
        } else {
            tree = categoryService.getTree();
        }
        return Result.success(tree.stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/tree/summary")
    @Operation(summary = "获取分类树摘要")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CategoryTreeResponse>> getTreeSummary(
            @RequestParam(required = false) String archiveType) {
        List<Category> tree;
        if (archiveType != null && !archiveType.isEmpty()) {
            tree = categoryService.getTreeByArchiveType(archiveType);
        } else {
            tree = categoryService.getTree();
        }
        return Result.success(tree.stream().map(CategoryTreeResponse::from).toList());
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CategoryResponse>> getChildren(@PathVariable Long id) {
        List<Category> children = categoryService.getChildren(id);
        return Result.success(children.stream().map(CategoryResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "移动分类")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> move(@PathVariable Long id, @RequestParam(required = false) Long newParentId) {
        categoryService.move(id, newParentId);
        return Result.success("移动成功", null);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取分类统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<CategoryStatisticsResponse> statistics(@PathVariable Long id) {
        categoryService.getById(id);
        long archiveCount = categoryService.countArchives(id);

        return Result.success(CategoryStatisticsResponse.builder()
                .archiveCount(archiveCount)
                .build());
    }
}
