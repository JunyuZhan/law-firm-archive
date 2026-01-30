package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CloseMatterCommand;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.command.UpdateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.matter.dto.MatterSimpleDTO;
import com.lawfirm.application.matter.dto.MatterTimelineDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.matter.service.MatterTimelineAppService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 案件管理 Controller */
@RestController
@RequestMapping("/matter")
@RequiredArgsConstructor
public class MatterController {

  /** 项目应用服务 */
  private final MatterAppService matterAppService;

  /** 项目时间线应用服务 */
  private final MatterTimelineAppService matterTimelineAppService;

  /** 审批人服务 */
  private final ApproverService approverService;

  /**
   * 分页查询案件列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("matter:list")
  public Result<PageResult<MatterDTO>> listMatters(final MatterQueryDTO query) {
    PageResult<MatterDTO> result = matterAppService.listMatters(query);
    return Result.success(result);
  }

  /**
   * 获取项目选择列表（公共接口，无需权限） 用于其他模块的下拉选择框，返回简化的项目信息 所有登录用户都可以访问
   *
   * <p>安全说明：只返回必要字段（id、编号、名称、状态），不包含金额、对方信息等敏感数据
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "获取项目选择列表", description = "公共接口，返回项目ID、编号和名称用于下拉选择")
  @GetMapping("/select-options")
  public Result<PageResult<MatterSimpleDTO>> getMatterSelectOptions(final MatterQueryDTO query) {
    // 默认只查询进行中的项目
    if (query.getStatus() == null) {
      query.setStatus("ACTIVE");
    }
    PageResult<MatterDTO> result = matterAppService.listMatters(query);

    // 转换为简单DTO，只保留必要字段
    List<MatterSimpleDTO> simpleList =
        result.getList().stream()
            .map(
                m ->
                    MatterSimpleDTO.builder()
                        .id(m.getId())
                        .matterNo(m.getMatterNo())
                        .name(m.getName())
                        .matterType(m.getMatterType())
                        .matterTypeName(m.getMatterTypeName())
                        .status(m.getStatus())
                        .statusName(m.getStatusName())
                        .clientName(m.getClientName())
                        .contractNo(m.getContractNo())
                        .leadLawyerName(m.getLeadLawyerName())
                        .build())
            .collect(Collectors.toList());

    return Result.success(
        PageResult.of(simpleList, result.getTotal(), result.getPageNum(), result.getPageSize()));
  }

  /**
   * 查询我的案件
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/my")
  public Result<PageResult<MatterDTO>> myMatters(final MatterQueryDTO query) {
    query.setMyMatters(true);
    PageResult<MatterDTO> result = matterAppService.listMatters(query);
    return Result.success(result);
  }

  /**
   * 获取案件详情
   *
   * @param id 案件ID
   * @return 案件信息
   */
  @GetMapping("/{id}")
  @RequirePermission("matter:list")
  public Result<MatterDTO> getMatter(@PathVariable final Long id) {
    MatterDTO matter = matterAppService.getMatterById(id);
    return Result.success(matter);
  }

  /**
   * 创建案件
   *
   * @param command 创建案件命令
   * @return 案件信息
   */
  @PostMapping
  @RequirePermission("matter:create")
  @OperationLog(module = "案件管理", action = "创建案件")
  @RepeatSubmit(interval = 5000, message = "请勿重复提交案件信息")
  public Result<MatterDTO> createMatter(@RequestBody @Valid final CreateMatterCommand command) {
    MatterDTO matter = matterAppService.createMatter(command);
    return Result.success(matter);
  }

  /**
   * 更新案件
   *
   * @param command 更新案件命令
   * @return 案件信息
   */
  @PutMapping
  @RequirePermission("matter:update")
  @OperationLog(module = "案件管理", action = "更新案件")
  public Result<MatterDTO> updateMatter(@RequestBody @Valid final UpdateMatterCommand command) {
    MatterDTO matter = matterAppService.updateMatter(command);
    return Result.success(matter);
  }

  /**
   * 删除案件（已禁用 - 项目不能删除，只能编辑和归档）
   *
   * @param id 案件ID
   * @return 空结果
   * @deprecated 项目不能删除，只能通过状态改为 ARCHIVED 来归档
   */
  @Deprecated
  @DeleteMapping("/{id}")
  @RequirePermission("matter:delete")
  @OperationLog(module = "案件管理", action = "删除案件")
  public Result<Void> deleteMatter(@PathVariable final Long id) {
    throw new BusinessException("项目不能删除，只能编辑和归档。请将项目状态改为归档(ARCHIVED)");
  }

  /**
   * 修改案件状态
   *
   * @param id 案件ID
   * @param request 修改状态请求
   * @return 空结果
   */
  @PutMapping("/{id}/status")
  @RequirePermission("matter:update")
  @OperationLog(module = "案件管理", action = "修改案件状态")
  public Result<Void> changeStatus(
      @PathVariable final Long id, @RequestBody @Valid final ChangeStatusRequest request) {
    matterAppService.changeStatus(id, request.getStatus());
    return Result.success();
  }

