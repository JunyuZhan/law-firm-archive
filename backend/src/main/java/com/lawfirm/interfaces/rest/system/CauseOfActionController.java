package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateCauseCommand;
import com.lawfirm.application.system.command.UpdateCauseCommand;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.system.entity.CauseOfAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 案由/罪名 API */
@Tag(name = "案由管理", description = "案由/罪名数据查询接口")
@RestController
@RequestMapping("/causes")
@RequiredArgsConstructor
public class CauseOfActionController {

  /** 案由/罪名服务. */
  private final CauseOfActionService causeOfActionService;

  /**
   * 获取民事案由树
   *
   * @return 案由树
   */
  @Operation(summary = "获取民事案由树")
  @GetMapping("/civil/tree")
  public Result<List<CauseOfActionService.CauseTreeNode>> getCivilCauseTree() {
    return Result.success(causeOfActionService.getCivilCauseTree());
  }

  /**
   * 获取刑事罪名树
   *
   * @return 罪名树
   */
  @Operation(summary = "获取刑事罪名树")
  @GetMapping("/criminal/tree")
  public Result<List<CauseOfActionService.CauseTreeNode>> getCriminalChargeTree() {
    return Result.success(causeOfActionService.getCriminalChargeTree());
  }

  /**
   * 获取行政案由树
   *
   * @return 案由树
   */
  @Operation(summary = "获取行政案由树")
  @GetMapping("/admin/tree")
  public Result<List<CauseOfActionService.CauseTreeNode>> getAdminCauseTree() {
    return Result.success(causeOfActionService.getAdminCauseTree());
  }

  /**
   * 搜索案由
   *
   * @param type 案由类型
   * @param keyword 关键词
   * @return 案由列表
   */
  @Operation(summary = "搜索案由")
  @GetMapping("/search")
  public Result<List<CauseOfAction>> searchCauses(
      @Parameter(description = "案由类型: CIVIL/CRIMINAL/ADMIN") @RequestParam final String type,
      @Parameter(description = "搜索关键词") @RequestParam final String keyword) {
    return Result.success(causeOfActionService.searchCauses(type, keyword));
  }

  /**
   * 获取案由名称
   *
   * @param code 案由代码
   * @param type 案由类型
   * @return 案由名称
   */
  @Operation(summary = "获取案由名称")
  @GetMapping("/name")
  public Result<String> getCauseName(
      @Parameter(description = "案由代码") @RequestParam final String code,
      @Parameter(description = "案由类型: CIVIL/CRIMINAL/ADMIN") @RequestParam(defaultValue = "CIVIL")
          final String type) {
    return Result.success(causeOfActionService.getCauseName(code, type));
  }

  // ==================== CRUD 操作 ====================

  /**
   * 获取案由详情
   *
   * @param id 案由ID
   * @return 案由详情
   */
  @Operation(summary = "获取案由详情")
  @GetMapping("/{id}")
  public Result<CauseOfAction> getCause(@PathVariable final Long id) {
    return Result.success(causeOfActionService.getCauseById(id));
  }

  /**
   * 创建案由/罪名
   *
   * @param command 创建命令
   * @return 创建的案由
   */
  @Operation(summary = "创建案由/罪名")
  @PostMapping
  @RequirePermission("system:cause:create")
  @OperationLog(module = "案由管理", action = "创建案由")
  public Result<CauseOfAction> createCause(@RequestBody final CreateCauseCommand command) {
    return Result.success(causeOfActionService.createCause(command));
  }

  /**
   * 更新案由/罪名
   *
   * @param id 案由ID
   * @param command 更新命令
   * @return 更新后的案由
   */
  @Operation(summary = "更新案由/罪名")
  @PutMapping("/{id}")
  @RequirePermission("system:cause:update")
  @OperationLog(module = "案由管理", action = "更新案由")
  public Result<CauseOfAction> updateCause(
      @PathVariable final Long id, @RequestBody final UpdateCauseCommand command) {
    return Result.success(causeOfActionService.updateCause(id, command));
  }

  /**
   * 删除案由/罪名
   *
   * @param id 案由ID
   * @return 操作结果
   */
  @Operation(summary = "删除案由/罪名")
  @DeleteMapping("/{id}")
  @RequirePermission("system:cause:delete")
  @OperationLog(module = "案由管理", action = "删除案由")
  public Result<Void> deleteCause(@PathVariable final Long id) {
    causeOfActionService.deleteCause(id);
    return Result.success();
  }

  /**
   * 启用/禁用案由
   *
   * @param id 案由ID
   * @return 空结果
   */
  @Operation(summary = "启用/禁用案由")
  @PostMapping("/{id}/toggle")
  @RequirePermission("system:cause:update")
  @OperationLog(module = "案由管理", action = "切换案由状态")
  public Result<Void> toggleCauseStatus(@PathVariable final Long id) {
    causeOfActionService.toggleCauseStatus(id);
    return Result.success();
  }
}
