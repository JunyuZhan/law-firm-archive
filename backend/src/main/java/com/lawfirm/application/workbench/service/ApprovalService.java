package com.lawfirm.application.workbench.service;

import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 审批服务工具类 用于各业务模块创建审批记录. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

  /** Approval Repository. */
  private final ApprovalRepository approvalRepository;

  /** User Repository. */
  private final UserRepository userRepository;

  /** Notification App Service. */
  private final NotificationAppService notificationAppService;

  /** Application Event Publisher. */
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 创建审批记录
   *
   * @param businessType 业务类型（CONTRACT, SEAL_APPLICATION, CONFLICT_CHECK, etc.）
   * @param businessId 业务ID
   * @param businessNo 业务编号
   * @param businessTitle 业务标题
   * @param approverId 审批人ID
   * @param priority 优先级（HIGH, MEDIUM, LOW）
   * @param urgency 紧急程度（URGENT, NORMAL）
   * @param businessSnapshot 业务数据快照（JSON格式）
   * @return 审批记录ID
   */
  @Transactional
  public Long createApproval(
      final String businessType,
      final Long businessId,
      final String businessNo,
      final String businessTitle,
      final Long approverId,
      final String priority,
      final String urgency,
      final String businessSnapshot) {
    Long applicantId = SecurityUtils.getUserId();
    String applicantName = SecurityUtils.getRealName();

    // 获取审批人姓名
    User approver = userRepository.getById(approverId);
    if (approver == null) {
      throw new BusinessException("审批人不存在");
    }
    String approverName = approver.getRealName();

    Approval approval =
        Approval.builder()
            .approvalNo(generateApprovalNo())
            .businessType(businessType)
            .businessId(businessId)
            .businessNo(businessNo)
            .businessTitle(businessTitle)
            .applicantId(applicantId)
            .applicantName(applicantName)
            .approverId(approverId)
            .approverName(approverName)
            .status("PENDING")
            .priority(priority != null ? priority : "MEDIUM")
            .urgency(urgency != null ? urgency : "NORMAL")
            .businessSnapshot(businessSnapshot)
            .build();

    approvalRepository.save(approval);
    log.info(
        "创建审批记录: approvalNo={}, businessType={}, businessId={}, approver={}",
        approval.getApprovalNo(),
        businessType,
        businessId,
        approverId);

    // 发送通知给审批人
    try {
      String businessTypeName = getBusinessTypeName(businessType);
      String title = "您有新的" + businessTypeName + "待审批";
      String content =
          String.format(
              "%s 提交了【%s】，请您审批。",
              applicantName, businessTitle != null ? businessTitle : businessTypeName);
      notificationAppService.sendSystemNotification(
          approverId, title, content, "APPROVAL", approval.getId());
      log.info("已发送审批通知给: {}", approverName);
    } catch (Exception e) {
      log.warn("发送审批通知失败: approverId={}", approverId, e);
    }

    return approval.getId();
  }

  /**
   * 获取业务类型名称.
   *
   * @param businessType 业务类型代码
   * @return 业务类型名称
   */
  private String getBusinessTypeName(final String businessType) {
    if (businessType == null) {
      return "审批";
    }
    return switch (businessType) {
      case "CONTRACT" -> "合同";
      case "SEAL_APPLICATION" -> "用印申请";
      case "CONFLICT_CHECK" -> "利冲检查";
      case "CONFLICT_EXEMPTION" -> "利冲豁免";
      case "EXPENSE" -> "费用报销";
      case "PAYMENT_AMENDMENT" -> "收款变更";
      case "MATTER_CLOSE" -> "项目结案";
      case "REGULARIZATION" -> "转正申请";
      case "RESIGNATION" -> "离职申请";
      case "LETTER_APPLICATION" -> "出函申请";
      case "ARCHIVE_STORE" -> "档案入库";
      default -> "审批";
    };
  }

  /**
   * 创建审批记录（简化版，使用默认优先级和紧急程度）.
   *
   * @param businessType 业务类型
   * @param businessId 业务ID
   * @param businessNo 业务编号
   * @param businessTitle 业务标题
   * @param approverId 审批人ID
   * @return 审批记录ID
   */
  @Transactional
  public Long createApproval(
      final String businessType,
      final Long businessId,
      final String businessNo,
      final String businessTitle,
      final Long approverId) {
    return createApproval(
        businessType, businessId, businessNo, businessTitle, approverId, "MEDIUM", "NORMAL", null);
  }

  /**
   * 审批完成后，更新业务模块状态 通过发布事件的方式通知业务模块更新状态
   *
   * @param approvalId 审批记录ID
   * @param result 审批结果（APPROVED 或 REJECTED）
   * @param comment 审批意见
   */
  @Transactional
  public void onApprovalCompleted(
      final Long approvalId, final String result, final String comment) {
    Approval approval = approvalRepository.getByIdOrThrow(approvalId, "审批记录不存在");

    String businessType = approval.getBusinessType();
    Long businessId = approval.getBusinessId();

    // 发布审批完成事件，由各业务模块的监听器处理
    ApprovalCompletedEvent event =
        new ApprovalCompletedEvent(this, approvalId, businessType, businessId, result, comment);
    eventPublisher.publishEvent(event);

    log.info(
        "审批完成事件已发布: approvalId={}, businessType={}, businessId={}, result={}",
        approvalId,
        businessType,
        businessId,
        result);
  }

  /**
   * 审批完成后，更新业务模块状态（兼容旧接口）.
   *
   * @param approvalId 审批记录ID
   * @param result 审批结果（APPROVED 或 REJECTED）
   */
  @Transactional
  public void onApprovalCompleted(final Long approvalId, final String result) {
    onApprovalCompleted(approvalId, result, null);
  }

  /**
   * 取消审批记录.
   *
   * @param approvalId 审批记录ID
   */
  @Transactional
  public void cancelApproval(final Long approvalId) {
    Approval approval = approvalRepository.findById(approvalId);
    if (approval == null) {
      log.warn("取消审批：审批记录不存在, id={}", approvalId);
      return;
    }

    if (!"PENDING".equals(approval.getStatus())) {
      log.warn("取消审批：审批状态不是待审批, id={}, status={}", approvalId, approval.getStatus());
      return;
    }

    approval.setStatus("CANCELLED");
    approvalRepository.updateById(approval);

    log.info("审批记录已取消: approvalId={}, approvalNo={}", approvalId, approval.getApprovalNo());
  }

  /**
   * 生成审批编号.
   *
   * @return 审批编号
   */
  private String generateApprovalNo() {
    return "APP"
        + System.currentTimeMillis()
        + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
