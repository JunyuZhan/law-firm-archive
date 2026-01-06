package com.lawfirm.application.workbench.listener;

import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.application.document.service.SealApplicationAppService;
import com.lawfirm.application.client.service.ConflictCheckAppService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批事件监听器
 * 处理审批完成后的业务逻辑
 * 
 * 注意：不使用 @Async，确保审批状态更新在同一事务中完成
 * 如果需要异步处理，应该在事务提交后使用 @TransactionalEventListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final ContractAppService contractAppService;
    private final SealApplicationAppService sealApplicationAppService;
    private final ConflictCheckAppService conflictCheckAppService;
    private final NotificationAppService notificationAppService;
    private final ApprovalRepository approvalRepository;

    /**
     * 监听审批完成事件
     * 同步处理，确保业务状态更新与审批状态更新在同一事务中
     */
    @EventListener
    @Transactional
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        String businessType = event.getBusinessType();
        Long businessId = event.getBusinessId();
        String result = event.getResult();
        String comment = event.getComment();
        Long approvalId = event.getApprovalId();

        log.info("处理审批完成事件: businessType={}, businessId={}, result={}", 
                businessType, businessId, result);

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
                default:
                    log.warn("未知的业务类型: {}", businessType);
            }
        } catch (Exception e) {
            log.error("处理审批完成事件失败: businessType={}, businessId={}", 
                    businessType, businessId, e);
        }
    }
    
    /**
     * 发送审批结果通知给申请人
     */
    private void sendApprovalResultNotification(Long approvalId, String result, String comment) {
        try {
            Approval approval = approvalRepository.findById(approvalId);
            if (approval == null || approval.getApplicantId() == null) {
                return;
            }
            
            String businessTypeName = getBusinessTypeName(approval.getBusinessType());
            String resultText = "APPROVED".equals(result) ? "已通过" : "已拒绝";
            String title = businessTypeName + "审批" + resultText;
            String content = String.format("您提交的【%s】%s。", 
                    approval.getBusinessTitle() != null ? approval.getBusinessTitle() : businessTypeName,
                    resultText);
            if (comment != null && !comment.isEmpty()) {
                content += " 审批意见：" + comment;
            }
            
            notificationAppService.sendSystemNotification(
                    approval.getApplicantId(),
                    title,
                    content,
                    "APPROVAL",
                    approvalId
            );
            
            log.info("审批结果通知已发送: approvalId={}, applicantId={}, result={}", 
                    approvalId, approval.getApplicantId(), result);
        } catch (Exception e) {
            log.warn("发送审批结果通知失败: approvalId={}", approvalId, e);
        }
    }
    
    /**
     * 获取业务类型名称
     */
    private String getBusinessTypeName(String businessType) {
        if (businessType == null) return "审批";
        return switch (businessType) {
            case "CONTRACT" -> "合同";
            case "SEAL_APPLICATION" -> "用印申请";
            case "CONFLICT_CHECK" -> "利冲检查";
            case "CONFLICT_EXEMPTION" -> "利冲豁免";
            case "EXPENSE" -> "费用报销";
            case "PAYMENT_AMENDMENT" -> "收款变更";
            case "MATTER_CLOSE" -> "项目结案";
            default -> "审批";
        };
    }

    /**
     * 处理合同审批
     */
    private void handleContractApproval(Long contractId, String result, String comment) {
        if ("APPROVED".equals(result)) {
            contractAppService.approve(contractId);
        } else if ("REJECTED".equals(result)) {
            contractAppService.reject(contractId, comment);
        }
    }

    /**
     * 处理用印申请审批
     */
    private void handleSealApplicationApproval(Long applicationId, String result, String comment) {
        if ("APPROVED".equals(result)) {
            sealApplicationAppService.approve(applicationId, comment);
        } else if ("REJECTED".equals(result)) {
            sealApplicationAppService.reject(applicationId, comment);
        }
    }

    /**
     * 处理利冲检查审批
     */
    private void handleConflictCheckApproval(Long checkId, String result, String comment) {
        if ("APPROVED".equals(result)) {
            conflictCheckAppService.approve(checkId, comment);
        } else if ("REJECTED".equals(result)) {
            conflictCheckAppService.reject(checkId, comment);
        }
    }

    /**
     * 处理利益冲突豁免审批
     */
    private void handleConflictExemptionApproval(Long checkId, String result, String comment) {
        if ("APPROVED".equals(result)) {
            conflictCheckAppService.approveExemption(checkId, comment);
        } else if ("REJECTED".equals(result)) {
            conflictCheckAppService.rejectExemption(checkId, comment);
        }
    }
}

