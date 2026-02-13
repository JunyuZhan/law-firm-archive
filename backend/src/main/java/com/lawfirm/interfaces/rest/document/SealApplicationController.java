package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateSealApplicationCommand;
import com.lawfirm.application.document.dto.SealApplicationDTO;
import com.lawfirm.application.document.dto.SealApplicationQueryDTO;
import com.lawfirm.application.document.service.SealApplicationAppService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用印申请接口 */
@RestController
@RequestMapping("/document/seal-application")
@RequiredArgsConstructor
public class SealApplicationController {

  /** 用印申请应用服务 */
  private final SealApplicationAppService applicationAppService;

  /** 审批人服务 */
  private final ApproverService approverService;

  /**
   * 分页查询用印申请
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("doc:seal:apply")
  public Result<PageResult<SealApplicationDTO>> list(final SealApplicationQueryDTO query) {
    return Result.success(applicationAppService.listApplications(query));
  }

  /**
   * 获取申请详情
   *
   * @param id 申请ID
   * @return 申请详情
   */
  @GetMapping("/{id}")
  @RequirePermission("doc:seal:apply")
  public Result<SealApplicationDTO> getById(@PathVariable final Long id) {
    return Result.success(applicationAppService.getApplicationById(id));
  }

  /**
   * 创建用印申请
   *
   * @param command 创建申请命令
   * @return 申请信息
   */
  @PostMapping
  @RequirePermission("doc:seal:apply")
  @OperationLog(module = "用印申请", action = "提交申请")
  public Result<SealApplicationDTO> create(
      @Valid @RequestBody final CreateSealApplicationCommand command) {
    return Result.success(applicationAppService.createApplication(command));
  }

  /**
   * 审批通过 注意：建议统一使用审批中心接口 /workbench/approval/approve 进行审批 此接口保留用于向后兼容，但权限已改为 approval:approve
   *
   * @param id 申请ID
   * @param comment 审批意见
   * @return 申请信息
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("approval:approve")
  @OperationLog(module = "用印申请", action = "审批通过")
  public Result<SealApplicationDTO> approve(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    return Result.success(applicationAppService.approve(id, comment));
  }

  /**
   * 审批拒绝 注意：建议统一使用审批中心接口 /workbench/approval/approve 进行审批 此接口保留用于向后兼容，但权限已改为 approval:approve
   *
   * @param id 申请ID
   * @param comment 审批意见
   * @return 申请信息
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("approval:approve")
  @OperationLog(module = "用印申请", action = "审批拒绝")
  public Result<SealApplicationDTO> reject(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    return Result.success(applicationAppService.reject(id, comment));
  }

  /**
   * 登记用印
   *
   * @param id 申请ID
   * @param remark 备注
   * @return 申请信息
   */
  @PostMapping("/{id}/use")
  @RequirePermission("doc:seal:apply")
  @OperationLog(module = "用印申请", action = "登记用印")
  public Result<SealApplicationDTO> registerUsage(
      @PathVariable final Long id, @RequestParam(required = false) final String remark) {
    return Result.success(applicationAppService.registerUsage(id, remark));
  }

  /**
   * 取消申请
   *
   * @param id 申请ID
   * @return 空结果
   */
  @PostMapping("/{id}/cancel")
  @RequirePermission("doc:seal:apply")
  @OperationLog(module = "用印申请", action = "取消申请")
  public Result<Void> cancel(@PathVariable final Long id) {
    applicationAppService.cancelApplication(id);
    return Result.success();
  }

  /**
   * 获取待审批列表
   *
   * @return 待审批申请列表
   */
  @GetMapping("/pending")
  @RequirePermission("doc:seal:apply")
  public Result<List<SealApplicationDTO>> getPending() {
    return Result.success(applicationAppService.getPendingApplications());
  }

  /**
   * 获取可选审批人列表（申请人架构垂直线上的领导） 包括：当前部门负责人、上级部门负责人、合伙人、主任
   *
   * @param applicantId 申请人ID（可选）
   * @return 审批人列表
   */
  @GetMapping("/approvers")
  @RequirePermission("doc:seal:apply")
  public Result<List<Map<String, Object>>> getApprovers(
      @RequestParam(required = false) final Long applicantId) {
    List<Map<String, Object>> approvers =
        approverService.getSealApplicationAvailableApprovers(applicantId);
    return Result.success(approvers);
  }

  /**
   * 获取保管人待办理的申请（审批通过且印章的保管人是当前用户）
   *
   * @return 待办理申请列表
   */
  @GetMapping("/keeper/pending")
  @RequirePermission("doc:seal:apply")
  public Result<List<SealApplicationDTO>> getPendingForKeeper() {
    Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
    return Result.success(applicationAppService.getPendingForKeeper(userId));
  }

  /**
   * 获取保管人已办理的申请（已用印且印章的保管人是当前用户）
   *
   * @return 已办理申请列表
   */
  @GetMapping("/keeper/processed")
  @RequirePermission("doc:seal:apply")
  public Result<List<SealApplicationDTO>> getProcessedByKeeper() {
    Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
    return Result.success(applicationAppService.getProcessedByKeeper(userId));
  }

  /**
   * 检查当前用户是否是任何印章的保管人
   *
   * @return 是否是保管人
   */
  @GetMapping("/keeper/check")
  @RequirePermission("doc:seal:apply")
  public Result<Boolean> checkIsKeeper() {
    return Result.success(applicationAppService.isAnySealKeeper());
  }
}
