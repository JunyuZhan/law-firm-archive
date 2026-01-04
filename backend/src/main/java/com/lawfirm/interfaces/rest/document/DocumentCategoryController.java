package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.dto.DocumentCategoryDTO;
import com.lawfirm.application.document.service.DocumentCategoryAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档分类接口
 */
@RestController
@RequestMapping("/document/category")
@RequiredArgsConstructor
public class DocumentCategoryController {

    private final DocumentCategoryAppService categoryAppService;

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    public Result<List<DocumentCategoryDTO>> getTree() {
        return Result.success(categoryAppService.getCategoryTree());
    }

    /**
     * 获取子分类
     */
    @GetMapping("/children")
    public Result<List<DocumentCategoryDTO>> getChildren(@RequestParam(defaultValue = "0") Long parentId) {
        return Result.success(categoryAppService.getChildren(parentId));
    }

    /**
     * 创建分类
     */
    @PostMapping
    @RequirePermission("doc:category:manage")
    @OperationLog(module = "文档分类", action = "创建分类")
    public Result<DocumentCategoryDTO> create(@RequestParam String name,
                                              @RequestParam(required = false) Long parentId,
                                              @RequestParam(required = false) String description) {
        return Result.success(categoryAppService.createCategory(name, parentId, description));
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    @RequirePermission("doc:category:manage")
    @OperationLog(module = "文档分类", action = "更新分类")
    public Result<DocumentCategoryDTO> update(@PathVariable Long id,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String description,
                                              @RequestParam(required = false) Integer sortOrder) {
        return Result.success(categoryAppService.updateCategory(id, name, description, sortOrder));
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    @RequirePermission("doc:category:manage")
    @OperationLog(module = "文档分类", action = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryAppService.deleteCategory(id);
        return Result.success();
    }
}
