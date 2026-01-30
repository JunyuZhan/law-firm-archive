package com.lawfirm.application.workbench.listener;

import com.lawfirm.application.admin.service.LetterAppService;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.client.service.ConflictCheckAppService;
import com.lawfirm.application.document.service.SealApplicationAppService;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.application.system.service.DataHandoverService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批事件监听器 处理审批完成后的业务逻辑
 *
 * <p>注意：不使用 @Async，确保审批状态更新在同一事务中完成 如果需要异步处理，应该在事务提交后使用 @TransactionalEventListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

  /** 合同应用服务. */
  private final ContractAppService contractAppService;

  /** 用印申请应用服务. */
  private final SealApplicationAppService sealApplicationAppService;

  /** 冲突检查应用服务. */
  private final ConflictCheckAppService conflictCheckAppService;

  /** 数据交接服务. */
  private final DataHandoverService dataHandoverService;

  /** 通知应用服务. */
  private final NotificationAppService notificationAppService;

  /** 审批仓储. */
  private final ApprovalRepository approvalRepository;

  /** 函件应用服务. */
  private final LetterAppService letterAppService;

  // 使用@Lazy避免循环依赖：ArchiveAppService -> ApprovalService -> EventListener -> ArchiveAppService
  /** 归档应用服务. */
  private ArchiveAppService archiveAppService;

  /**
   * 设置归档应用服务（使用@Lazy避免循环依赖）.
   *
   * @param archiveAppService 归档应用服务
   */
  @Autowired
  @Lazy
  public void setArchiveAppService(final ArchiveAppService archiveAppService) {
    this.archiveAppService = archiveAppService;
  }

  /**
   * 监听审批完成事件 同步处理，确保业务状态更新与审批状态更新在同一事务中.
   *
   * @param event 审批完成事件
   */
  @EventListener
  @Transactional
  public void handleApprovalCompleted(final ApprovalCompletedEvent event) {
    String businessType = event.getBusinessType();
    Long businessId = event.getBusinessId();
    String result = event.getResult();
    String comment = event.getComment();
    Long approvalId = event.getApprovalId();

    log.info(
        "处理审批完成事件: businessType={}, businessId={}, result={}", businessType, businessId, result);

    try {
      // 发送审批结果通知给申请人
      sendApprovalResultNotification(approvalId, result, comment);

      switch (businessType) {
        case "CONTRACT":
          handleContractApproval(businessId, result, comment);
          break;
        case "SEAL_APPLICATION":
          handleSealApplicationApproval(businessId, result, comment);
          break;
        case "CONFLICT_CHECK":
          handleConflictCheckApproval(businessId, result, comment);
          break;
        case "CONFLICT_EXEMPTION":
          handleConflictExemptionApproval(businessId, result, comment);
          break;
        case "DATA_HANDOVER":
          handleDataHandoverApproval(businessId, result, comment);
          break;
        case "LETTER_APPLICATION":
          handleLetterApplicationApproval(businessId, result, comment);
          break;
        case "MATTER_CLOSE":
          // 项目结案审批已经在 MatterAppService.approveCloseMatter 中处理
          // 这里不需要重复处理，避免状态冲突
          log.info("项目结案审批已完成，状态已在审批方法中更新: matterId={}, result={}", businessId, result);
          break;
        case "ARCHIVE_STORE":
          handleArchiveStoreApproval(businessId, result, comment);
          break;
        default:
          log.warn("未知的业务类型: {}", businessType);
      }
    } catch (Exception e) {
      log.error("处理审批完成事件失败: businessType={}, businessId={}", businessType, businessId, e);
    }
  }

  /**
   * 发送审批结果通知给申请人.
   *
   * @param approvalId 审批ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void sendApprovalResultNotification(
      final Long approvalId, final String result, final String comment) {
    try {
      Approval approval = approvalRepository.findById(approvalId);
      if (approval == null || approval.getApplicantId() == null) {
        return;
      }

      String businessTypeName = getBusinessTypeName(approval.getBusinessType());
      String resultText = ApprovalStatus.APPROVED.equals(result) ? "已通过" : "已拒绝";
      String title = businessTypeName + "审批" + resultText;
      String content =
          String.format(
              "您提交的【%s】%s。",
              approval.getBusinessTitle() != null ? approval.getBusinessTitle() : businessTypeName,
              resultText);
      if (comment != null && !comment.isEmpty()) {
        content += " 审批意见：" + comment;
      }

      notificationAppService.sendSystemNotification(
          approval.getApplicantId(), title, content, "APPROVAL", approvalId);

      log.info(
          "审批结果通知已发送: approvalId={}, applicantId={}, result={}",
          approvalId,
          approval.getApplicantId(),
          result);
    } catch (Exception e) {
      log.warn("发送审批结果通知失败: approvalId={}", approvalId, e);
    }
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
      case "DATA_HANDOVER" -> "数据交接";
      case "LETTER_APPLICATION" -> "出函申请";
      case "ARCHIVE_STORE" -> "档案入库";
      default -> "审批";
    };
  }

  /**
   * 处理出函申请审批.
   *
   * @param applicationId 申请ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleLetterApplicationApproval(
      final Long applicationId, final String result, final String comment) {
    if (ApprovalStatus.APPROVED.equals(result)) {
      letterAppService.onApprovalApproved(applicationId, comment);
    } else if (ApprovalStatus.REJECTED.equals(result)) {
      letterAppService.onApprovalRejected(applicationId, comment);
    }
  }

  /**
   * 处理合同审批.
   *
   * @param contractId 合同ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleContractApproval(
      final Long contractId, final String result, final String comment) {
    try {
      if (ApprovalStatus.APPROVED.equals(result)) {
        contractAppService.approve(contractId);
      } else if (ApprovalStatus.REJECTED.equals(result)) {
        contractAppService.reject(contractId, comment);
      }
    } catch (BusinessException e) {
      // 如果状态检查失败（如状态不是PENDING），记录警告但不抛出异常
      // 这可能是因为合同已经从其他地方审批了
      log.warn(
          "合同审批业务状态更新失败（可能已从其他地方审批）: contractId={}, result={}, error={}",
          contractId,
          result,
          e.getMessage());
    }
  }

  /**
   * 处理用印申请审批.
   *
   * @param applicationId 申请ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleSealApplicationApproval(
      final Long applicationId, final String result, final String comment) {
    try {
      if (ApprovalStatus.APPROVED.equals(result)) {
        sealApplicationAppService.approve(applicationId, comment);
      } else if (ApprovalStatus.REJECTED.equals(result)) {
        sealApplicationAppService.reject(applicationId, comment);
      }
    } catch (BusinessException e) {
      // 如果状态检查失败（如状态不是PENDING），记录警告但不抛出异常
      log.warn(
          "用印申请审批业务状态更新失败（可能已从其他地方审批）: applicationId={}, result={}, error={}",
          applicationId,
          result,
          e.getMessage());
    }
  }

  /**
   * 处理利冲检查审批.
   *
   * @param checkId 检查ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleConflictCheckApproval(
      final Long checkId, final String result, final String comment) {
    try {
      if (ApprovalStatus.APPROVED.equals(result)) {
        conflictCheckAppService.approve(checkId, comment);
      } else if (ApprovalStatus.REJECTED.equals(result)) {
        conflictCheckAppService.reject(checkId, comment);
      }
    } catch (BusinessException e) {
      // 如果状态检查失败，记录警告但不抛出异常
      log.warn(
          "利冲检查审批业务状态更新失败（可能已从其他地方审批）: checkId={}, result={}, error={}",
          checkId,
          result,
          e.getMessage());
    }
  }

  /**
   * 处理利益冲突豁免审批.
   *
   * @param checkId 检查ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleConflictExemptionApproval(
      final Long checkId, final String result, final String comment) {
    try {
      if (ApprovalStatus.APPROVED.equals(result)) {
        conflictCheckAppService.approveExemption(checkId, comment);
      } else if (ApprovalStatus.REJECTED.equals(result)) {
        conflictCheckAppService.rejectExemption(checkId, comment);
      }
    } catch (BusinessException e) {
      // 如果状态检查失败，记录警告但不抛出异常
      log.warn(
          "利冲豁免审批业务状态更新失败（可能已从其他地方审批）: checkId={}, result={}, error={}",
          checkId,
          result,
          e.getMessage());
    }
  }

  /**
   * 处理数据交接审批.
   *
   * @param handoverId 交接ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleDataHandoverApproval(
      final Long handoverId, final String result, final String comment) {
    if (ApprovalStatus.APPROVED.equals(result)) {
      dataHandoverService.onApprovalApproved(handoverId);
    } else if (ApprovalStatus.REJECTED.equals(result)) {
      dataHandoverService.onApprovalRejected(handoverId, comment);
    }
  }

  /**
   * 处理档案入库审批.
   *
   * @param archiveId 档案ID
   * @param result 审批结果
   * @param comment 审批意见
   */
  private void handleArchiveStoreApproval(
      final Long archiveId, final String result, final String comment) {
    try {
      if (ApprovalStatus.APPROVED.equals(result)) {
        archiveAppService.onStoreApprovalApproved(archiveId, comment);
      } else if (ApprovalStatus.REJECTED.equals(result)) {
        archiveAppService.onStoreApprovalRejected(archiveId, comment);
      }
    } catch (Exception e) {
      log.warn(
          "档案入库审批业务状态更新失败: archiveId={}, result={}, error={}", archiveId, result, e.getMessage());
    }
  }
}
