package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateDeadlineCommand;
import com.lawfirm.application.matter.command.UpdateDeadlineCommand;
import com.lawfirm.application.matter.dto.DeadlineDTO;
import com.lawfirm.application.matter.dto.DeadlineQueryDTO;
import com.lawfirm.application.matter.service.DeadlineAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 期限提醒 Controller */
@Tag(name = "期限提醒管理", description = "期限提醒管理相关接口")
@RestController
@RequestMapping("/matter/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

  /** 期限应用服务 */
  private final DeadlineAppService deadlineAppService;

  /**
   * 分页查询期限提醒列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询期限提醒列表")
  @GetMapping("/list")
  @RequirePermission("deadline:list")
  public Result<PageResult<DeadlineDTO>> listDeadlines(final DeadlineQueryDTO query) {
    PageResult<DeadlineDTO> result = deadlineAppService.listDeadlines(query);
    return Result.success(result);
  }

  /**
   * 获取期限提醒详情
   *
   * @param id 期限提醒ID
   * @return 期限提醒详情
   */
  @Operation(summary = "获取期限提醒详情")
  @GetMapping("/{id}")
  @RequirePermission("deadline:view")
  public Result<DeadlineDTO> getDeadlineById(@PathVariable final Long id) {
    DeadlineDTO dto = deadlineAppService.getDeadlineById(id);
    return Result.success(dto);
  }

  /**
   * 根据项目ID查询期限列表
   *
   * @param matterId 项目ID
   * @return 期限列表
   */
  @Operation(summary = "根据项目ID查询期限列表")
  @GetMapping("/matter/{matterId}")
  @RequirePermission("deadline:view")
  public Result<List<DeadlineDTO>> getDeadlinesByMatterId(@PathVariable final Long matterId) {
    List<DeadlineDTO> deadlines = deadlineAppService.getDeadlinesByMatterId(matterId);
    return Result.success(deadlines);
  }

  /**
   * 创建期限提醒
   *
   * @param command 创建期限提醒命令
   * @return 期限提醒信息
   */
  @Operation(summary = "创建期限提醒")
  @PostMapping
  @RequirePermission("deadline:create")
  @OperationLog(module = "期限提醒管理", action = "创建期限提醒")
  public Result<DeadlineDTO> createDeadline(
      @RequestBody @Valid final CreateDeadlineCommand command) {
    DeadlineDTO dto = deadlineAppService.createDeadline(command);
    return Result.success(dto);
  }

  /**
   * 自动创建期限提醒（根据项目信息）
   *
   * @param matterId 项目ID
   * @return 空结果
   */
  @Operation(summary = "自动创建期限提醒")
  @PostMapping("/auto-create/{matterId}")
  @RequirePermission("deadline:create")
  @OperationLog(module = "期限提醒管理", action = "自动创建期限提醒")
  public Result<Void> autoCreateDeadlines(@PathVariable final Long matterId) {
    deadlineAppService.autoCreateDeadlines(matterId);
    return Result.success();
  }

  /**
   * 更新期限提醒
   *
   * @param command 更新期限提醒命令
   * @return 期限提醒信息
   */
  @Operation(summary = "更新期限提醒")
  @PutMapping
  @RequirePermission("deadline:edit")
  @OperationLog(module = "期限提醒管理", action = "更新期限提醒")
  public Result<DeadlineDTO> updateDeadline(
      @RequestBody @Valid final UpdateDeadlineCommand command) {
    DeadlineDTO dto = deadlineAppService.updateDeadline(command);
    return Result.success(dto);
  }

  /**
   * 完成期限
   *
   * @param id 期限提醒ID
   * @return 期限提醒信息
   */
  @Operation(summary = "完成期限")
  @PostMapping("/{id}/complete")
  @RequirePermission("deadline:edit")
  @OperationLog(module = "期限提醒管理", action = "完成期限")
  public Result<DeadlineDTO> completeDeadline(@PathVariable final Long id) {
    DeadlineDTO dto = deadlineAppService.completeDeadline(id);
    return Result.success(dto);
  }

  /**
   * 删除期限提醒
   *
   * @param id 期限提醒ID
   * @return 空结果
   */
  @Operation(summary = "删除期限提醒")
  @DeleteMapping("/{id}")
  @RequirePermission("deadline:delete")
  @OperationLog(module = "期限提醒管理", action = "删除期限提醒")
  public Result<Void> deleteDeadline(@PathVariable final Long id) {
    deadlineAppService.deleteDeadline(id);
    return Result.success();
  }

  /**
   * 获取我的即将到期的期限
   *
   * @param days 天数（默认7天）
   * @param limit 数量限制（默认5条）
   * @return 期限列表
   */
  @Operation(summary = "获取我的即将到期的期限")
  @GetMapping("/my-upcoming")
  @RequirePermission("deadline:view")
  public Result<List<DeadlineDTO>> getMyUpcomingDeadlines(
      @RequestParam(defaultValue = "7") final Integer days,
      @RequestParam(defaultValue = "5") final Integer limit) {
    List<DeadlineDTO> deadlines = deadlineAppService.getMyUpcomingDeadlines(days, limit);
    return Result.success(deadlines);
  }
}
