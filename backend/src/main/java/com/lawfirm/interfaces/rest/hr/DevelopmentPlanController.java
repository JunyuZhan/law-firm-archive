package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateDevelopmentPlanCommand;
import com.lawfirm.application.hr.dto.DevelopmentPlanDTO;
import com.lawfirm.application.hr.service.DevelopmentPlanAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 发展规划控制器 */
@Tag(name = "发展规划", description = "个人发展规划管理接口")
@RestController
@RequestMapping("/hr/development-plan")
@RequiredArgsConstructor
public class DevelopmentPlanController {

  /** 发展规划服务. */
  private final DevelopmentPlanAppService developmentPlanAppService;

  /**
   * 分页查询发展规划
   *
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param keyword 关键词
   * @param status 状态
   * @param employeeId 员工ID
   * @param planYear 规划年度
   * @return 分页结果
   */
  @Operation(summary = "分页查询发展规划")
  @GetMapping
  @RequirePermission("hr:development:list")
  public Result<PageResult<DevelopmentPlanDTO>> list(
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "状态") @RequestParam(required = false) final String status,
      @Parameter(description = "员工ID") @RequestParam(required = false) final Long employeeId,
      @Parameter(description = "规划年度") @RequestParam(required = false) final Integer planYear) {
    PageResult<DevelopmentPlanDTO> result =
        developmentPlanAppService.listPlans(
            pageNum, pageSize, keyword, status, employeeId, planYear);
    return Result.success(result);
  }

  /**
   * 获取规划详情
   *
   * @param id 规划ID
   * @return 规划详情
   */
  @Operation(summary = "获取规划详情")
  @GetMapping("/{id}")
  @RequirePermission("hr:development:view")
  public Result<DevelopmentPlanDTO> getById(@PathVariable final Long id) {
    DevelopmentPlanDTO plan = developmentPlanAppService.getPlanById(id);
    return Result.success(plan);
  }

  /**
   * 获取我的当年规划
   *
   * @return 我的当年规划
   */
  @Operation(summary = "获取我的当年规划")
  @GetMapping("/my-current")
  public Result<DevelopmentPlanDTO> getMyCurrentPlan() {
    DevelopmentPlanDTO plan = developmentPlanAppService.getMyCurrentPlan();
    return Result.success(plan);
  }

  /**
   * 创建发展规划
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建发展规划")
  @PostMapping
  @RequirePermission("hr:development:create")
  @OperationLog(module = "发展规划", action = "创建规划")
  public Result<DevelopmentPlanDTO> create(
      @RequestBody @Valid final CreateDevelopmentPlanCommand command) {
    DevelopmentPlanDTO plan = developmentPlanAppService.createPlan(command);
    return Result.success(plan);
  }

  /**
   * 更新发展规划
   *
   * @param id 规划ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新发展规划")
  @PutMapping("/{id}")
  @RequirePermission("hr:development:update")
  @OperationLog(module = "发展规划", action = "更新规划")
  public Result<DevelopmentPlanDTO> update(
      @PathVariable final Long id, @RequestBody @Valid final CreateDevelopmentPlanCommand command) {
    DevelopmentPlanDTO plan = developmentPlanAppService.updatePlan(id, command);
    return Result.success(plan);
  }

  /**
   * 删除发展规划
   *
   * @param id 规划ID
   * @return 无返回
   */
  @Operation(summary = "删除发展规划")
  @DeleteMapping("/{id}")
  @RequirePermission("hr:development:delete")
  @OperationLog(module = "发展规划", action = "删除规划")
  public Result<Void> delete(@PathVariable final Long id) {
    developmentPlanAppService.deletePlan(id);
    return Result.success();
  }

  /**
   * 提交规划
   *
   * @param id 规划ID
   * @return 无返回
   */
  @Operation(summary = "提交规划")
  @PostMapping("/{id}/submit")
  @RequirePermission("hr:development:update")
  @OperationLog(module = "发展规划", action = "提交规划")
  public Result<Void> submit(@PathVariable final Long id) {
    developmentPlanAppService.submitPlan(id);
    return Result.success();
  }

  /**
   * 审核规划
   *
   * @param id 规划ID
   * @param comment 审核意见
   * @return 无返回
   */
  @Operation(summary = "审核规划")
  @PostMapping("/{id}/review")
  @RequirePermission("hr:development:review")
  @OperationLog(module = "发展规划", action = "审核规划")
  public Result<Void> review(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    developmentPlanAppService.reviewPlan(id, comment);
    return Result.success();
  }

  /**
   * 更新里程碑状态
   *
   * @param milestoneId 里程碑ID
   * @param status 状态
   * @param completionNote 完成说明
   * @return 无返回
   */
  @Operation(summary = "更新里程碑状态")
  @PostMapping("/milestones/{milestoneId}/status")
  @RequirePermission("hr:development:update")
  @OperationLog(module = "发展规划", action = "更新里程碑")
  public Result<Void> updateMilestoneStatus(
      @PathVariable final Long milestoneId,
      @RequestParam final String status,
      @RequestParam(required = false) final String completionNote) {
    developmentPlanAppService.updateMilestoneStatus(milestoneId, status, completionNote);
    return Result.success();
  }
}
