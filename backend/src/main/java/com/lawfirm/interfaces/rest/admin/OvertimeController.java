package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.ApplyOvertimeCommand;
import com.lawfirm.application.admin.dto.OvertimeApplicationDTO;
import com.lawfirm.application.admin.service.OvertimeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 加班申请接口（M8-004） */
@Tag(name = "加班管理", description = "加班申请和审批")
@RestController
@RequestMapping("/admin/overtime")
@RequiredArgsConstructor
public class OvertimeController {

  /** 加班应用服务 */
  private final OvertimeAppService overtimeAppService;

  /**
   * 申请加班
   *
   * @param command 申请命令
   * @return 申请记录
   */
  @Operation(summary = "申请加班")
  @PostMapping("/apply")
  @RequirePermission("admin:overtime:apply")
  @OperationLog(module = "加班管理", action = "申请加班")
  public Result<OvertimeApplicationDTO> applyOvertime(
      @RequestBody @Valid final ApplyOvertimeCommand command) {
    return Result.success(overtimeAppService.applyOvertime(command));
  }

  /**
   * 审批加班申请
   *
   * @param id 申请ID
   * @param request 审批请求
   * @return 申请记录
   */
  @Operation(summary = "审批加班申请")
  @PostMapping("/{id}/approve")
  @RequirePermission("admin:overtime:approve")
  @OperationLog(module = "加班管理", action = "审批加班申请")
  public Result<OvertimeApplicationDTO> approveOvertime(
      @PathVariable final Long id, @RequestBody final ApproveRequest request) {
    return Result.success(
        overtimeAppService.approveOvertime(id, request.getApproved(), request.getComment()));
  }

  /**
   * 查询我的加班申请
   *
   * @return 申请列表
   */
  @Operation(summary = "查询我的加班申请")
  @GetMapping("/my")
  @RequirePermission("admin:overtime:list")
  public Result<List<OvertimeApplicationDTO>> getMyApplications() {
    return Result.success(overtimeAppService.getMyApplications());
  }

  /**
   * 查询指定日期范围的加班申请
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 申请列表
   */
  @Operation(summary = "查询指定日期范围的加班申请")
  @GetMapping("/range")
  @RequirePermission("admin:overtime:list")
  public Result<List<OvertimeApplicationDTO>> getApplicationsByDateRange(
      @RequestParam final LocalDate startDate, @RequestParam final LocalDate endDate) {
    return Result.success(overtimeAppService.getApplicationsByDateRange(startDate, endDate));
  }

  /** 审批请求 */
  @Data
  public static class ApproveRequest {
    /** 是否批准 */
    private Boolean approved;

    /** 审批意见 */
    private String comment;
  }
}
