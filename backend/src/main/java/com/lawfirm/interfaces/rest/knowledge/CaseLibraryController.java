package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateCaseLibraryCommand;
import com.lawfirm.application.knowledge.dto.CaseCategoryDTO;
import com.lawfirm.application.knowledge.dto.CaseLibraryDTO;
import com.lawfirm.application.knowledge.dto.CaseLibraryQueryDTO;
import com.lawfirm.application.knowledge.service.CaseLibraryAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 案例库接口 */
@Tag(name = "案例库", description = "案例查询、收藏相关接口")
@RestController
@RequestMapping("/knowledge/case")
@RequiredArgsConstructor
public class CaseLibraryController {

  /** 案例库应用服务. */
  private final CaseLibraryAppService caseLibraryAppService;

  /**
   * 获取案例分类树
   *
   * @return 分类树
   */
  @Operation(summary = "获取案例分类树")
  @GetMapping("/categories")
  public Result<List<CaseCategoryDTO>> getCategoryTree() {
    return Result.success(caseLibraryAppService.getCategoryTree());
  }

  /**
   * 分页查询案例
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询案例")
  @GetMapping
  public Result<PageResult<CaseLibraryDTO>> listCases(final CaseLibraryQueryDTO query) {
    return Result.success(caseLibraryAppService.listCases(query));
  }

  /**
   * 获取案例详情
   *
   * @param id 案例ID
   * @return 案例详情
   */
  @Operation(summary = "获取案例详情")
  @GetMapping("/{id}")
  public Result<CaseLibraryDTO> getCaseById(@PathVariable final Long id) {
    return Result.success(caseLibraryAppService.getCaseById(id));
  }

  /**
   * 创建案例
   *
   * @param command 创建命令
   * @return 创建的案例
   */
  @Operation(summary = "创建案例")
  @PostMapping
  @RequirePermission("knowledge:case:create")
  @OperationLog(module = "案例库", action = "创建案例")
  public Result<CaseLibraryDTO> createCase(@RequestBody final CreateCaseLibraryCommand command) {
    return Result.success(caseLibraryAppService.createCase(command));
  }

  /**
   * 更新案例
   *
   * @param id 案例ID
   * @param command 更新命令
   * @return 更新后的案例
   */
  @Operation(summary = "更新案例")
  @PutMapping("/{id}")
  @RequirePermission("knowledge:case:edit")
  @OperationLog(module = "案例库", action = "更新案例")
  public Result<CaseLibraryDTO> updateCase(
      @PathVariable final Long id, @RequestBody final CreateCaseLibraryCommand command) {
    return Result.success(caseLibraryAppService.updateCase(id, command));
  }

  /**
   * 删除案例
   *
   * @param id 案例ID
   * @return 操作结果
   */
  @Operation(summary = "删除案例")
  @DeleteMapping("/{id}")
  @RequirePermission("knowledge:case:delete")
  @OperationLog(module = "案例库", action = "删除案例")
  public Result<Void> deleteCase(@PathVariable final Long id) {
    caseLibraryAppService.deleteCase(id);
    return Result.success();
  }

  /**
   * 收藏案例
   *
   * @param id 案例ID
   * @return 操作结果
   */
  @Operation(summary = "收藏案例")
  @PostMapping("/{id}/collect")
  public Result<Void> collectCase(@PathVariable final Long id) {
    caseLibraryAppService.collectCase(id);
    return Result.success();
  }

  /**
   * 取消收藏案例
   *
   * @param id 案例ID
   * @return 操作结果
   */
  @Operation(summary = "取消收藏案例")
  @DeleteMapping("/{id}/collect")
  public Result<Void> uncollectCase(@PathVariable final Long id) {
    caseLibraryAppService.uncollectCase(id);
    return Result.success();
  }

  /**
   * 获取我的收藏案例
   *
   * @return 收藏列表
   */
  @Operation(summary = "获取我的收藏案例")
  @GetMapping("/collected")
  public Result<List<CaseLibraryDTO>> getMyCollectedCases() {
    return Result.success(caseLibraryAppService.getMyCollectedCases());
  }
}
