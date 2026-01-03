package com.lawfirm.application.workbench.listener;

import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.application.document.service.SealApplicationAppService;
import com.lawfirm.application.client.service.ConflictCheckAppService;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批事件监听器
 * 处理审批完成后的业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final ContractAppService contractAppService;
    private final SealApplicationAppService sealApplicationAppService;
    private final ConflictCheckAppService conflictCheckAppService;

    /**
     * 监听审批完成事件
     */
    @Async
    @EventListener
    @Transactional
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        String businessType = event.getBusinessType();
        Long businessId = event.getBusinessId();
        String result = event.getResult();
        String comment = event.getComment();

        log.info("处理审批完成事件: businessType={}, businessId={}, result={}", 
                businessType, businessId, result);

        try {
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

