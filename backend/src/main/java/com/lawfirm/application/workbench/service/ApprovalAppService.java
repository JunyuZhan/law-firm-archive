package com.lawfirm.application.workbench.service;

import com.lawfirm.application.document.service.FileAccessService;
import com.lawfirm.application.finance.command.ApproveExpenseCommand;
import com.lawfirm.application.finance.service.ExpenseAppService;
import com.lawfirm.application.hr.command.ApproveRegularizationCommand;
import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.service.RegularizationAppService;
import com.lawfirm.application.hr.service.ResignationAppService;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.workbench.command.ApproveCommand;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.dto.ApprovalQueryDTO;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 审批应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalAppService {

  /** 审批仓储 */
  private final ApprovalRepository approvalRepository;

  /** 审批Mapper */
  private final ApprovalMapper approvalMapper;

  /** 应用事件发布器 */
  private final ApplicationEventPublisher eventPublisher;

  /** 项目应用服务 */
  private final MatterAppService matterAppService;

  /** 费用应用服务 */
  private final ExpenseAppService expenseAppService;

  /** 转正应用服务 */
  private final RegularizationAppService regularizationAppService;

  /** 离职应用服务 */
  private final ResignationAppService resignationAppService;

  /** 文件访问服务 */
  private final FileAccessService fileAccessService;

  /** Minio对象存储服务 */
  @SuppressWarnings("unused")
  private final MinioService minioService;

  /**
   * 分页查询审批记录 数据权限过滤： - ALL: 可查看所有审批 - DEPT_AND_CHILD: 可查看本部门及下级部门的审批 - DEPT: 可查看本部门的审批 - SELF:
   * 只能查看自己发起或需要自己审批的记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ApprovalDTO> listApprovals(final ApprovalQueryDTO query) {
    try {
      Long currentUserId = SecurityUtils.getUserId();
      boolean isAdmin = isAdminOrDirector();

      // 计算分页偏移量
      int offset = (query.getPageNum() - 1) * query.getPageSize();

      // 在数据库层面进行权限过滤和分页（避免内存分页性能问题）
      List<Approval> approvals =
          approvalMapper.selectApprovalPageWithPermission(
              query.getStatus(),
              query.getBusinessType(),
              query.getApplicantId(),
              query.getApproverId(),
              currentUserId,
              isAdmin,
              offset,
              query.getPageSize());

      // 查询总数（用于分页）
      long total =
          approvalMapper.countApprovalWithPermission(
              query.getStatus(),
              query.getBusinessType(),
              query.getApplicantId(),
              query.getApproverId(),
              currentUserId,
              isAdmin);

      List<ApprovalDTO> dtos = approvals.stream().map(this::toDTO).collect(Collectors.toList());

      return PageResult.of(dtos, total, query.getPageNum(), query.getPageSize());
    } catch (Exception e) {
      log.error("查询审批记录失败", e);
      // 返回空结果，避免500错误
      return PageResult.of(new ArrayList<>(), 0, query.getPageNum(), query.getPageSize());
    }
  }

  /**
   * 判断当前用户是否为管理员或主任
   *
   * @return 是否为管理员或主任
   */
  private boolean isAdminOrDirector() {
    java.util.Set<String> roles = SecurityUtils.getRoles();
    return roles != null && (roles.contains("ADMIN") || roles.contains("DIRECTOR"));
  }

  /**
   * 获取待审批列表
   *
   * @return 审批列表
   */
  public List<ApprovalDTO> getPendingApprovals() {
    Long currentUserId = SecurityUtils.getUserId();
    List<Approval> approvals = approvalMapper.selectPendingApprovals(currentUserId);
    return approvals.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取我发起的审批
   *
   * @return 审批列表
   */
  public List<ApprovalDTO> getMyInitiatedApprovals() {
    Long currentUserId = SecurityUtils.getUserId();
    List<Approval> approvals = approvalMapper.selectMyInitiatedApprovals(currentUserId);
    return approvals.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取审批历史（我处理过的审批记录） 审批中心的数据权限逻辑： - 管理员/主任：可查看全部已完成审批（用于管理和统计） - 其他角色（包括财务）：只能查看自己发起或自己审批过的记录
   *
   * @return 审批列表
   */
  public List<ApprovalDTO> getMyApprovedHistory() {
    Long currentUserId = SecurityUtils.getUserId();

    List<Approval> approvals;

    if (isAdminOrDirector()) {
      // 管理员/主任可查看所有已完成审批
      approvals = approvalMapper.selectAllApprovalHistory();
    } else {
      // 其他角色：只能查看自己发起或自己审批过的记录
      approvals = approvalMapper.selectSelfApprovalHistory(currentUserId);
    }

    return approvals.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取审批详情 权限检查：只有申请人、审批人、或管理员/主任可以查看
   *
   * @param id 审批ID
   * @return 审批DTO
   */
  public ApprovalDTO getApprovalById(final Long id) {
    Approval approval = approvalRepository.getByIdOrThrow(id, "审批记录不存在");

    // 数据权限检查
    Long currentUserId = SecurityUtils.getUserId();

    // 管理员/主任可查看所有
    if (isAdminOrDirector()) {
      return toDTO(approval);
    }

    // 申请人或审批人可查看（审批中心的核心逻辑）
    if (currentUserId.equals(approval.getApplicantId())
        || currentUserId.equals(approval.getApproverId())) {
      return toDTO(approval);
    }

    throw new BusinessException("无权查看此审批记录");
  }

  /**
   * 审批操作（通过/拒绝） 修复：业务状态更新失败时回滚整个事务，保证数据一致性
   *
   * @param command 审批命令
   */
  @Transactional(rollbackFor = Exception.class)
  public void approve(final ApproveCommand command) {
    Approval approval = approvalRepository.getByIdOrThrow(command.getApprovalId(), "审批记录不存在");

    // 检查权限
    Long currentUserId = SecurityUtils.getUserId();
    if (!approval.getApproverId().equals(currentUserId)) {
      throw new BusinessException("无权审批此记录");
    }

    // 检查状态
    if (!ApprovalStatus.canApprove(approval.getStatus())) {
      throw new BusinessException("该审批记录已处理，无法重复审批");
    }

    // 拒绝时必须填写拒绝事由
    if (ApprovalStatus.REJECTED.equals(command.getResult())) {
      if (command.getComment() == null || command.getComment().trim().isEmpty()) {
        throw new BusinessException("拒绝时必须填写拒绝事由");
      }
    }

    String businessType = approval.getBusinessType();
    Long businessId = approval.getBusinessId();
    Boolean approved = ApprovalStatus.APPROVED.equals(command.getResult());

    // 先更新业务状态（失败则整个事务回滚）
    updateBusinessStatus(businessType, businessId, approved, command.getComment());

    // 业务状态更新成功后，更新审批状态
    approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
    approval.setComment(command.getComment());
    approval.setApprovedAt(LocalDateTime.now());
    approvalRepository.updateById(approval);

    log.info(
        "审批完成: approvalNo={}, result={}, approver={}",
        approval.getApprovalNo(),
        command.getResult(),
        currentUserId);

    // 发布审批完成事件，通知其他业务模块
    eventPublisher.publishEvent(
        new ApprovalCompletedEvent(
            this,
            approval.getId(),
            approval.getBusinessType(),
            approval.getBusinessId(),
            command.getResult(),
            command.getComment()));
  }

  /**
   * 更新业务状态（独立方法，异常向上抛出以触发事务回滚）
   *
   * @param businessType 业务类型
   * @param businessId 业务ID
   * @param approved 是否通过
   * @param comment 审批意见
   */
  private void updateBusinessStatus(
      final String businessType,
      final Long businessId,
      final Boolean approved,
      final String comment) {
    switch (businessType) {
      case "MATTER_CLOSE":
        // 项目结案审批有特殊的业务逻辑（如触发归档流程）
        matterAppService.approveCloseMatter(businessId, approved, comment);
        log.info("项目结案审批业务状态已更新: matterId={}, approved={}", businessId, approved);
        break;

      case "EXPENSE":
        // 费用报销审批
        ApproveExpenseCommand expenseCommand = new ApproveExpenseCommand();
        expenseCommand.setExpenseId(businessId);
        expenseCommand.setAction(approved ? "APPROVE" : "REJECT");
        expenseCommand.setComment(comment);
        expenseAppService.approveExpense(expenseCommand);
        log.info("费用报销审批业务状态已更新: expenseId={}, approved={}", businessId, approved);
        break;

      case "REGULARIZATION":
        // 转正申请审批
        ApproveRegularizationCommand regCommand = new ApproveRegularizationCommand();
        regCommand.setApproved(approved);
        regCommand.setComment(comment);
        regularizationAppService.approveRegularization(businessId, regCommand);
        log.info("转正申请审批业务状态已更新: regularizationId={}, approved={}", businessId, approved);
        break;

      case "RESIGNATION":
        // 离职申请审批
        ApproveResignationCommand resCommand = new ApproveResignationCommand();
        resCommand.setApproved(approved);
        resCommand.setComment(comment);
        resignationAppService.approveResignation(businessId, resCommand);
        log.info("离职申请审批业务状态已更新: resignationId={}, approved={}", businessId, approved);
        break;

      default:
        // 其他审批类型由事件监听器处理，不需要在这里更新
        log.debug("业务类型 {} 由事件监听器处理", businessType);
        break;
    }
  }

  /**
   * 批量审批（全部成功或全部失败） 修复：先验证所有记录，再批量更新，保证事务原子性
   *
   * @param approvalIds 审批ID列表
   * @param result 审批结果
   * @param comment 审批意见
   * @return 批量审批结果
   */
  @Transactional(rollbackFor = Exception.class)
  public BatchApproveResult batchApprove(
      final List<Long> approvalIds, final String result, final String comment) {
    Long currentUserId = SecurityUtils.getUserId();

    if (approvalIds == null || approvalIds.isEmpty()) {
      throw new BusinessException("请选择要审批的记录");
    }

    // 第1阶段：批量查询所有审批记录（避免N+1查询）
    List<Approval> approvals = approvalRepository.listByIds(approvalIds);
    if (approvals.size() != approvalIds.size()) {
      // 找出缺失的审批记录ID
      java.util.Set<Long> foundIds =
          approvals.stream().map(Approval::getId).collect(java.util.stream.Collectors.toSet());
      java.util.List<Long> missingIds =
          approvalIds.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new BusinessException("审批记录不存在: " + missingIds);
    }

    // 验证所有审批记录的权限和状态
    for (Approval approval : approvals) {
      if (!approval.getApproverId().equals(currentUserId)) {
        throw new BusinessException("无权审批此记录: " + approval.getApprovalNo());
      }
      if (!ApprovalStatus.canApprove(approval.getStatus())) {
        throw new BusinessException("该审批记录已处理: " + approval.getApprovalNo());
      }
    }

    // 第2阶段：批量更新（所有验证通过后）
    Boolean approved = ApprovalStatus.APPROVED.equals(result);
    for (Approval approval : approvals) {
      approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
      approval.setComment(comment);
      approval.setApprovedAt(LocalDateTime.now());
      approvalRepository.updateById(approval);

      // 发布审批完成事件
      eventPublisher.publishEvent(
          new ApprovalCompletedEvent(
              this,
              approval.getId(),
              approval.getBusinessType(),
              approval.getBusinessId(),
              result,
              comment));
    }

    log.info("批量审批完成: 总数={}, result={}, approver={}", approvalIds.size(), result, currentUserId);

    return BatchApproveResult.builder()
        .total(approvalIds.size())
        .successCount(approvalIds.size())
        .skipCount(0)
        .errors(java.util.Collections.emptyList())
        .build();
  }

  /** 批量审批结果 */
  @lombok.Data
  @lombok.Builder
  public static class BatchApproveResult {
    /** 总数 */
    private int total;

    /** 成功数 */
    private int successCount;

    /** 跳过数 */
    private int skipCount;

    /** 错误列表 */
    private List<String> errors;
  }

  /**
   * 获取业务审批记录
   *
   * @param businessType 业务类型
   * @param businessId 业务ID
   * @return 审批列表
   */
  public List<ApprovalDTO> getBusinessApprovals(final String businessType, final Long businessId) {
    List<Approval> approvals = approvalMapper.selectByBusiness(businessType, businessId);
    return approvals.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * Entity 转 DTO
   *
   * @param approval 审批实体
   * @return 审批DTO
   */
  private ApprovalDTO toDTO(final Approval approval) {
    ApprovalDTO dto = new ApprovalDTO();
    dto.setId(approval.getId());
    dto.setApprovalNo(approval.getApprovalNo());
    dto.setBusinessType(approval.getBusinessType());
    dto.setBusinessTypeName(getBusinessTypeName(approval.getBusinessType()));
    dto.setBusinessId(approval.getBusinessId());
    dto.setBusinessNo(approval.getBusinessNo());
    dto.setBusinessTitle(approval.getBusinessTitle());
    dto.setApplicantId(approval.getApplicantId());
    dto.setApplicantName(approval.getApplicantName());
    dto.setApproverId(approval.getApproverId());
    dto.setApproverName(approval.getApproverName());
    dto.setStatus(approval.getStatus());
    dto.setStatusName(getStatusName(approval.getStatus()));
    dto.setComment(approval.getComment());
    dto.setApprovedAt(approval.getApprovedAt());
    dto.setPriority(approval.getPriority());
    dto.setPriorityName(getPriorityName(approval.getPriority()));
    dto.setUrgency(approval.getUrgency());
    dto.setUrgencyName(getUrgencyName(approval.getUrgency()));
    dto.setCreatedAt(approval.getCreatedAt());
    dto.setUpdatedAt(approval.getUpdatedAt());
    dto.setBusinessSnapshot(approval.getBusinessSnapshot());
    return dto;
  }

  private String getBusinessTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "CONTRACT" -> "合同审批";
      case "SEAL_APPLICATION" -> "用印申请";
      case "CONFLICT_CHECK" -> "利冲检查";
      case "CONFLICT_EXEMPTION" -> "利冲豁免";
      case "EXPENSE" -> "费用报销";
      case "PAYMENT_AMENDMENT" -> "收款变更";
      case "MATTER_CLOSE" -> "项目结案";
      case "REGULARIZATION" -> "转正申请";
      case "RESIGNATION" -> "离职申请";
      case "LETTER_APPLICATION" -> "出函申请";
      default -> type;
    };
  }

  private String getStatusName(final String status) {
    return ApprovalStatus.getStatusName(status);
  }

  private String getPriorityName(final String priority) {
    if (priority == null) {
      return null;
    }
    return switch (priority) {
      case "HIGH" -> "高";
      case "MEDIUM" -> "中";
      case "LOW" -> "低";
      default -> priority;
    };
  }

  private String getUrgencyName(final String urgency) {
    if (urgency == null) {
      return null;
    }
    return switch (urgency) {
      case "URGENT" -> "紧急";
      case "NORMAL" -> "普通";
      default -> urgency;
    };
  }

  /**
   * 上传审批附件文件
   *
   * @param file 文件
   * @param approvalId 审批ID
   * @return 文件URL
   */
  @Transactional
  /**
   * 上传审批附件
   *
   * @param file 文件
   * @param approvalId 审批ID
   * @return 文件URL
   */
  public String uploadApprovalFile(final MultipartFile file, final Long approvalId) {
    Approval approval = approvalRepository.getByIdOrThrow(approvalId, "审批记录不存在");

    // 使用FileAccessService上传文件（审批附件的路径包含businessType）
    Map<String, String> storageInfo =
        fileAccessService.uploadFile(
            file,
            MinioPathGenerator.FileType.APPROVAL,
            approval.getBusinessId(), // 这里使用businessId，实际应该从业务对象获取matterId
            approval.getBusinessType() // folder参数代表businessType
            );

    // 设置存储信息
    approval.setFileUrl(storageInfo.get("fileUrl"));
    approval.setBucketName(storageInfo.get("bucketName"));
    approval.setStoragePath(storageInfo.get("storagePath"));
    approval.setPhysicalName(storageInfo.get("physicalName"));
    approval.setFileHash(storageInfo.get("fileHash"));

    approvalRepository.updateById(approval);

    log.info(
        "审批附件文件上传成功: approvalId={}, fileName={}, storagePath={}",
        approvalId,
        file.getOriginalFilename(),
        storageInfo.get("storagePath"));

    return storageInfo.get("fileUrl");
  }
}
