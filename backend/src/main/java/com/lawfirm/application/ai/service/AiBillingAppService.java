package com.lawfirm.application.ai.service;

import com.lawfirm.application.ai.command.SalaryDeductionLinkCommand;
import com.lawfirm.application.ai.dto.AiMonthlyBillDTO;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiMonthlyBill;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiMonthlyBillRepository;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI账单应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBillingAppService {

  /** AI月度账单仓储 */
  private final AiMonthlyBillRepository billRepository;

  /** AI使用日志仓储 */
  private final AiUsageLogRepository usageLogRepository;

  /** AI用户配额仓储 */
  private final AiUserQuotaRepository quotaRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /** 系统配置应用服务 */
  private final SysConfigAppService sysConfigAppService;

  /**
   * 获取指定月份的用户账单列表
   *
   * @param year 年份
   * @param month 月份
   * @return 账单列表
   */
  public List<AiMonthlyBillDTO> getMonthlyBills(final Integer year, final Integer month) {
    // 检查权限：只有管理员、财务可以查看
    if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
      throw new BusinessException("无权查看AI费用账单");
    }

    Integer finalYear = year;
    Integer finalMonth = month;
    if (finalYear == null) {
      finalYear = LocalDate.now().getYear();
    }
    if (finalMonth == null) {
      finalMonth = LocalDate.now().getMonthValue();
    }

    List<AiMonthlyBill> bills = billRepository.findByPeriod(finalYear, finalMonth);
    return bills.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取指定用户的账单
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 账单DTO
   */
  public AiMonthlyBillDTO getUserBill(final Long userId, final Integer year, final Integer month) {
    // 普通用户只能查看自己的账单
    Long currentUserId = SecurityUtils.getUserId();
    if (!SecurityUtils.isAdmin()
        && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")
        && !currentUserId.equals(userId)) {
      throw new BusinessException("无权查看他人账单");
    }

    Integer finalYear = year;
    Integer finalMonth = month;
    if (finalYear == null) {
      finalYear = LocalDate.now().getYear();
    }
    if (finalMonth == null) {
      finalMonth = LocalDate.now().getMonthValue();
    }

    AiMonthlyBill bill = billRepository.findByUserAndPeriod(userId, finalYear, finalMonth);
    if (bill == null) {
      return null;
    }
    return toDTO(bill);
  }

  /**
   * 生成月度账单
   *
   * @param year 年份
   * @param month 月份
   * @return 生成的账单数量
   */
  @Transactional
  public int generateMonthlyBills(final Integer year, final Integer month) {
    // 检查权限
    if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE")) {
      throw new BusinessException("无权生成AI费用账单");
    }

    Integer finalYear = year;
    Integer finalMonth = month;
    if (finalYear == null) {
      finalYear = LocalDate.now().minusMonths(1).getYear();
    }
    if (finalMonth == null) {
      finalMonth = LocalDate.now().minusMonths(1).getMonthValue();
    }

    log.info("开始生成{}年{}月AI费用账单", finalYear, finalMonth);

    LocalDate startDate = LocalDate.of(finalYear, finalMonth, 1);
    LocalDate endDate = YearMonth.of(finalYear, finalMonth).atEndOfMonth();

    // 获取该月有使用记录的所有用户
    List<Map<String, Object>> userUsageList =
        usageLogRepository.getActiveUsersInPeriod(startDate, endDate);

    int count = 0;
    for (Map<String, Object> userUsage : userUsageList) {
      Long userId = ((Number) userUsage.get("user_id")).longValue();

      try {
        generateUserBill(userId, finalYear, finalMonth);
        count++;
      } catch (Exception e) {
        log.error("生成用户{}的账单失败", userId, e);
      }
    }

    log.info("生成{}年{}月AI费用账单完成，共生成{}份账单", finalYear, finalMonth, count);
    return count;
  }

  /**
   * 生成单个用户的月度账单
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 账单DTO
   */
  @Transactional
  public AiMonthlyBillDTO generateUserBill(
      final Long userId, final Integer year, final Integer month) {
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
    BigDecimal totalCost =
        stats.get("total_cost") != null ? (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO;
    BigDecimal userCost = calculateUserCost(totalCost, chargeRatio);

    // 创建账单
    AiMonthlyBill bill =
        AiMonthlyBill.builder()
            .billYear(year)
            .billMonth(month)
            .userId(userId)
            .userName(user.getRealName())
            .departmentId(user.getDepartmentId())
            .totalCalls(
                stats.get("total_calls") != null
                    ? ((Number) stats.get("total_calls")).intValue()
                    : 0)
            .totalTokens(
                stats.get("total_tokens") != null
                    ? ((Number) stats.get("total_tokens")).longValue()
                    : 0L)
            .promptTokens(
                stats.get("prompt_tokens") != null
                    ? ((Number) stats.get("prompt_tokens")).longValue()
                    : 0L)
            .completionTokens(
                stats.get("completion_tokens") != null
                    ? ((Number) stats.get("completion_tokens")).longValue()
                    : 0L)
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
   *
   * @param billId 账单ID
   * @param remark 备注
   */
  @Transactional
  public void markDeducted(final Long billId, final String remark) {
    // 检查权限
    if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE")) {
      throw new BusinessException("无权操作账单");
    }

    AiMonthlyBill bill = billRepository.getByIdOrThrow(billId, "账单不存在");

    if ("DEDUCTED".equals(bill.getDeductionStatus())
        || "WAIVED".equals(bill.getDeductionStatus())) {
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
   *
   * @param billId 账单ID
   * @param reason 原因
   */
  @Transactional
  public void waiveBill(final Long billId, final String reason) {
    // 检查权限
    if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
      throw new BusinessException("无权减免账单");
    }

    AiMonthlyBill bill = billRepository.getByIdOrThrow(billId, "账单不存在");

    if ("DEDUCTED".equals(bill.getDeductionStatus())
        || "WAIVED".equals(bill.getDeductionStatus())) {
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
   *
   * @param command 关联命令
   */
  @Transactional
  public void linkToSalaryDeduction(final SalaryDeductionLinkCommand command) {
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
   * 查询待扣减账单数量
   *
   * @return 待扣减账单数量
   */
  public int countPendingBills() {
    List<AiMonthlyBill> pendingBills = billRepository.findByStatus("PENDING");
    return pendingBills != null ? pendingBills.size() : 0;
  }

  /**
   * 获取收费比例.
   *
   * @param userId 用户ID
   * @return 收费比例(0-100)
   */
  private Integer getChargeRatio(final Long userId) {
    // 1. 检查用户是否在免费名单
    AiUserQuota quota = quotaRepository.findByUserId(userId);
    if (quota != null && Boolean.TRUE.equals(quota.getExemptBilling())) {
      return 0; // 免计费用户
    }

    // 2. 读取管理员配置的收费比例（从系统配置读取）
    String chargeRatioStr = sysConfigAppService.getConfigValue("ai.billing.charge_ratio");
    if (chargeRatioStr != null && !chargeRatioStr.trim().isEmpty()) {
      try {
        Integer ratio = Integer.parseInt(chargeRatioStr.trim());
        // 确保比例在合理范围内（0-100）
        if (ratio >= 0 && ratio <= 100) {
          return ratio;
        } else {
          log.warn("配置ai.billing.charge_ratio值{}超出范围(0-100)，使用默认值100", ratio);
        }
      } catch (NumberFormatException e) {
        log.warn("配置ai.billing.charge_ratio值{}无法解析为整数，使用默认值100", chargeRatioStr, e);
      }
    }
    return 100; // 默认100%
  }

  /**
   * 计算用户应付费用.
   *
   * @param totalCost 总费用
   * @param chargeRatio 收费比例
   * @return 用户应付费用
   */
  private BigDecimal calculateUserCost(final BigDecimal totalCost, final int chargeRatio) {
    if (chargeRatio <= 0) {
      return BigDecimal.ZERO;
    }
    if (chargeRatio >= 100) {
      return totalCost;
    }

    return totalCost
        .multiply(BigDecimal.valueOf(chargeRatio))
        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
  }

  /**
   * Entity转DTO.
   *
   * @param bill 账单实体
   * @return 账单DTO
   */
  private AiMonthlyBillDTO toDTO(final AiMonthlyBill bill) {
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
      Department department = departmentRepository.getById(bill.getDepartmentId());
      if (department != null) {
        dto.setDepartmentName(department.getName());
      }
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

  private String getDeductionStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "PENDING" -> "待扣减";
      case "DEDUCTED" -> "已扣减";
      case "WAIVED" -> "已减免";
      default -> status;
    };
  }
}
