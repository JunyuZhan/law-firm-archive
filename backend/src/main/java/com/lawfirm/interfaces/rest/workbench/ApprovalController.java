package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.ApproveCommand;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.dto.ApprovalQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 审批中心 Controller */
@Tag(name = "审批中心", description = "审批管理相关接口")
@RestController
@RequestMapping("/workbench/approval")
@RequiredArgsConstructor
public class ApprovalController {

  /** 审批应用服务. */
  private final ApprovalAppService approvalAppService;

  /**
   * 分页查询审批记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询审批记录")
  @GetMapping("/list")
  @RequirePermission("approval:list")
  public Result<PageResult<ApprovalDTO>> listApprovals(final ApprovalQueryDTO query) {
    PageResult<ApprovalDTO> result = approvalAppService.listApprovals(query);
    return Result.success(result);
  }

  /**
   * 获取待审批列表
   *
   * @return 待审批列表
   */
  @Operation(summary = "获取待审批列表")
  @GetMapping("/pending")
  @RequirePermission("approval:list")
  public Result<List<ApprovalDTO>> getPendingApprovals() {
    List<ApprovalDTO> approvals = approvalAppService.getPendingApprovals();
    return Result.success(approvals);
  }

  /**
   * 获取我发起的审批
   *
   * @return 审批记录列表
   */
  @Operation(summary = "获取我发起的审批")
  @GetMapping("/my-initiated")
  @RequirePermission("approval:list")
  public Result<List<ApprovalDTO>> getMyInitiatedApprovals() {
    List<ApprovalDTO> approvals = approvalAppService.getMyInitiatedApprovals();
    return Result.success(approvals);
  }

  /**
   * 获取我审批过的记录（审批历史）
   *
   * @return 审批记录列表
   */
  @Operation(summary = "审批历史", description = "获取当前用户已处理的审批记录")
  @GetMapping("/my-history")
  @RequirePermission("approval:list")
  public Result<List<ApprovalDTO>> getMyApprovedHistory() {
    List<ApprovalDTO> approvals = approvalAppService.getMyApprovedHistory();
    return Result.success(approvals);
  }

  /**
   * 获取审批详情
   *
   * @param id 审批ID
   * @return 审批详情
   */
  @Operation(summary = "获取审批详情")
  @GetMapping("/{id}")
  @RequirePermission("approval:list")
  public Result<ApprovalDTO> getApproval(@PathVariable final Long id) {
    ApprovalDTO approval = approvalAppService.getApprovalById(id);
    return Result.success(approval);
  }

  /**
   * 审批操作（通过/拒绝）
   *
   * @param command 审批命令
   * @return 操作结果
   */
  @Operation(summary = "审批操作（通过/拒绝）")
  @PostMapping("/approve")
  @RequirePermission("approval:approve")
  @OperationLog(module = "审批中心", action = "审批操作")
  public Result<Void> approve(@RequestBody @Valid final ApproveCommand command) {
    approvalAppService.approve(command);
    return Result.success();
  }

  /**
   * 批量审批
   *
   * @param request 批量审批请求
   * @return 批量审批结果
   */
  @Operation(summary = "批量审批", description = "一次性审批多个待审批事项")
  @PostMapping("/batch-approve")
  @RequirePermission("approval:approve")
  @OperationLog(module = "审批中心", action = "批量审批")
  public Result<ApprovalAppService.BatchApproveResult> batchApprove(
      @RequestBody @Valid final BatchApproveRequest request) {
    String status =
        Boolean.TRUE.equals(request.getResult())
            ? ApprovalStatus.APPROVED
            : ApprovalStatus.REJECTED;
    ApprovalAppService.BatchApproveResult result =
        approvalAppService.batchApprove(request.getApprovalIds(), status, request.getComment());
    return Result.success(result);
  }

  /**
   * 获取业务审批记录
   *
   * @param businessId 业务ID
   * @param businessType 业务类型
   * @return 审批记录列表
   */
  @Operation(summary = "获取业务审批记录")
  @GetMapping("/business")
  @RequirePermission("approval:list")
  public Result<List<ApprovalDTO>> getBusinessApprovals(
      @RequestParam final Long businessId, @RequestParam final String businessType) {
    List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals(businessType, businessId);
    return Result.success(approvals);
  }

  /** 批量审批请求. */
  @Data
  public static class BatchApproveRequest {
    /** 审批ID列表. */
    @jakarta.validation.constraints.NotEmpty(message = "审批ID列表不能为空")
    @jakarta.validation.constraints.Size(max = 100, message = "批量审批数量不能超过100")
    private List<Long> approvalIds;

    /** 审批结果（true通过，false拒绝）. */
    private Boolean result;

    /** 审批意见. */
    private String comment;
  }
}