  /**
   * 添加团队成员
   *
   * @param id 案件ID
   * @param request 添加成员请求
   * @return 空结果
   */
  @PostMapping("/{id}/participant")
  @RequirePermission("matter:update")
  @OperationLog(module = "案件管理", action = "添加团队成员")
  public Result<Void> addParticipant(
      @PathVariable final Long id, @RequestBody @Valid final AddParticipantRequest request) {
    matterAppService.addParticipant(
        id,
        request.getUserId(),
        request.getRole(),
        request.getCommissionRate(),
        request.getIsOriginator());
    return Result.success();
  }

  /**
   * 移除团队成员
   *
   * @param id 案件ID
   * @param userId 用户ID
   * @return 空结果
   */
  @DeleteMapping("/{id}/participant/{userId}")
  @RequirePermission("matter:update")
  @OperationLog(module = "案件管理", action = "移除团队成员")
  public Result<Void> removeParticipant(
      @PathVariable final Long id, @PathVariable final Long userId) {
    matterAppService.removeParticipant(id, userId);
    return Result.success();
  }

  /**
   * 获取结案可选审批人列表 规则：优先显示团队负责人（TEAM_LEADER），其次是主任（DIRECTOR）
   *
   * @return 审批人列表
   */
  @GetMapping("/close/approvers")
  @RequirePermission("matter:close")
  @Operation(summary = "获取结案审批人列表", description = "获取可选的结案审批人，优先推荐团队负责人")
  public Result<List<Map<String, Object>>> getCloseApprovers() {
    List<Map<String, Object>> approvers = approverService.getMatterCloseAvailableApprovers();
    return Result.success(approvers);
  }

  /**
   * 申请项目结案
   *
   * @param id 案件ID
   * @param command 结案命令
   * @return 案件信息
   */
  @PostMapping("/{id}/close/apply")
  @RequirePermission("matter:close")
  @OperationLog(module = "案件管理", action = "申请项目结案")
  public Result<MatterDTO> applyCloseMatter(
      @PathVariable final Long id, @RequestBody @Valid final CloseMatterCommand command) {
    command.setMatterId(id);
    MatterDTO matter = matterAppService.applyCloseMatter(command);
    return Result.success(matter);
  }

  /**
   * 审批项目结案
   *
   * @param id 案件ID
   * @param request 审批结案请求
   * @return 案件信息
   */
  @PostMapping("/{id}/close/approve")
  @RequirePermission("matter:approve")
  @OperationLog(module = "案件管理", action = "审批项目结案")
  public Result<MatterDTO> approveCloseMatter(
      @PathVariable final Long id, @RequestBody @Valid final ApproveCloseRequest request) {
    MatterDTO matter =
        matterAppService.approveCloseMatter(id, request.getApproved(), request.getComment());
    return Result.success(matter);
  }

  /**
   * 生成结案报告
   *
   * @param id 案件ID
   * @return 结案报告
   */
  @GetMapping("/{id}/close/report")
  @RequirePermission("matter:view")
  @OperationLog(module = "案件管理", action = "生成结案报告")
  public Result<String> generateCloseReport(@PathVariable final Long id) {
    String report = matterAppService.generateCloseReport(id);
    return Result.success(report);
  }

  /**
   * 获取项目时间线（M3-024，P2）
   *
   * @param id 案件ID
   * @return 时间线列表
   */
  @GetMapping("/{id}/timeline")
  @RequirePermission("matter:view")
  @Operation(summary = "获取项目时间线", description = "获取项目的所有关键事件时间线")
  public Result<List<MatterTimelineDTO>> getMatterTimeline(@PathVariable final Long id) {
    List<MatterTimelineDTO> timeline = matterTimelineAppService.getMatterTimeline(id);
    return Result.success(timeline);
  }

  /**
   * 基于合同创建项目 从已审批的合同自动填充项目信息
   *
   * @param contractId 合同ID
   * @param command 创建案件命令
   * @return 案件信息
   */
  @PostMapping("/from-contract/{contractId}")
  @RequirePermission("matter:create")
  @OperationLog(module = "案件管理", action = "基于合同创建项目")
  @Operation(summary = "基于合同创建项目", description = "从已审批的合同自动填充项目信息并创建项目")
  public Result<MatterDTO> createMatterFromContract(
      @PathVariable final Long contractId, @RequestBody @Valid final CreateMatterCommand command) {
    MatterDTO matter = matterAppService.createMatterFromContract(contractId, command);
    return Result.success(matter);
  }

  // ========== Request DTOs ==========

  /** 变更状态请求 */
  @Data
  public static class ChangeStatusRequest {
    /** 状态 */
    @NotBlank(message = "状态不能为空")
    private String status;
  }

  /** 添加参与人请求 */
  @Data
  public static class AddParticipantRequest {
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 角色 */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 提成比例 */
    private BigDecimal commissionRate;

    /** 是否发起人 */
    private Boolean isOriginator;
  }

  /** 批准结案请求 */
  @Data
  public static class ApproveCloseRequest {
    /** 是否批准 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批意见 */
    private String comment;
  }
}
