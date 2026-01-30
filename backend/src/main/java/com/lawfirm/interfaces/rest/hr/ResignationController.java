package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.command.CreateResignationCommand;
import com.lawfirm.application.hr.dto.ResignationDTO;
import com.lawfirm.application.hr.dto.ResignationQueryDTO;
import com.lawfirm.application.hr.service.ResignationAppService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 离职申请管理接口 */
@Tag(name = "离职申请管理", description = "离职申请管理相关接口")
@RestController
@RequestMapping("/hr/resignation")
@RequiredArgsConstructor
public class ResignationController {

  /** 离职服务. */
  private final ResignationAppService resignationAppService;

  /**
   * 分页查询离职申请
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询离职申请")
  @GetMapping
  @RequirePermission("hr:resignation:list")
  public Result<PageResult<ResignationDTO>> listResignations(final ResignationQueryDTO query) {
    return Result.success(resignationAppService.listResignations(query));
  }

  /**
   * 根据ID查询离职申请
   *
   * @param id 申请ID
   * @return 申请详情
   */
  @Operation(summary = "根据ID查询离职申请")
  @GetMapping("/{id}")
  @RequirePermission("hr:resignation:detail")
  public Result<ResignationDTO> getResignation(@PathVariable final Long id) {
    return Result.success(resignationAppService.getResignationById(id));
  }

  /**
   * 根据员工ID查询离职申请
   *
   * @param employeeId 员工ID
   * @return 申请列表
   */
  @Operation(summary = "根据员工ID查询离职申请")
  @GetMapping("/employee/{employeeId}")
  @RequirePermission("hr:resignation:list")
  public Result<List<ResignationDTO>> getResignationsByEmployeeId(
      @PathVariable final Long employeeId) {
    return Result.success(resignationAppService.getResignationsByEmployeeId(employeeId));
  }

  /**
   * 创建离职申请
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建离职申请")
  @PostMapping
  @RequirePermission("hr:resignation:create")
  @OperationLog(module = "离职申请管理", action = "创建离职申请")
  public Result<ResignationDTO> createResignation(
      @Valid @RequestBody final CreateResignationCommand command) {
    return Result.success(resignationAppService.createResignation(command));
  }

  /**
   * 审批离职申请
   *
   * @param id 申请ID
   * @param command 审批命令
   * @return 审批结果
   */
  @Operation(summary = "审批离职申请")
  @PostMapping("/{id}/approve")
  @RequirePermission("hr:resignation:approve")
  @OperationLog(module = "离职申请管理", action = "审批离职申请")
  public Result<ResignationDTO> approveResignation(
      @PathVariable final Long id, @Valid @RequestBody final ApproveResignationCommand command) {
    return Result.success(resignationAppService.approveResignation(id, command));
  }

  /**
   * 完成交接
   *
   * @param id 申请ID
   * @param handoverNote 交接备注
   * @return 申请详情
   */
  @Operation(summary = "完成交接")
  @PostMapping("/{id}/complete-handover")
  @RequirePermission("hr:resignation:handover")
  @OperationLog(module = "离职申请管理", action = "完成离职交接")
  public Result<ResignationDTO> completeHandover(
      @PathVariable final Long id, @RequestParam(required = false) final String handoverNote) {
    return Result.success(resignationAppService.completeHandover(id, handoverNote));
  }

  /**
   * 删除离职申请
   *
   * @param id 申请ID
   * @return 无返回
   */
  @Operation(summary = "删除离职申请")
  @DeleteMapping("/{id}")
  @RequirePermission("hr:resignation:delete")
  @OperationLog(module = "离职申请管理", action = "删除离职申请")
  public Result<Void> deleteResignation(@PathVariable final Long id) {
    resignationAppService.deleteResignation(id);
    return Result.success();
  }
}
