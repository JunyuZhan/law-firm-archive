package com.lawfirm.application.workbench.service;

import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 审批服务工具类
 * 用于各业务模块创建审批记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final NotificationAppService notificationAppService;

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
    public Long createApproval(String businessType, Long businessId, String businessNo, 
                              String businessTitle, Long approverId, String priority, 
                              String urgency, String businessSnapshot) {
        Long applicantId = SecurityUtils.getUserId();
        String applicantName = SecurityUtils.getRealName();
        
        // 获取审批人姓名
        User approver = userRepository.getById(approverId);
        if (approver == null) {
            throw new BusinessException("审批人不存在");
        }
        String approverName = approver.getRealName();
        
        Approval approval = Approval.builder()
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
        log.info("创建审批记录: approvalNo={}, businessType={}, businessId={}, approver={}", 
                approval.getApprovalNo(), businessType, businessId, approverId);
        
        // 发送通知给审批人
        try {
            String businessTypeName = getBusinessTypeName(businessType);
            String title = "您有新的" + businessTypeName + "待审批";
            String content = String.format("%s 提交了【%s】，请您审批。", 
                    applicantName, 
                    businessTitle != null ? businessTitle : businessTypeName);
            notificationAppService.sendSystemNotification(
                    approverId,
                    title,
                    content,
                    "APPROVAL",
                    approval.getId()
            );
            log.info("已发送审批通知给: {}", approverName);
        } catch (Exception e) {
            log.warn("发送审批通知失败: approverId={}", approverId, e);
        }
        
        return approval.getId();
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
            case "REGULARIZATION" -> "转正申请";
            case "RESIGNATION" -> "离职申请";
            default -> "审批";
        };
    }

    /**
     * 创建审批记录（简化版，使用默认优先级和紧急程度）
     */
    @Transactional
    public Long createApproval(String businessType, Long businessId, String businessNo, 
                              String businessTitle, Long approverId) {
        return createApproval(businessType, businessId, businessNo, businessTitle, 
                             approverId, "MEDIUM", "NORMAL", null);
    }

    /**
     * 审批完成后，更新业务模块状态
     * 此方法由业务模块在审批回调中调用
     */
    @Transactional
    public void onApprovalCompleted(Long approvalId, String result) {
        Approval approval = approvalRepository.getByIdOrThrow(approvalId, "审批记录不存在");
        
        // 根据业务类型，调用相应的业务模块服务更新状态
        String businessType = approval.getBusinessType();
        Long businessId = approval.getBusinessId();
        
        // TODO: 通过事件机制或服务注入的方式，通知业务模块更新状态
        // 这里先记录日志，后续完善
        log.info("审批完成回调: approvalId={}, businessType={}, businessId={}, result={}", 
                approvalId, businessType, businessId, result);
    }

    /**
     * 生成审批编号
     */
    private String generateApprovalNo() {
        return "APP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

