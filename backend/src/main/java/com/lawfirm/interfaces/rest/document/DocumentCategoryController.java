package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.dto.DocumentCategoryDTO;
import com.lawfirm.application.document.service.DocumentCategoryAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 文档分类接口 */
@RestController
@RequestMapping("/document/category")
@RequiredArgsConstructor
public class DocumentCategoryController {

  /** 文档分类应用服务 */
  private final DocumentCategoryAppService categoryAppService;

  /**
   * 获取分类树
   *
   * @return 分类树列表
   */
  @GetMapping("/tree")
  public Result<List<DocumentCategoryDTO>> getTree() {
    return Result.success(categoryAppService.getCategoryTree());
  }

  /**
   * 获取子分类
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  @GetMapping("/children")
  public Result<List<DocumentCategoryDTO>> getChildren(
      @RequestParam(defaultValue = "0") final Long parentId) {
    return Result.success(categoryAppService.getChildren(parentId));
  }

  /**
   * 创建分类
   *
   * @param name 分类名称
   * @param parentId 父分类ID
   * @param description 分类描述
   * @return 分类信息
   */
  @PostMapping
  @RequirePermission("doc:category:manage")
  @OperationLog(module = "文档分类", action = "创建分类")
  public Result<DocumentCategoryDTO> create(
      @RequestParam final String name,
      @RequestParam(required = false) final Long parentId,
      @RequestParam(required = false) final String description) {
    return Result.success(categoryAppService.createCategory(name, parentId, description));
  }

  /**
   * 更新分类
   *
   * @param id 分类ID
   * @param name 分类名称
   * @param description 分类描述
   * @param sortOrder 排序顺序
   * @return 分类信息
   */
  @PutMapping("/{id}")
  @RequirePermission("doc:category:manage")
  @OperationLog(module = "文档分类", action = "更新分类")
  public Result<DocumentCategoryDTO> update(
      @PathVariable final Long id,
      @RequestParam(required = false) final String name,
      @RequestParam(required = false) final String description,
      @RequestParam(required = false) final Integer sortOrder) {
    return Result.success(categoryAppService.updateCategory(id, name, description, sortOrder));
  }

  /**
   * 删除分类
   *
   * @param id 分类ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("doc:category:manage")
  @OperationLog(module = "文档分类", action = "删除分类")
  public Result<Void> delete(@PathVariable final Long id) {
    categoryAppService.deleteCategory(id);
    return Result.success();
  }
}
