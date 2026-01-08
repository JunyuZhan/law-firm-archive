package com.lawfirm.application.finance.service;

import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Expense;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.ExpenseRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 律师视角的财务服务
 * 提供"我的收款"、"我的提成"等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyFinanceService {

    private final ContractRepository contractRepository;
    private final ContractParticipantRepository participantRepository;
    private final FeeRepository feeRepository;
    private final CommissionRepository commissionRepository;
    private final ClientRepository clientRepository;
    private final ExpenseRepository expenseRepository;
    private final MatterRepository matterRepository;

    /**
     * 获取当前用户参与的合同收款情况
     */
    public List<Map<String, Object>> getMyContractPayments() {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null) {
                log.warn("无法获取当前用户ID");
                return new ArrayList<>();
            }
            
            // 查询当前用户参与的合同
            List<ContractParticipant> myParticipations = participantRepository.findByUserId(currentUserId);
            if (myParticipations == null || myParticipations.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (ContractParticipant participation : myParticipations) {
                try {
                    if (participation == null || participation.getContractId() == null) {
                        continue;
                    }
                    
                    Contract contract = contractRepository.findById(participation.getContractId());
                    if (contract == null || (contract.getDeleted() != null && contract.getDeleted()) || !"ACTIVE".equals(contract.getStatus())) {
                        continue;
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("contractId", contract.getId());
                    item.put("contractNo", contract.getContractNo() != null ? contract.getContractNo() : "-");
                    item.put("contractName", contract.getName() != null ? contract.getName() : "-");
                    
                    // 客户名称
                    if (contract.getClientId() != null) {
                        try {
                            Client client = clientRepository.findById(contract.getClientId());
                            item.put("clientName", client != null && (client.getDeleted() == null || !client.getDeleted()) ? client.getName() : "-");
                        } catch (Exception e) {
                            log.warn("获取客户信息失败, clientId: {}", contract.getClientId(), e);
                            item.put("clientName", "-");
                        }
                    } else {
                        item.put("clientName", "-");
                    }
                    
                    // 金额信息
                    BigDecimal totalAmount = contract.getTotalAmount() != null ? contract.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal paidAmount = contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO;
                    BigDecimal unpaidAmount = totalAmount.subtract(paidAmount);
                    
                    item.put("totalAmount", totalAmount);
                    item.put("paidAmount", paidAmount);
                    item.put("unpaidAmount", unpaidAmount);
                    
                    // 收款进度
                    int progress = 0;
                    if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                        try {
                            progress = paidAmount.multiply(new BigDecimal("100"))
                                    .divide(totalAmount, 0, RoundingMode.HALF_UP)
                                    .intValue();
                        } catch (Exception e) {
                            log.warn("计算收款进度失败, contractId: {}", contract.getId(), e);
                            progress = 0;
                        }
                    }
                    item.put("paymentProgress", Math.min(progress, 100));
                    
                    // 我的角色
                    item.put("myRole", participation.getRole());
                    item.put("myRoleName", getRoleName(participation.getRole()));
                    
                    // 最近收款日期
                    try {
                        Fee lastFee = feeRepository.findLastByContractId(contract.getId());
                        item.put("lastPaymentDate", lastFee != null ? lastFee.getActualDate() : null);
                    } catch (Exception e) {
                        log.warn("获取最近收款日期失败, contractId: {}", contract.getId(), e);
                        item.put("lastPaymentDate", null);
                    }
                    
                    result.add(item);
                } catch (Exception e) {
                    log.error("处理合同参与记录失败, participationId: {}", participation != null ? participation.getId() : "null", e);
                    // 继续处理下一条记录
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取我的收款记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取当前用户的提成记录
     */
    public List<Map<String, Object>> getMyCommissions(String status) {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null) {
                log.warn("无法获取当前用户ID");
                return new ArrayList<>();
            }
            
            List<Commission> commissions;
            try {
                if (status != null && !status.isEmpty()) {
                    commissions = commissionRepository.findByUserIdAndStatus(currentUserId, status);
                } else {
                    commissions = commissionRepository.findByUserId(currentUserId);
                }
            } catch (Exception e) {
                log.error("查询提成记录失败, userId: {}, status: {}", currentUserId, status, e);
                return new ArrayList<>();
            }
            
            if (commissions == null || commissions.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Commission commission : commissions) {
                try {
                    if (commission == null || (commission.getDeleted() != null && commission.getDeleted())) {
                        continue;
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", commission.getId());
                    
                    // 合同信息
                    if (commission.getContractId() != null) {
                        try {
                            Contract contract = contractRepository.findById(commission.getContractId());
                            if (contract != null && (contract.getDeleted() == null || !contract.getDeleted())) {
                                item.put("contractNo", contract.getContractNo() != null ? contract.getContractNo() : "-");
                                item.put("contractName", contract.getName() != null ? contract.getName() : "-");
                                
                                if (contract.getClientId() != null) {
                                    try {
                                        Client client = clientRepository.findById(contract.getClientId());
                                        item.put("clientName", client != null && (client.getDeleted() == null || !client.getDeleted()) ? client.getName() : "-");
                                    } catch (Exception e) {
                                        log.warn("获取客户信息失败, clientId: {}", contract.getClientId(), e);
                                        item.put("clientName", "-");
                                    }
                                } else {
                                    item.put("clientName", "-");
                                }
                            } else {
                                item.put("contractNo", "-");
                                item.put("contractName", "-");
                                item.put("clientName", "-");
                            }
                        } catch (Exception e) {
                            log.warn("获取合同信息失败, contractId: {}", commission.getContractId(), e);
                            item.put("contractNo", "-");
                            item.put("contractName", "-");
                            item.put("clientName", "-");
                        }
                    } else {
                        item.put("contractNo", "-");
                        item.put("contractName", "-");
                        item.put("clientName", "-");
                    }
                    
                    // 提成信息
                    item.put("paymentAmount", commission.getGrossAmount() != null ? commission.getGrossAmount() : BigDecimal.ZERO);
                    item.put("commissionRate", commission.getCommissionRate() != null ? commission.getCommissionRate() : BigDecimal.ZERO);
                    item.put("commissionAmount", commission.getCommissionAmount() != null ? commission.getCommissionAmount() : BigDecimal.ZERO);
                    item.put("status", commission.getStatus());
                    item.put("statusName", getCommissionStatusName(commission.getStatus()));
                    item.put("calculatedAt", commission.getCreatedAt());
                    item.put("paidAt", commission.getPaidAt());
                    item.put("remark", commission.getRemark());
                    
                    result.add(item);
                } catch (Exception e) {
                    log.error("处理提成记录失败, commissionId: {}", commission != null ? commission.getId() : "null", e);
                    // 继续处理下一条记录
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取我的提成记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取角色名称
     */
    private String getRoleName(String role) {
        if (role == null) return "-";
        return switch (role) {
            case "LEAD" -> "承办律师";
            case "CO_COUNSEL" -> "协办律师";
            case "ORIGINATOR" -> "案源人";
            case "PARALEGAL" -> "律师助理";
            default -> role;
        };
    }

    /**
     * 获取提成状态名称
     */
    private String getCommissionStatusName(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "PENDING" -> "待发放";
            case "PAID" -> "已发放";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 获取当前用户的费用报销记录
     */
    public List<Map<String, Object>> getMyExpenses() {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId == null) {
                log.warn("无法获取当前用户ID");
                return new ArrayList<>();
            }
            
            List<Expense> expenses = expenseRepository.findByApplicantId(currentUserId);
            if (expenses == null || expenses.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Expense expense : expenses) {
                try {
                    if (expense == null || (expense.getDeleted() != null && expense.getDeleted())) {
                        continue;
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", expense.getId());
                    item.put("expenseNo", expense.getExpenseNo() != null ? expense.getExpenseNo() : "-");
                    item.put("expenseType", expense.getExpenseType());
                    item.put("expenseCategory", expense.getExpenseCategory());
                    item.put("amount", expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO);
                    item.put("currency", expense.getCurrency() != null ? expense.getCurrency() : "CNY");
                    item.put("expenseDate", expense.getExpenseDate());
                    item.put("description", expense.getDescription());
                    item.put("status", expense.getStatus());
                    item.put("statusName", getExpenseStatusName(expense.getStatus()));
                    item.put("matterId", expense.getMatterId());
                    item.put("createdAt", expense.getCreatedAt());
                    item.put("updatedAt", expense.getUpdatedAt());
                    
                    // 项目名称
                    if (expense.getMatterId() != null) {
                        try {
                            Matter matter = matterRepository.findById(expense.getMatterId());
                            if (matter != null && (matter.getDeleted() == null || !matter.getDeleted())) {
                                item.put("matterName", matter.getName() != null ? matter.getName() : "-");
                            } else {
                                item.put("matterName", "-");
                            }
                        } catch (Exception e) {
                            log.warn("获取项目信息失败, matterId: {}", expense.getMatterId(), e);
                            item.put("matterName", "-");
                        }
                    } else {
                        item.put("matterName", "-");
                    }
                    
                    result.add(item);
                } catch (Exception e) {
                    log.error("处理费用报销记录失败, expenseId: {}", expense != null ? expense.getId() : "null", e);
                    // 继续处理下一条记录
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取我的费用报销记录失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取费用报销状态名称
     */
    private String getExpenseStatusName(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "PAID" -> "已支付";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
