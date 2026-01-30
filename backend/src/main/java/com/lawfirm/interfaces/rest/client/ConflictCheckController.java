package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateConflictCheckCommand;
import com.lawfirm.application.client.dto.ConflictCheckDTO;
import com.lawfirm.application.client.dto.ConflictCheckQueryDTO;
import com.lawfirm.application.client.service.ConflictCheckAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 利冲检查 Controller */
@RestController
@RequestMapping("/client/conflict-check")
@RequiredArgsConstructor
public class ConflictCheckController {

  /** 利冲检查服务. */
  private final ConflictCheckAppService conflictCheckAppService;

  /**
   * 分页查询利冲检查列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("conflict:list")
  public Result<PageResult<ConflictCheckDTO>> listConflictChecks(
      final ConflictCheckQueryDTO query) {
    PageResult<ConflictCheckDTO> result = conflictCheckAppService.listConflictChecks(query);
    return Result.success(result);
  }

  /**
   * 获取利冲检查详情
   *
   * @param id 检查ID
   * @return 检查详情
   */
  @GetMapping("/{id}")
  @RequirePermission("conflict:list")
  public Result<ConflictCheckDTO> getConflictCheck(@PathVariable final Long id) {
    ConflictCheckDTO check = conflictCheckAppService.getConflictCheckById(id);
    return Result.success(check);
  }

  /**
   * 创建利冲检查（关联案件）
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("conflict:create")
  @OperationLog(module = "利冲检查", action = "创建利冲检查")
  public Result<ConflictCheckDTO> createConflictCheck(
      @RequestBody @Valid final CreateConflictCheckCommand command) {
    ConflictCheckDTO check = conflictCheckAppService.createConflictCheck(command);
    return Result.success(check);
  }

  /**
   * 申请利冲审查（简化版，手动申请） 用于前端手动申请利冲检查，不需要关联案件
   *
   * @param request 申请请求
   * @return 创建结果
   */
  @PostMapping("/apply")
  @RequirePermission("conflict:create")
  @OperationLog(module = "利冲检查", action = "申请利冲审查")
  public Result<ConflictCheckDTO> applyConflictCheck(
      @RequestBody @Valid final ApplyConflictCheckRequest request) {
    ConflictCheckDTO check =
        conflictCheckAppService.applyConflictCheck(
            request.getClientName(),
            request.getOpposingParty(),
            request.getMatterName(),
            request.getCheckType(),
            request.getRemark());
    return Result.success(check);
  }

  /**
   * 审核通过（豁免）
   *
   * @param id 检查ID
   * @param request 审核请求
   * @return 无返回
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("conflict:approve")
  @OperationLog(module = "利冲检查", action = "审核通过")
  public Result<Void> approve(
      @PathVariable final Long id, @RequestBody final ReviewRequest request) {
    conflictCheckAppService.approve(id, request.getComment());
    return Result.success();
  }

  /**
   * 审核拒绝
   *
   * @param id 检查ID
   * @param request 审核请求
   * @return 无返回
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("conflict:approve")
  @OperationLog(module = "利冲检查", action = "审核拒绝")
  public Result<Void> reject(
      @PathVariable final Long id, @RequestBody final ReviewRequest request) {
    conflictCheckAppService.reject(id, request.getComment());
    return Result.success();
  }

  /**
   * 申请利益冲突豁免
   *
   * @param command 申请命令
   * @return 检查结果
   */
  @PostMapping("/exemption/apply")
  @RequirePermission("conflict:exemption")
  @OperationLog(module = "利冲检查", action = "申请豁免")
  public Result<ConflictCheckDTO> applyExemption(
      @RequestBody @Valid
          final com.lawfirm.application.client.command.ApplyExemptionCommand command) {
    ConflictCheckDTO result = conflictCheckAppService.applyExemption(command);
    return Result.success(result);
  }

  /**
   * 批准豁免申请
   *
   * @param id 检查ID
   * @param request 审核请求
   * @return 无返回
   */
  @PostMapping("/exemption/{id}/approve")
  @RequirePermission("conflict:exemption:approve")
  @OperationLog(module = "利冲检查", action = "批准豁免")
  public Result<Void> approveExemption(
      @PathVariable final Long id, @RequestBody final ReviewRequest request) {
    conflictCheckAppService.approveExemption(id, request.getComment());
    return Result.success();
  }

