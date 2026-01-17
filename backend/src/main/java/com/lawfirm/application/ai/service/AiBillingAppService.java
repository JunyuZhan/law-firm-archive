package com.lawfirm.application.ai.service;

import com.lawfirm.application.ai.command.SalaryDeductionLinkCommand;
import com.lawfirm.application.ai.dto.AiMonthlyBillDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiMonthlyBill;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiMonthlyBillRepository;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI账单应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBillingAppService {

    private final AiMonthlyBillRepository billRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final AiUserQuotaRepository quotaRepository;
    private final UserRepository userRepository;

    /**
     * 获取指定月份的用户账单列表
     */
    public List<AiMonthlyBillDTO> getMonthlyBills(Integer year, Integer month) {
        // 检查权限：只有管理员、财务可以查看
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
            throw new BusinessException("无权查看AI费用账单");
        }

        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        List<AiMonthlyBill> bills = billRepository.findByPeriod(year, month);
        return bills.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定用户的账单
     */
    public AiMonthlyBillDTO getUserBill(Long userId, Integer year, Integer month) {
        // 普通用户只能查看自己的账单
        Long currentUserId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")
                && !currentUserId.equals(userId)) {
            throw new BusinessException("无权查看他人账单");
        }

        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        AiMonthlyBill bill = billRepository.findByUserAndPeriod(userId, year, month);
        if (bill == null) {
            return null;
        }
        return toDTO(bill);
    }

    /**
     * 生成月度账单
     */
    @Transactional
    public int generateMonthlyBills(Integer year, Integer month) {
        // 检查权限
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE")) {
            throw new BusinessException("无权生成AI费用账单");
        }

        if (year == null) {
            year = LocalDate.now().minusMonths(1).getYear();
        }
        if (month == null) {
            month = LocalDate.now().minusMonths(1).getMonthValue();
        }

        log.info("开始生成{}年{}月AI费用账单", year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // 获取该月有使用记录的所有用户
        List<Map<String, Object>> userUsageList = usageLogRepository.getActiveUsersInPeriod(startDate, endDate);

        int count = 0;
        for (Map<String, Object> userUsage : userUsageList) {
            Long userId = ((Number) userUsage.get("user_id")).longValue();

            try {
                generateUserBill(userId, year, month);
                count++;
            } catch (Exception e) {
                log.error("生成用户{}的账单失败", userId, e);
            }
        }

        log.info("生成{}年{}月AI费用账单完成，共生成{}份账单", year, month, count);
        return count;
    }

    /**
     * 生成单个用户的月度账单
     */
    @Transactional
    public AiMonthlyBillDTO generateUserBill(Long userId, Integer year, Integer month) {
        // 检查是否已存在账单
        AiMonthlyBill existingBill = billRepository.findByUserAndPeriod(userId, year, month);
        if (existingBill != null) {
            log.info("用户{}的{}年{}月账单已存在", userId, year, month);
            return toDTO(existingBill);
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // 获取用户该月的使用统计
        Map<String, Object> stats = usageLogRepository.getSummary(userId, startDate, endDate);

        // 获取用户信息
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在: " + userId);
        }

        // 获取收费比例
        Integer chargeRatio = getChargeRatio(userId);

        // 计算用户应付费用
        BigDecimal totalCost = stats.get("total_cost") != null ? (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO;
        BigDecimal userCost = calculateUserCost(totalCost, chargeRatio);

        // 创建账单
        AiMonthlyBill bill = AiMonthlyBill.builder()
                .billYear(year)
                .billMonth(month)
                .userId(userId)
                .userName(user.getRealName())
                .departmentId(user.getDepartmentId())
                .totalCalls(stats.get("total_calls") != null ? ((Number) stats.get("total_calls")).intValue() : 0)
                .totalTokens(stats.get("total_tokens") != null ? ((Number) stats.get("total_tokens")).longValue() : 0L)
                .promptTokens(stats.get("prompt_tokens") != null ? ((Number) stats.get("prompt_tokens")).longValue() : 0L)
                .completionTokens(stats.get("completion_tokens") != null ? ((Number) stats.get("completion_tokens")).longValue() : 0L)
                .totalCost(totalCost)
                .userCost(userCost)
                .chargeRatio(chargeRatio)
                .deductionStatus("PENDING")
                .createdBy(SecurityUtils.getUserId())
                .build();

        billRepository.save(bill);
        log.info("生成用户{}的{}年{}月账单成功，费用: {}元", userId, year, month, userCost);

        return toDTO(bill);
    }

    /**
     * 标记账单已扣减
     */
    @Transactional
    public void markDeducted(Long billId, String remark) {
        // 检查权限
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE")) {
            throw new BusinessException("无权操作账单");
        }

        AiMonthlyBill bill = billRepository.getByIdOrThrow(billId, "账单不存在");

        if ("DEDUCTED".equals(bill.getDeductionStatus()) || "WAIVED".equals(bill.getDeductionStatus())) {
            throw new BusinessException("账单已处理，无法重复操作");
        }

        bill.setDeductionStatus("DEDUCTED");
        bill.setDeductionAmount(bill.getUserCost());
        bill.setDeductedAt(java.time.LocalDateTime.now());
        bill.setDeductedBy(SecurityUtils.getUserId());
        bill.setDeductionRemark(remark);

        billRepository.updateById(bill);
        log.info("标记账单已扣减: billId={}, amount={}", billId, bill.getUserCost());
    }

    /**
     * 减免账单
     */
    @Transactional
    public void waiveBill(Long billId, String reason) {
        // 检查权限
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
            throw new BusinessException("无权减免账单");
        }

        AiMonthlyBill bill = billRepository.getByIdOrThrow(billId, "账单不存在");

        if ("DEDUCTED".equals(bill.getDeductionStatus()) || "WAIVED".equals(bill.getDeductionStatus())) {
            throw new BusinessException("账单已处理，无法重复操作");
        }

        bill.setDeductionStatus("WAIVED");
        bill.setDeductionAmount(BigDecimal.ZERO);
        bill.setDeductedAt(java.time.LocalDateTime.now());
        bill.setDeductedBy(SecurityUtils.getUserId());
        bill.setDeductionRemark(reason);

        billRepository.updateById(bill);
        log.info("减免账单: billId={}, originalAmount={}", billId, bill.getUserCost());
    }

    /**
     * 关联工资扣减记录
     */
    @Transactional
    public void linkToSalaryDeduction(SalaryDeductionLinkCommand command) {
        // 检查权限
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE")) {
            throw new BusinessException("无权关联工资扣减");
        }

        for (Long billId : command.getBillIds()) {
            AiMonthlyBill bill = billRepository.getByIdOrThrow(billId, "账单不存在");

            if (!"PENDING".equals(bill.getDeductionStatus())) {
                log.warn("账单{}状态不是待扣减，跳过关联", billId);
                continue;
            }

            bill.setSalaryDeductionId(command.getSalarySheetId());
            bill.setDeductionStatus("DEDUCTED");
            bill.setDeductionAmount(bill.getUserCost());
            bill.setDeductedAt(java.time.LocalDateTime.now());
            bill.setDeductedBy(SecurityUtils.getUserId());
            bill.setDeductionRemark(command.getRemark());

            billRepository.updateById(bill);
            log.info("关联账单到工资扣减: billId={}, salarySheetId={}", billId, command.getSalarySheetId());
        }
    }

    /**
     * 获取收费比例
     */
    private Integer getChargeRatio(Long userId) {
        // 1. 检查用户是否在免费名单
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota != null && Boolean.TRUE.equals(quota.getExemptBilling())) {
            return 0; // 免计费用户
        }

        // 2. 读取管理员配置的收费比例（从系统配置读取）
        // TODO: 从sys_config表读取ai.billing.charge_ratio配置
        return 100; // 默认100%
    }

    /**
     * 计算用户应付费用
     */
    private BigDecimal calculateUserCost(BigDecimal totalCost, int chargeRatio) {
        if (chargeRatio <= 0) {
            return BigDecimal.ZERO;
        }
        if (chargeRatio >= 100) {
            return totalCost;
        }

        return totalCost.multiply(BigDecimal.valueOf(chargeRatio))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /**
     * Entity转DTO
     */
    private AiMonthlyBillDTO toDTO(AiMonthlyBill bill) {
        AiMonthlyBillDTO dto = new AiMonthlyBillDTO();
        dto.setId(bill.getId());
        dto.setBillYear(bill.getBillYear());
        dto.setBillMonth(bill.getBillMonth());
        dto.setBillMonthDisplay(bill.getBillYear() + "-" + String.format("%02d", bill.getBillMonth()));
        dto.setUserId(bill.getUserId());
        dto.setUserName(bill.getUserName());
        dto.setDepartmentId(bill.getDepartmentId());
        dto.setTotalCalls(bill.getTotalCalls());
        dto.setTotalTokens(bill.getTotalTokens());
        dto.setPromptTokens(bill.getPromptTokens());
        dto.setCompletionTokens(bill.getCompletionTokens());
        dto.setTotalCost(bill.getTotalCost());
        dto.setUserCost(bill.getUserCost());
        dto.setChargeRatio(bill.getChargeRatio());
        dto.setDeductionStatus(bill.getDeductionStatus());
        dto.setDeductionStatusName(getDeductionStatusName(bill.getDeductionStatus()));
        dto.setDeductionAmount(bill.getDeductionAmount());
        dto.setDeductedAt(bill.getDeductedAt());
        dto.setDeductedBy(bill.getDeductedBy());
        dto.setDeductionRemark(bill.getDeductionRemark());
        dto.setSalaryDeductionId(bill.getSalaryDeductionId());
        dto.setCreatedAt(bill.getCreatedAt());
        dto.setUpdatedAt(bill.getUpdatedAt());

        // 查询部门名称
        if (bill.getDepartmentId() != null) {
            // TODO: 从departmentRepository查询部门名称
        }

        // 查询操作人姓名
        if (bill.getDeductedBy() != null) {
            User user = userRepository.findById(bill.getDeductedBy());
            if (user != null) {
                dto.setDeductedByName(user.getRealName());
            }
        }

        return dto;
    }

    private String getDeductionStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待扣减";
            case "DEDUCTED" -> "已扣减";
            case "WAIVED" -> "已减免";
            default -> status;
        };
    }
}
