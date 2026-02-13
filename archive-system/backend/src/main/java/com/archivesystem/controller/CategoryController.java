package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.Category;
import com.archivesystem.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类管理控制器.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "档案分类CRUD接口")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "创建分类")
    public Result<Category> create(@Valid @RequestBody Category category) {
        Category created = categoryService.create(category);
        return Result.success("创建成功", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        Category updated = categoryService.update(id, category);
        return Result.success("更新成功", updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Result<Category> getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    public Result<List<Category>> getTree(
            @RequestParam(required = false) String archiveType) {
        List<Category> tree;
        if (archiveType != null && !archiveType.isEmpty()) {
            tree = categoryService.getTreeByArchiveType(archiveType);
        } else {
            tree = categoryService.getTree();
        }
        return Result.success(tree);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类")
    public Result<List<Category>> getChildren(@PathVariable Long id) {
        List<Category> children = categoryService.getChildren(id);
        return Result.success(children);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "移动分类")
    public Result<Void> move(@PathVariable Long id, @RequestParam(required = false) Long newParentId) {
        categoryService.move(id, newParentId);
        return Result.success("移动成功", null);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取分类统计")
    public Result<Map<String, Object>> statistics(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        long archiveCount = categoryService.countArchives(id);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("category", category);
        stats.put("archiveCount", archiveCount);
        
        return Result.success(stats);
    }
}