  /**
   * 拒绝豁免申请
   *
   * @param id 检查ID
   * @param request 审核请求
   * @return 无返回
   */
  @PostMapping("/exemption/{id}/reject")
  @RequirePermission("conflict:exemption:approve")
  @OperationLog(module = "利冲检查", action = "拒绝豁免")
  public Result<Void> rejectExemption(
      @PathVariable final Long id, @RequestBody final ReviewRequest request) {
    conflictCheckAppService.rejectExemption(id, request.getComment());
    return Result.success();
  }

  /**
   * 快速利冲检索（不创建记录，仅检查是否存在冲突） 用于新增客户时的实时检查
   *
   * @param request 检索请求
   * @return 检索结果
   */
  @PostMapping("/quick")
  @RequirePermission("conflict:list")
  public Result<QuickConflictCheckResponse> quickConflictCheck(
      @RequestBody @Valid final QuickConflictCheckRequest request) {
    var result =
        conflictCheckAppService.quickConflictCheck(
            request.getClientName(), request.getOpposingParty());
    QuickConflictCheckResponse response = new QuickConflictCheckResponse();
    response.setHasConflict(result.hasConflict());
    response.setConflictDetail(result.conflictDetail());
    response.setCandidates(
        result.candidates().stream()
            .map(CandidateDTO::from)
            .collect(java.util.stream.Collectors.toList()));
    response.setRiskLevel(result.riskLevel());
    response.setRiskSummary(result.riskSummary());
    return Result.success(response);
  }

  // ========== Request DTOs ==========

  /** 审核请求DTO */
  @Data
  public static class ReviewRequest {
    /** 审核意见 */
    private String comment;
  }

  /** 申请请求DTO */
  @Data
  public static class ApplyConflictCheckRequest {
    /** 客户名称 */
    @NotBlank(message = "客户名称不能为空")
    private String clientName;

    /** 对方当事人 */
    @NotBlank(message = "对方当事人不能为空")
    private String opposingParty;

    /** 案件名称 */
    @NotBlank(message = "案件名称不能为空")
    private String matterName;

    /** 检查类型 */
    private String checkType;

    /** 备注 */
    private String remark;
  }

  /** 快速检索请求DTO */
  @Data
  public static class QuickConflictCheckRequest {
    /** 客户名称 */
    @NotBlank(message = "客户名称不能为空")
    private String clientName;

    /** 对方当事人 */
    @NotBlank(message = "对方当事人不能为空")
    private String opposingParty;
  }

  /** 快速检索响应DTO */
  @Data
  public static class QuickConflictCheckResponse {
    /** 是否存在冲突 */
    private boolean hasConflict;

    /** 冲突详情 */
    private String conflictDetail;

    /** 候选人列表 */
    private java.util.List<CandidateDTO> candidates;

    /** 风险等级 */
    private String riskLevel;

    /** 风险摘要 */
    private String riskSummary;
  }

  /** 候选人DTO */
  @Data
  public static class CandidateDTO {
    /** 客户ID */
    private Long clientId;

    /** 客户编号 */
    private String clientNo;

    /** 客户名称 */
    private String clientName;

    /** 客户类型 */
    private String clientType;

    /** 匹配分数 */
    private int matchScore;

    /** 匹配类型 */
    private String matchType;

    /** 风险等级 */
    private String riskLevel;

    /** 风险原因 */
    private String riskReason;

    /**
     * 从ConflictCandidate转换为DTO
     *
     * @param c 冲突候选人
     * @return 候选人DTO
     */
    public static CandidateDTO from(final ConflictCheckAppService.ConflictCandidate c) {
      CandidateDTO dto = new CandidateDTO();
      dto.setClientId(c.clientId());
      dto.setClientNo(c.clientNo());
      dto.setClientName(c.clientName());
      dto.setClientType(c.clientType());
      dto.setMatchScore(c.matchScore());
      dto.setMatchType(c.matchType());
      dto.setRiskLevel(c.riskLevel());
      dto.setRiskReason(c.riskReason());
      return dto;
    }
  }
}
