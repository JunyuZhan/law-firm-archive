package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.ApproveRegularizationCommand;
import com.lawfirm.application.hr.command.CreateRegularizationCommand;
import com.lawfirm.application.hr.dto.RegularizationDTO;
import com.lawfirm.application.hr.dto.RegularizationQueryDTO;
import com.lawfirm.application.hr.service.RegularizationAppService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 转正申请管理接口 */
@Tag(name = "转正申请管理", description = "转正申请管理相关接口")
@RestController
@RequestMapping("/hr/regularization")
@RequiredArgsConstructor
public class RegularizationController {

  /** 转正服务. */
  private final RegularizationAppService regularizationAppService;

  /**
   * 分页查询转正申请
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询转正申请")
  @GetMapping
  @RequirePermission("hr:regularization:list")
  public Result<PageResult<RegularizationDTO>> listRegularizations(
      final RegularizationQueryDTO query) {
    return Result.success(regularizationAppService.listRegularizations(query));
  }

  /**
   * 根据ID查询转正申请
   *
   * @param id 申请ID
   * @return 申请详情
   */
  @Operation(summary = "根据ID查询转正申请")
  @GetMapping("/{id}")
  @RequirePermission("hr:regularization:detail")
  public Result<RegularizationDTO> getRegularization(@PathVariable final Long id) {
    return Result.success(regularizationAppService.getRegularizationById(id));
  }

  /**
   * 根据员工ID查询转正申请
   *
   * @param employeeId 员工ID
   * @return 申请列表
   */
  @Operation(summary = "根据员工ID查询转正申请")
  @GetMapping("/employee/{employeeId}")
  @RequirePermission("hr:regularization:list")
  public Result<List<RegularizationDTO>> getRegularizationsByEmployeeId(
      @PathVariable final Long employeeId) {
    return Result.success(regularizationAppService.getRegularizationsByEmployeeId(employeeId));
  }

  /**
   * 创建转正申请
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建转正申请")
  @PostMapping
  @RequirePermission("hr:regularization:create")
  @OperationLog(module = "转正申请管理", action = "创建转正申请")
  public Result<RegularizationDTO> createRegularization(
      @Valid @RequestBody final CreateRegularizationCommand command) {
    return Result.success(regularizationAppService.createRegularization(command));
  }

  /**
   * 审批转正申请
   *
   * @param id 申请ID
   * @param command 审批命令
   * @return 审批结果
   */
  @Operation(summary = "审批转正申请")
  @PostMapping("/{id}/approve")
  @RequirePermission("hr:regularization:approve")
  @OperationLog(module = "转正申请管理", action = "审批转正申请")
  public Result<RegularizationDTO> approveRegularization(
      @PathVariable final Long id, @Valid @RequestBody final ApproveRegularizationCommand command) {
    return Result.success(regularizationAppService.approveRegularization(id, command));
  }

  /**
   * 删除转正申请
   *
   * @param id 申请ID
   * @return 无返回
   */
  @Operation(summary = "删除转正申请")
  @DeleteMapping("/{id}")
  @RequirePermission("hr:regularization:delete")
  @OperationLog(module = "转正申请管理", action = "删除转正申请")
  public Result<Void> deleteRegularization(@PathVariable final Long id) {
    regularizationAppService.deleteRegularization(id);
    return Result.success();
  }
}
