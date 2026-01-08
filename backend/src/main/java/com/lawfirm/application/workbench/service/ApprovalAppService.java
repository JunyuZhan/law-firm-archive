package com.lawfirm.application.workbench.service;

import com.lawfirm.application.workbench.command.ApproveCommand;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.dto.ApprovalQueryDTO;
import com.lawfirm.application.workbench.event.ApprovalCompletedEvent;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalAppService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalMapper approvalMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询审批记录
     * 数据权限过滤：
     * - ALL: 可查看所有审批
     * - DEPT_AND_CHILD: 可查看本部门及下级部门的审批
     * - DEPT: 可查看本部门的审批
     * - SELF: 只能查看自己发起或需要自己审批的记录
     */
    public PageResult<ApprovalDTO> listApprovals(ApprovalQueryDTO query) {
        try {
            List<Approval> approvals = approvalMapper.selectApprovalPage(
                    query.getStatus(),
                    query.getBusinessType(),
                    query.getApplicantId(),
                    query.getApproverId()
            );

            // 应用数据权限过滤
            approvals = applyDataScopeFilter(approvals);

            // 分页处理
            int total = approvals.size();
            int start = (query.getPageNum() - 1) * query.getPageSize();
            int end = Math.min(start + query.getPageSize(), total);
            List<Approval> pagedList = total > 0 ? approvals.subList(Math.max(0, start), end) : new ArrayList<>();

            List<ApprovalDTO> dtos = pagedList.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtos, total, query.getPageNum(), query.getPageSize());
        } catch (Exception e) {
            log.error("查询审批记录失败", e);
            // 返回空结果，避免500错误
            return PageResult.of(new ArrayList<>(), 0, query.getPageNum(), query.getPageSize());
        }
    }
    
    /**
     * 应用数据权限过滤
     * 审批中心的数据权限逻辑：
     * - 管理员/主任：可查看全部审批记录（用于统计和管理）
     * - 其他角色（包括财务）：只能看到自己发起的 + 需要自己审批的
     * 
     * 注意：财务虽然有 ALL 数据权限（用于财务数据），但审批中心只展示与自己相关的审批
     */
    private List<Approval> applyDataScopeFilter(List<Approval> approvals) {
        Long currentUserId = SecurityUtils.getUserId();
        
        // 只有管理员和主任可以看全部审批（真正的管理层）
        if (isAdminOrDirector()) {
            return approvals;
        }
        
        // 其他角色（包括财务、团队负责人、律师等）：只能看到自己相关的审批
        return approvals.stream()
                .filter(a -> currentUserId.equals(a.getApplicantId()) || 
                            currentUserId.equals(a.getApproverId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 判断当前用户是否为管理员或主任
     */
    private boolean isAdminOrDirector() {
        java.util.Set<String> roles = SecurityUtils.getRoles();
        return roles != null && (roles.contains("ADMIN") || roles.contains("DIRECTOR"));
    }

    /**
     * 获取待审批列表
     */
    public List<ApprovalDTO> getPendingApprovals() {
        Long currentUserId = SecurityUtils.getUserId();
        List<Approval> approvals = approvalMapper.selectPendingApprovals(currentUserId);
        return approvals.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取我发起的审批
     */
    public List<ApprovalDTO> getMyInitiatedApprovals() {
        Long currentUserId = SecurityUtils.getUserId();
        List<Approval> approvals = approvalMapper.selectMyInitiatedApprovals(currentUserId);
        return approvals.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取审批历史（我处理过的审批记录）
     * 审批中心的数据权限逻辑：
     * - 管理员/主任：可查看全部已完成审批（用于管理和统计）
     * - 其他角色（包括财务）：只能查看自己发起或自己审批过的记录
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
     * 获取部门及所有下级部门ID
     */
    private List<Long> getAllChildDepartmentIds(Long parentId) {
        List<Long> result = new java.util.ArrayList<>();
        collectChildDeptIds(parentId, result);
        return result;
    }
    
    private void collectChildDeptIds(Long parentId, List<Long> result) {
        try {
            List<Long> childIds = approvalMapper.selectChildDeptIds(parentId);
            if (childIds != null && !childIds.isEmpty()) {
                result.addAll(childIds);
                for (Long childId : childIds) {
                    collectChildDeptIds(childId, result);
                }
            }
        } catch (Exception e) {
            log.warn("查询子部门失败: parentId={}", parentId, e);
        }
    }

    /**
     * 获取审批详情
     * 权限检查：只有申请人、审批人、或管理员/主任可以查看
     */
    public ApprovalDTO getApprovalById(Long id) {
        Approval approval = approvalRepository.getByIdOrThrow(id, "审批记录不存在");
        
        // 数据权限检查
        Long currentUserId = SecurityUtils.getUserId();
        
        // 管理员/主任可查看所有
        if (isAdminOrDirector()) {
            return toDTO(approval);
        }
        
        // 申请人或审批人可查看（审批中心的核心逻辑）
        if (currentUserId.equals(approval.getApplicantId()) || 
            currentUserId.equals(approval.getApproverId())) {
            return toDTO(approval);
        }
        
        throw new BusinessException("无权查看此审批记录");
    }

    /**
     * 审批操作（通过/拒绝）
     */
    @Transactional
    public void approve(ApproveCommand command) {
        Approval approval = approvalRepository.getByIdOrThrow(command.getApprovalId(), "审批记录不存在");

        // 检查权限
        Long currentUserId = SecurityUtils.getUserId();
        if (!approval.getApproverId().equals(currentUserId)) {
            throw new BusinessException("无权审批此记录");
        }

        // 检查状态
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BusinessException("该审批记录已处理，无法重复审批");
        }

        // 拒绝时必须填写拒绝事由
        if ("REJECTED".equals(command.getResult())) {
            if (command.getComment() == null || command.getComment().trim().isEmpty()) {
                throw new BusinessException("拒绝时必须填写拒绝事由");
            }
        }

        // 更新审批状态
        approval.setStatus("APPROVED".equals(command.getResult()) ? "APPROVED" : "REJECTED");
        approval.setComment(command.getComment());
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.updateById(approval);

        log.info("审批完成: approvalNo={}, result={}, approver={}", 
                approval.getApprovalNo(), command.getResult(), currentUserId);

        // 发布审批完成事件，通知业务模块更新状态
        eventPublisher.publishEvent(new ApprovalCompletedEvent(
                this,
                approval.getId(),
                approval.getBusinessType(),
                approval.getBusinessId(),
                command.getResult(),
                command.getComment()
        ));
    }

    /**
     * 批量审批
     */
    @Transactional
    public BatchApproveResult batchApprove(List<Long> approvalIds, String result, String comment) {
        Long currentUserId = SecurityUtils.getUserId();
        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new java.util.ArrayList<>();
        
        for (Long approvalId : approvalIds) {
            try {
                Approval approval = approvalRepository.findById(approvalId);
                if (approval == null) {
                    errors.add("审批记录不存在: " + approvalId);
                    skipCount++;
                    continue;
                }
                
                if (!approval.getApproverId().equals(currentUserId)) {
                    errors.add("无权审批此记录: " + approval.getApprovalNo());
                    skipCount++;
                    continue;
                }
                
                if (!"PENDING".equals(approval.getStatus())) {
                    errors.add("该审批记录已处理: " + approval.getApprovalNo());
                    skipCount++;
                    continue;
                }
                
                // 更新审批状态
                approval.setStatus("APPROVED".equals(result) ? "APPROVED" : "REJECTED");
                approval.setComment(comment);
                approval.setApprovedAt(LocalDateTime.now());
                approvalRepository.updateById(approval);
                
                // 发布审批完成事件，通知业务模块更新状态
                eventPublisher.publishEvent(new ApprovalCompletedEvent(
                        this,
                        approval.getId(),
                        approval.getBusinessType(),
                        approval.getBusinessId(),
                        result,
                        comment
                ));
                
                successCount++;
            } catch (Exception e) {
                log.error("批量审批处理失败: approvalId={}", approvalId, e);
                errors.add("处理失败: " + approvalId + " - " + e.getMessage());
                skipCount++;
            }
        }
        
        log.info("批量审批完成: 总数={}, 成功={}, 跳过={}, result={}", 
                approvalIds.size(), successCount, skipCount, result);
        
        return BatchApproveResult.builder()
                .total(approvalIds.size())
                .successCount(successCount)
                .skipCount(skipCount)
                .errors(errors)
                .build();
    }
    
    /**
     * 批量审批结果
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchApproveResult {
        private int total;
        private int successCount;
        private int skipCount;
        private List<String> errors;
    }

    /**
     * 获取业务审批记录
     */
    public List<ApprovalDTO> getBusinessApprovals(String businessType, Long businessId) {
        List<Approval> approvals = approvalMapper.selectByBusiness(businessType, businessId);
        return approvals.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Entity 转 DTO
     */
    private ApprovalDTO toDTO(Approval approval) {
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

    private String getBusinessTypeName(String type) {
        if (type == null) return null;
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

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getPriorityName(String priority) {
        if (priority == null) return null;
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }

    private String getUrgencyName(String urgency) {
        if (urgency == null) return null;
        return switch (urgency) {
            case "URGENT" -> "紧急";
            case "NORMAL" -> "普通";
            default -> urgency;
        };
    }
}

