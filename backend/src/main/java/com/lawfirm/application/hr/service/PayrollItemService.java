package com.lawfirm.application.hr.service;

import com.lawfirm.application.hr.command.AddPayrollItemCommand;
import com.lawfirm.application.hr.command.ConfirmPayrollCommand;
import com.lawfirm.application.hr.command.UpdatePayrollItemCommand;
import com.lawfirm.application.hr.dto.PayrollItemDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.constant.CommissionStatus;
import com.lawfirm.common.constant.PayrollStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.PayrollDeduction;
import com.lawfirm.domain.hr.entity.PayrollIncome;
import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.domain.hr.entity.PayrollSheet;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.PayrollDeductionRepository;
import com.lawfirm.domain.hr.repository.PayrollIncomeRepository;
import com.lawfirm.domain.hr.repository.PayrollItemRepository;
import com.lawfirm.domain.hr.repository.PayrollSheetRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工资明细服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollItemService {

  /** 默认自动确认日期（每月27日） */
  private static final int DEFAULT_AUTO_CONFIRM_DAY = 27;

  /** 默认自动确认时间（23:59:59） */
  private static final int DEFAULT_AUTO_CONFIRM_HOUR = 23;

  /** 默认自动确认分钟和秒（59） */
  private static final int DEFAULT_AUTO_CONFIRM_MINUTE_SECOND = 59;

  /** 个税起征点（3000元） */
  private static final int INCOME_TAX_THRESHOLD_1 = 3000;

  /** 个税起征点（12000元） */
  private static final int INCOME_TAX_THRESHOLD_2 = 12000;

  /** 个税起征点（25000元） */
  private static final int INCOME_TAX_THRESHOLD_3 = 25000;

  /** 个税起征点（35000元） */
  private static final int INCOME_TAX_THRESHOLD_4 = 35000;

  /** 个税起征点（55000元） */
  private static final int INCOME_TAX_THRESHOLD_5 = 55000;

  /** 个税起征点（80000元） */
  private static final int INCOME_TAX_THRESHOLD_6 = 80000;

  /** 工资表仓储 */
  private final PayrollSheetRepository payrollSheetRepository;

  /** 工资项仓储 */
  private final PayrollItemRepository payrollItemRepository;

  /** 工资收入仓储 */
  private final PayrollIncomeRepository payrollIncomeRepository;

  /** 工资扣减仓储 */
  private final PayrollDeductionRepository payrollDeductionRepository;

  /** 员工仓储 */
  private final EmployeeRepository employeeRepository;

  /** HR合同仓储 */
  @Qualifier("hrContractRepository")
  private final com.lawfirm.domain.hr.repository.ContractRepository hrContractRepository;

  /** 财务合同仓储 */
  @Qualifier("financeContractRepository")
  private final ContractRepository financeContractRepository;

  /** 提成仓储 */
  private final CommissionRepository commissionRepository;

  /** 支付仓储 */
  private final PaymentRepository paymentRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 工资应用服务（延迟加载以打破循环依赖） */
  @Lazy @Autowired private PayrollAppService payrollAppService;

  /**
   * 为工资表添加员工工资明细.
   *
   * @param payrollSheetId 工资表ID
   * @param command 添加命令
   * @return 工资明细DTO
   */
  @Transactional
  public PayrollItemDTO addPayrollItemForEmployee(
      final Long payrollSheetId, final AddPayrollItemCommand command) {
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(payrollSheetId, "工资表不存在");

    // 检查工资表状态（审批后不可修改）
    if (!PayrollStatus.canEdit(sheet.getStatus())) {
      throw new BusinessException("工资表状态不允许添加员工（已提交审批或已审批通过的工资表不可修改）");
    }

    // 检查员工是否存在
    Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

    // 检查该员工是否已经在该工资表中有工资明细
    List<PayrollItem> existingItems = payrollItemRepository.findByPayrollSheetId(payrollSheetId);
    boolean alreadyExists =
        existingItems.stream()
            .anyMatch(item -> item.getEmployeeId().equals(command.getEmployeeId()));
    if (alreadyExists) {
      throw new BusinessException("该员工已在此工资表中存在工资明细");
    }

    PayrollItem item;

    if (Boolean.TRUE.equals(command.getAutoLoad())) {
      // 自动载入：从劳动合同和提成表载入数据
      LocalDate startDate = LocalDate.of(sheet.getPayrollYear(), sheet.getPayrollMonth(), 1);
      LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
      item = createPayrollItemForEmployee(sheet, employee, startDate, endDate);
    } else {
      // 手动创建：创建空的工资明细
      User user = userRepository.getByIdOrThrow(employee.getUserId(), "员工关联的用户不存在");

      item =
          PayrollItem.builder()
              .payrollSheetId(sheet.getId())
              .employeeId(employee.getId())
              .userId(user.getId())
              .employeeNo(employee.getEmployeeNo())
              .employeeName(user.getRealName())
              .grossAmount(BigDecimal.ZERO)
              .deductionAmount(BigDecimal.ZERO)
              .netAmount(BigDecimal.ZERO)
              .confirmStatus(PayrollStatus.ITEM_PENDING)
              .build();

      payrollItemRepository.save(item);
    }

    // 重新计算工资表汇总
    payrollAppService.recalculatePayrollSheetSummary(sheet.getId());

    log.info(
        "为工资表添加员工工资明细: payrollSheetId={}, employeeId={}, autoLoad={}",
        payrollSheetId,
        command.getEmployeeId(),
        command.getAutoLoad());

    return toItemDTO(item);
  }

  /**
   * 更新工资明细 权限：只有 ADMIN/DIRECTOR/FINANCE 可以更新工资明细.
   *
   * @param command 更新命令
   * @return 工资明细DTO
   */
  @Transactional
  public PayrollItemDTO updatePayrollItem(final UpdatePayrollItemCommand command) {
    // 权限检查：只有管理层和财务可以更新工资明细
    checkPayrollEditPermission();

    PayrollItem item = payrollItemRepository.getByIdOrThrow(command.getPayrollItemId(), "工资明细不存在");

    // 检查工资表状态（审批后不可修改）
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(item.getPayrollSheetId(), "工资表不存在");
    if (!PayrollStatus.canEdit(sheet.getStatus())) {
      throw new BusinessException("工资表状态不允许修改（已提交审批或已审批通过的工资表不可修改）");
    }

    // 更新扣减项
    BigDecimal taxDeductionAmount = BigDecimal.ZERO; // 税费扣减项总和
    BigDecimal otherDeductionAmount = BigDecimal.ZERO; // 其他扣减项总和

    if (command.getDeductions() != null) {
      // 删除旧的扣减项
      List<PayrollDeduction> oldDeductions =
          payrollDeductionRepository.findByPayrollItemId(item.getId());
      oldDeductions.forEach(deduction -> payrollDeductionRepository.removeById(deduction.getId()));

      // 创建新的扣减项，并分类统计
      for (UpdatePayrollItemCommand.PayrollDeductionItem deductionCmd : command.getDeductions()) {
        PayrollDeduction deduction =
            PayrollDeduction.builder()
                .payrollItemId(item.getId())
                .deductionType(deductionCmd.getDeductionType())
                .amount(deductionCmd.getAmount())
                .remark(deductionCmd.getRemark())
                .sourceType(
                    deductionCmd.getSourceType() != null ? deductionCmd.getSourceType() : "MANUAL")
                .build();
        payrollDeductionRepository.save(deduction);

        // 根据扣减类型分类：税费类（个人所得税、社保、公积金）和其他类
        if (isTaxDeduction(deductionCmd.getDeductionType())) {
          taxDeductionAmount = taxDeductionAmount.add(deductionCmd.getAmount());
        } else {
          otherDeductionAmount = otherDeductionAmount.add(deductionCmd.getAmount());
        }
      }
    }

    // 计算收入（提成总额）
    Employee employee = employeeRepository.getByIdOrThrow(item.getEmployeeId(), "员工不存在");
    User user = userRepository.getByIdOrThrow(employee.getUserId(), "员工关联的用户不存在");
    LocalDate startDate = LocalDate.of(sheet.getPayrollYear(), sheet.getPayrollMonth(), 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

    List<Commission> commissions =
        getCommissionDetailsForEmployee(user.getId(), startDate, endDate);
    BigDecimal commissionTotal =
        commissions.stream()
            .map(Commission::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 更新收入项（提成明细）- 删除旧的，创建新的
    List<PayrollIncome> oldIncomes = payrollIncomeRepository.findByPayrollItemId(item.getId());
    oldIncomes.forEach(income -> payrollIncomeRepository.removeById(income.getId()));

    // 保存新的收入项（提成明细）
    for (Commission comm : commissions) {
      String contractName = getContractName(comm.getContractId());

      PayrollIncome commissionIncome =
          PayrollIncome.builder()
              .payrollItemId(item.getId())
              .incomeType("COMMISSION")
              .amount(comm.getCommissionAmount())
              .sourceType("AUTO")
              .sourceId(comm.getContractId())
              .remark(String.format("合同提成：%s", contractName))
              .build();
      payrollIncomeRepository.save(commissionIncome);
    }

    // 应发工资 = 收入（提成总额）- 税费扣减项
    BigDecimal grossAmount = commissionTotal.subtract(taxDeductionAmount);

    // 实发工资 = 应发工资 - 其他扣减项（预支等）
    BigDecimal netAmount = grossAmount.subtract(otherDeductionAmount);

    // 扣减总额 = 税费扣减项 + 其他扣减项（用于显示）
    BigDecimal deductionAmount = taxDeductionAmount.add(otherDeductionAmount);

    // 设置确认截止时间（如果前端传入了，使用传入的值；否则使用默认值：每月27日24时）
    LocalDateTime confirmDeadline;
    if (command.getConfirmDeadline() != null) {
      confirmDeadline = command.getConfirmDeadline();
    } else {
      // 如果工资明细已存在且有确认截止时间，保持不变；否则使用默认值
      if (item.getConfirmDeadline() != null) {
        confirmDeadline = item.getConfirmDeadline();
      } else {
        // 默认每月27日24时
        confirmDeadline =
            LocalDateTime.of(
                sheet.getPayrollYear(),
                sheet.getPayrollMonth(),
                DEFAULT_AUTO_CONFIRM_DAY,
                DEFAULT_AUTO_CONFIRM_HOUR,
                DEFAULT_AUTO_CONFIRM_MINUTE_SECOND,
                DEFAULT_AUTO_CONFIRM_MINUTE_SECOND);
      }
    }

    // 如果员工已经确认过或拒绝过，财务再次修改时需要重置确认状态为PENDING，让员工重新确认
    boolean needResetConfirmStatus =
        PayrollStatus.ITEM_CONFIRMED.equals(item.getConfirmStatus())
            || PayrollStatus.ITEM_REJECTED.equals(item.getConfirmStatus());

    // 更新工资明细
    item.setGrossAmount(grossAmount);
    item.setDeductionAmount(deductionAmount);
    item.setNetAmount(netAmount);
    item.setConfirmDeadline(confirmDeadline);

    // 如果员工已经确认过或拒绝过，重置确认状态为PENDING
    if (needResetConfirmStatus) {
      item.setConfirmStatus(PayrollStatus.ITEM_PENDING);
      item.setConfirmedAt(null);
      item.setConfirmComment(null);
      log.info(
          "员工已确认/拒绝的工资明细被财务修改，重置确认状态为PENDING: payrollItemId={}, oldStatus={}",
          item.getId(),
          item.getConfirmStatus());
    }

    payrollItemRepository.updateById(item);

    // 重新计算工资表汇总
    payrollAppService.recalculatePayrollSheetSummary(sheet.getId());

    // 如果工资表是草稿状态或已确认状态，自动提交（变为待确认状态），以便员工可以确认
    if (PayrollStatus.DRAFT.equals(sheet.getStatus())
        || PayrollStatus.CONFIRMED.equals(sheet.getStatus())) {
      sheet.setStatus(PayrollStatus.PENDING_CONFIRM);
      sheet.setSubmittedAt(LocalDateTime.now());
      sheet.setSubmittedBy(SecurityUtils.getUserId());
      payrollSheetRepository.updateById(sheet);
      log.info("财务编辑扣减项后自动提交工资表: payrollNo={}", sheet.getPayrollNo());
    }

    // 发送通知给员工
    sendPayrollUpdateNotification(
        item.getUserId(), sheet.getPayrollYear(), sheet.getPayrollMonth(), item.getId());

    return toItemDTO(item);
  }

  /**
   * 根据年月和员工ID更新或创建工资明细（用于没有工资表时也能编辑）.
   *
   * @param year 年份
   * @param month 月份
   * @param employeeId 员工ID
   * @param command 更新命令
   * @return 工资明细DTO
   */
  @Transactional
  public PayrollItemDTO updateOrCreatePayrollItemByEmployee(
      final Integer year,
      final Integer month,
      final Long employeeId,
      final UpdatePayrollItemCommand command) {
    // 1. 检查或创建工资表
    PayrollSheet sheet = ensurePayrollSheetExists(year, month);

    // 2. 检查或创建工资明细
    Employee employee = employeeRepository.getByIdOrThrow(employeeId, "员工不存在");
    User user = userRepository.getByIdOrThrow(employee.getUserId(), "员工关联的用户不存在");
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

    PayrollItem item =
        payrollItemRepository
            .lambdaQuery()
            .eq(PayrollItem::getPayrollSheetId, sheet.getId())
            .eq(PayrollItem::getEmployeeId, employeeId)
            .eq(PayrollItem::getDeleted, false)
            .one();

    LocalDateTime itemConfirmDeadline = determineConfirmDeadline(command, item, year, month, sheet);

    if (item == null) {
      item =
          createPayrollItemWithCommissions(
              sheet, employee, user, itemConfirmDeadline, startDate, endDate);
    }

    // 3. 更新扣减项
    DeductionAmounts deductionAmounts = updatePayrollDeductions(item, command);

    // 4. 计算收入并更新工资明细
    List<Commission> commissions =
        getCommissionDetailsForEmployee(user.getId(), startDate, endDate);
    PayrollAmounts amounts = calculatePayrollAmounts(commissions, deductionAmounts);
    LocalDateTime confirmDeadline = determineConfirmDeadline(command, item, year, month, sheet);
    updatePayrollItemInternal(item, amounts, confirmDeadline);

    // 5. 完成更新（重新计算汇总、提交、发送通知）
    finalizePayrollUpdate(sheet, item);

    return toItemDTO(item);
  }

  /**
   * 员工确认工资表。
   *
   * @param command 确认命令
   */
  @Transactional
  public void confirmPayrollItem(final ConfirmPayrollCommand command) {
    PayrollItem item = payrollItemRepository.getByIdOrThrow(command.getPayrollItemId(), "工资明细不存在");

    // 权限检查：只能确认自己的工资
    Long currentUserId = SecurityUtils.getUserId();
    if (!item.getUserId().equals(currentUserId)) {
      throw new BusinessException("只能确认自己的工资");
    }

    // 检查工资表状态
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(item.getPayrollSheetId(), "工资表不存在");
    if (!PayrollStatus.PENDING_CONFIRM.equals(sheet.getStatus())) {
      throw new BusinessException("工资表状态不允许确认");
    }

    // 如果拒绝，必须填写理由
    if (PayrollStatus.ITEM_REJECTED.equals(command.getConfirmStatus())) {
      if (command.getConfirmComment() == null || command.getConfirmComment().trim().isEmpty()) {
        throw new BusinessException("拒绝时必须填写理由");
      }
      // 拒绝后，工资表状态需要返回财务处理
      sheet.setStatus(PayrollStatus.DRAFT); // 返回草稿状态，财务可以查看并修改
      payrollSheetRepository.updateById(sheet);
    }

    // 更新确认状态
    item.setConfirmStatus(command.getConfirmStatus());
    item.setConfirmedAt(LocalDateTime.now());
    item.setConfirmComment(command.getConfirmComment());
    payrollItemRepository.updateById(item);

    // 重新计算已确认人数
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(item.getPayrollSheetId());
    long confirmedCount =
        items.stream()
            .filter(i -> PayrollStatus.ITEM_CONFIRMED.equals(i.getConfirmStatus()))
            .count();

    sheet.setConfirmedCount((int) confirmedCount);

    // 如果所有员工都已确认，自动更新工资表状态为已确认
    if (confirmedCount == items.size() && items.size() > 0) {
      sheet.setStatus(PayrollStatus.CONFIRMED);
    }

    payrollSheetRepository.updateById(sheet);

    log.info("员工确认工资: payrollItemId={}, status={}", item.getId(), command.getConfirmStatus());
  }

  /**
   * 创建工资明细（用于自动生成）
   *
   * @param sheet 工资表
   * @param employee 员工
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工资明细
   */
  public PayrollItem createPayrollItemForEmployee(
      final PayrollSheet sheet,
      final Employee employee,
      final LocalDate startDate,
      final LocalDate endDate) {
    if (employee == null) {
      log.warn("员工信息为空，跳过创建工资明细");
      return null;
    }

    if (employee.getUserId() == null) {
      log.warn("员工userId为空，跳过创建工资明细: employeeId={}", employee.getId());
      return null;
    }

    User user =
        userRepository.getByIdOrThrow(
            employee.getUserId(), "员工关联的用户不存在: userId=" + employee.getUserId());

    // 1. 获取提成明细（从提成表汇总，为每个合同创建单独的收入项）
    // 只获取已审批（APPROVED）或已发放（PAID）状态的提成
    List<Commission> commissions =
        getCommissionDetailsForEmployee(user.getId(), startDate, endDate);
    BigDecimal commissionTotal = BigDecimal.ZERO;
    for (Commission comm : commissions) {
      commissionTotal = commissionTotal.add(comm.getCommissionAmount());
    }

    // 2. 计算扣减项（自动计算社保、公积金、个税）
    // 扣减计算基于基本工资作为社保公积金基数
    BigDecimal taxDeductionAmount = BigDecimal.ZERO; // 税费扣减项总和
    BigDecimal otherDeductionAmount = BigDecimal.ZERO; // 其他扣减项总和

    // 获取员工的劳动合同，使用基本工资作为社保公积金基数
    Optional<com.lawfirm.domain.hr.entity.Contract> contractOpt =
        hrContractRepository.findActiveContractByEmployeeId(employee.getId());
    if (contractOpt.isPresent()) {
      com.lawfirm.domain.hr.entity.Contract hrContract = contractOpt.get();

      // 使用基本工资作为社保和公积金的缴费基数
      BigDecimal baseSalary = hrContract.getBaseSalary();
      if (baseSalary == null) {
        baseSalary = BigDecimal.ZERO;
      }

      // 社保个人部分（约10.5%：养老8% + 医疗2% + 失业0.5%）
      BigDecimal socialInsurance = baseSalary.multiply(new BigDecimal("0.105"));

      // 公积金个人部分（通常5%-12%，取7%）
      BigDecimal housingFund = baseSalary.multiply(new BigDecimal("0.07"));

      otherDeductionAmount = socialInsurance.add(housingFund);
    }

    // 计算应税收入（提成 - 社保公积金 - 5000元起征点）
    BigDecimal taxableIncome =
        commissionTotal.subtract(otherDeductionAmount).subtract(new BigDecimal("5000"));
    if (taxableIncome.compareTo(BigDecimal.ZERO) > 0) {
      // 简化的个税计算（累进税率：0-3000:3%, 3000-12000:10%, 12000-25000:20%...）
      taxDeductionAmount = calculateIncomeTax(taxableIncome);
    }

    // 应发工资 = 收入（提成总额）- 税费扣减项
    BigDecimal grossAmount = commissionTotal.subtract(taxDeductionAmount);

    // 实发工资 = 应发工资 - 其他扣减项（预支等）
    BigDecimal netAmount = grossAmount.subtract(otherDeductionAmount);

    // 扣减总额 = 税费扣减项 + 其他扣减项（用于显示）
    BigDecimal totalDeductionAmount = taxDeductionAmount.add(otherDeductionAmount);

    // 默认确认截止时间为每月27日24时
    LocalDateTime confirmDeadline =
        LocalDateTime.of(
            sheet.getPayrollYear(),
            sheet.getPayrollMonth(),
            DEFAULT_AUTO_CONFIRM_DAY,
            DEFAULT_AUTO_CONFIRM_HOUR,
            DEFAULT_AUTO_CONFIRM_MINUTE_SECOND,
            DEFAULT_AUTO_CONFIRM_MINUTE_SECOND);

    // 创建工资明细
    PayrollItem item =
        PayrollItem.builder()
            .payrollSheetId(sheet.getId())
            .employeeId(employee.getId())
            .userId(user.getId())
            .employeeNo(employee.getEmployeeNo())
            .employeeName(user.getRealName())
            .grossAmount(grossAmount)
            .deductionAmount(totalDeductionAmount)
            .netAmount(netAmount)
            .confirmStatus(PayrollStatus.ITEM_PENDING)
            .confirmDeadline(confirmDeadline)
            .build();

    payrollItemRepository.save(item);

    // 为每个提成记录创建单独的收入项，记录合同信息
    for (Commission comm : commissions) {
      String contractName = getContractName(comm.getContractId());

      PayrollIncome commissionIncome =
          PayrollIncome.builder()
              .payrollItemId(item.getId())
              .incomeType("COMMISSION")
              .amount(comm.getCommissionAmount())
              .sourceType("AUTO")
              .sourceId(comm.getContractId()) // 记录合同ID
              .remark(String.format("合同提成：%s", contractName)) // 记录合同名称
              .build();
      payrollIncomeRepository.save(commissionIncome);
    }

    return item;
  }

  /**
   * 转换为工资明细DTO。
   *
   * @param item 工资明细实体
   * @return 工资明细DTO
   */
  public PayrollItemDTO toItemDTO(final PayrollItem item) {
    if (item == null) {
      return null;
    }

    PayrollItemDTO dto = new PayrollItemDTO();
    dto.setId(item.getId());
    dto.setPayrollSheetId(item.getPayrollSheetId());
    dto.setEmployeeId(item.getEmployeeId());
    dto.setUserId(item.getUserId());
    dto.setEmployeeNo(item.getEmployeeNo());
    dto.setEmployeeName(item.getEmployeeName());
    dto.setGrossAmount(item.getGrossAmount());
    dto.setDeductionAmount(item.getDeductionAmount());
    dto.setNetAmount(item.getNetAmount());
    dto.setConfirmStatus(item.getConfirmStatus());
    dto.setConfirmStatusName(getConfirmStatusName(item.getConfirmStatus()));
    dto.setConfirmedAt(item.getConfirmedAt());
    dto.setConfirmComment(item.getConfirmComment());
    dto.setConfirmDeadline(item.getConfirmDeadline());
    dto.setCreatedAt(item.getCreatedAt());
    dto.setUpdatedAt(item.getUpdatedAt());

    // 加载收入项和扣减项（只有当ID不为null时才查询）
    if (item.getId() != null) {
      List<PayrollIncome> incomes = payrollIncomeRepository.findByPayrollItemId(item.getId());
      dto.setIncomes(
          incomes != null
              ? incomes.stream().map(this::toIncomeDTO).collect(Collectors.toList())
              : new ArrayList<>());

      List<PayrollDeduction> deductions =
          payrollDeductionRepository.findByPayrollItemId(item.getId());
      dto.setDeductions(
          deductions != null
              ? deductions.stream().map(this::toDeductionDTO).collect(Collectors.toList())
              : new ArrayList<>());
    } else {
      dto.setIncomes(new ArrayList<>());
      dto.setDeductions(new ArrayList<>());
    }

    return dto;
  }

  /**
   * 确保工资表存在
   *
   * @param year 年份
   * @param month 月份
   * @return 工资表
   */
  private PayrollSheet ensurePayrollSheetExists(final Integer year, final Integer month) {
    PayrollSheet sheet = payrollSheetRepository.findByYearAndMonth(year, month).orElse(null);

    if (sheet == null) {
      // 创建工资表
      String payrollNo = payrollAppService.generatePayrollNo(year, month);
      // 默认自动确认截止时间为每月27日24时
      LocalDateTime autoConfirmDeadline =
          LocalDateTime.of(
              year,
              month,
              DEFAULT_AUTO_CONFIRM_DAY,
              DEFAULT_AUTO_CONFIRM_HOUR,
              DEFAULT_AUTO_CONFIRM_MINUTE_SECOND,
              DEFAULT_AUTO_CONFIRM_MINUTE_SECOND);

      sheet =
          PayrollSheet.builder()
              .payrollNo(payrollNo)
              .payrollYear(year)
              .payrollMonth(month)
              .status(PayrollStatus.DRAFT)
              .totalEmployees(0)
              .totalGrossAmount(BigDecimal.ZERO)
              .totalDeductionAmount(BigDecimal.ZERO)
              .totalNetAmount(BigDecimal.ZERO)
              .confirmedCount(0)
              .autoConfirmDeadline(autoConfirmDeadline)
              .build();
      payrollSheetRepository.save(sheet);
    }

    return sheet;
  }

  /** 扣减金额封装类 */
  private static class DeductionAmounts {
    /** 税费扣减项总和 */
    private BigDecimal taxDeductionAmount = BigDecimal.ZERO;

    /** 其他扣减项总和 */
    private BigDecimal otherDeductionAmount = BigDecimal.ZERO;
  }

  /** 工资金额封装类 */
  private static class PayrollAmounts {
    /** 应发工资 */
    private BigDecimal grossAmount;

    /** 扣减总额 */
    private BigDecimal deductionAmount;

    /** 实发工资 */
    private BigDecimal netAmount;
  }

  /**
   * 确定确认截止时间
   *
   * @param command 更新命令
   * @param item 工资明细
   * @param year 年份
   * @param month 月份
   * @param sheet 工资表
   * @return 确认截止时间
   */
  private LocalDateTime determineConfirmDeadline(
      final UpdatePayrollItemCommand command,
      final PayrollItem item,
      final Integer year,
      final Integer month,
      final PayrollSheet sheet) {
    if (command.getConfirmDeadline() != null) {
      return command.getConfirmDeadline();
    }
    // 如果工资明细已存在且有确认截止时间，保持不变；否则使用默认值
    if (item != null && item.getConfirmDeadline() != null) {
      return item.getConfirmDeadline();
    }
    // 默认每月27日24时
    return LocalDateTime.of(
        sheet != null ? sheet.getPayrollYear() : year,
        sheet != null ? sheet.getPayrollMonth() : month,
        DEFAULT_AUTO_CONFIRM_DAY,
        DEFAULT_AUTO_CONFIRM_HOUR,
        DEFAULT_AUTO_CONFIRM_MINUTE_SECOND,
        DEFAULT_AUTO_CONFIRM_MINUTE_SECOND);
  }

  /**
   * 创建工资明细并保存提成收入项
   *
   * @param sheet 工资表
   * @param employee 员工
   * @param user 用户
   * @param confirmDeadline 确认截止时间
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工资明细
   */
  private PayrollItem createPayrollItemWithCommissions(
      final PayrollSheet sheet,
      final Employee employee,
      final User user,
      final LocalDateTime confirmDeadline,
      final LocalDate startDate,
      final LocalDate endDate) {
    // 创建工资明细，实时计算提成
    List<Commission> commissions =
        getCommissionDetailsForEmployee(user.getId(), startDate, endDate);
    BigDecimal commissionTotal =
        commissions.stream()
            .map(Commission::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    PayrollItem item =
        PayrollItem.builder()
            .payrollSheetId(sheet.getId())
            .employeeId(employee.getId())
            .userId(user.getId())
            .employeeNo(employee.getEmployeeNo())
            .employeeName(user.getRealName())
            .grossAmount(commissionTotal)
            .deductionAmount(BigDecimal.ZERO)
            .netAmount(commissionTotal)
            .confirmStatus(PayrollStatus.ITEM_PENDING)
            .confirmDeadline(confirmDeadline)
            .build();
    payrollItemRepository.save(item);

    // 保存收入项（提成明细）
    saveCommissionIncomes(item, commissions);

    return item;
  }

  /**
   * 保存提成收入项
   *
   * @param item 工资明细
   * @param commissions 提成列表
   */
  private void saveCommissionIncomes(final PayrollItem item, final List<Commission> commissions) {
    for (Commission comm : commissions) {
      String contractName = getContractName(comm.getContractId());

      PayrollIncome commissionIncome =
          PayrollIncome.builder()
              .payrollItemId(item.getId())
              .incomeType("COMMISSION")
              .amount(comm.getCommissionAmount())
              .sourceType("AUTO")
              .sourceId(comm.getContractId())
              .remark(String.format("合同提成：%s", contractName))
              .build();
      payrollIncomeRepository.save(commissionIncome);
    }
  }

  /**
   * 获取合同名称
   *
   * @param contractId 合同ID
   * @return 合同名称
   */
  private String getContractName(final Long contractId) {
    if (contractId == null) {
      return "未知合同";
    }
    try {
      Contract financeContract = financeContractRepository.getById(contractId);
      if (financeContract != null) {
        return financeContract.getName() != null && !financeContract.getName().isEmpty()
            ? financeContract.getName()
            : (financeContract.getContractNo() != null ? financeContract.getContractNo() : "未知合同");
      }
    } catch (Exception e) {
      log.warn("获取合同信息失败: contractId={}", contractId, e);
    }
    return "未知合同";
  }

  /**
   * 更新扣减项
   *
   * @param item 工资明细
   * @param command 更新命令
   * @return 扣减金额
   */
  private DeductionAmounts updatePayrollDeductions(
      final PayrollItem item, final UpdatePayrollItemCommand command) {
    DeductionAmounts amounts = new DeductionAmounts();

    if (command.getDeductions() != null) {
      // 删除旧的扣减项
      List<PayrollDeduction> oldDeductions =
          payrollDeductionRepository.findByPayrollItemId(item.getId());
      oldDeductions.forEach(deduction -> payrollDeductionRepository.removeById(deduction.getId()));

      // 创建新的扣减项，并分类统计
      for (UpdatePayrollItemCommand.PayrollDeductionItem deductionCmd : command.getDeductions()) {
        PayrollDeduction deduction =
            PayrollDeduction.builder()
                .payrollItemId(item.getId())
                .deductionType(deductionCmd.getDeductionType())
                .amount(deductionCmd.getAmount())
                .remark(deductionCmd.getRemark())
                .sourceType(
                    deductionCmd.getSourceType() != null ? deductionCmd.getSourceType() : "MANUAL")
                .build();
        payrollDeductionRepository.save(deduction);

        // 根据扣减类型分类：税费类（个人所得税、社保、公积金）和其他类
        if (isTaxDeduction(deductionCmd.getDeductionType())) {
          amounts.taxDeductionAmount = amounts.taxDeductionAmount.add(deductionCmd.getAmount());
        } else {
          amounts.otherDeductionAmount = amounts.otherDeductionAmount.add(deductionCmd.getAmount());
        }
      }
    }

    return amounts;
  }

  /**
   * 计算工资金额
   *
   * @param commissions 提成列表
   * @param deductionAmounts 扣减金额
   * @return 工资金额
   */
  private PayrollAmounts calculatePayrollAmounts(
      final List<Commission> commissions, final DeductionAmounts deductionAmounts) {
    PayrollAmounts amounts = new PayrollAmounts();

    BigDecimal commissionTotal =
        commissions.stream()
            .map(Commission::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 应发工资 = 收入（提成总额）- 税费扣减项
    amounts.grossAmount = commissionTotal.subtract(deductionAmounts.taxDeductionAmount);

    // 实发工资 = 应发工资 - 其他扣减项（预支等）
    amounts.netAmount = amounts.grossAmount.subtract(deductionAmounts.otherDeductionAmount);

    // 扣减总额 = 税费扣减项 + 其他扣减项（用于显示）
    amounts.deductionAmount =
        deductionAmounts.taxDeductionAmount.add(deductionAmounts.otherDeductionAmount);

    return amounts;
  }

  /**
   * 更新工资明细
   *
   * @param item 工资明细
   * @param amounts 工资金额
   * @param confirmDeadline 确认截止时间
   */
  private void updatePayrollItemInternal(
      final PayrollItem item, final PayrollAmounts amounts, final LocalDateTime confirmDeadline) {
    // 如果员工已经确认过或拒绝过，财务再次修改时需要重置确认状态为PENDING，让员工重新确认
    boolean needResetConfirmStatus =
        PayrollStatus.ITEM_CONFIRMED.equals(item.getConfirmStatus())
            || PayrollStatus.ITEM_REJECTED.equals(item.getConfirmStatus());

    String oldStatus = item.getConfirmStatus();
    item.setGrossAmount(amounts.grossAmount);
    item.setDeductionAmount(amounts.deductionAmount);
    item.setNetAmount(amounts.netAmount);
    item.setConfirmDeadline(confirmDeadline);

    // 如果员工已经确认过或拒绝过，重置确认状态为PENDING
    if (needResetConfirmStatus) {
      item.setConfirmStatus(PayrollStatus.ITEM_PENDING);
      item.setConfirmedAt(null);
      item.setConfirmComment(null);
      log.info(
          "员工已确认/拒绝的工资明细被财务修改，重置确认状态为PENDING: payrollItemId={}, oldStatus={}",
          item.getId(),
          oldStatus);
    }

    payrollItemRepository.updateById(item);
  }

  /**
   * 完成工资更新（重新计算汇总、提交、发送通知）
   *
   * @param sheet 工资表
   * @param item 工资明细
   */
  private void finalizePayrollUpdate(final PayrollSheet sheet, final PayrollItem item) {
    // 重新计算工资表汇总
    payrollAppService.recalculatePayrollSheetSummary(sheet.getId());

    // 如果工资表是草稿状态或已确认状态，自动提交（变为待确认状态），以便员工可以确认
    if (PayrollStatus.DRAFT.equals(sheet.getStatus())
        || PayrollStatus.CONFIRMED.equals(sheet.getStatus())) {
      sheet.setStatus(PayrollStatus.PENDING_CONFIRM);
      sheet.setSubmittedAt(LocalDateTime.now());
      sheet.setSubmittedBy(SecurityUtils.getUserId());
      payrollSheetRepository.updateById(sheet);
      log.info("财务编辑扣减项后自动提交工资表: payrollNo={}", sheet.getPayrollNo());
    }

    // 发送通知给员工
    sendPayrollUpdateNotification(
        item.getUserId(), sheet.getPayrollYear(), sheet.getPayrollMonth(), item.getId());
  }

  /**
   * 获取员工的提成明细列表（指定月份） 返回每个合同的提成记录，用于创建收入项明细 根据收款日期过滤，只返回指定月份内的已审批提成.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 提成明细列表
   */
  private List<Commission> getCommissionDetailsForEmployee(
      final Long userId, final LocalDate startDate, final LocalDate endDate) {
    // 查询该用户的所有已审批提成
    List<Commission> allCommissions = commissionRepository.findByUserId(userId);

    return allCommissions.stream()
        .filter(
            c -> {
              // 只返回已审批或已发放状态的提成
              if (!CommissionStatus.APPROVED.equals(c.getStatus())
                  && !CommissionStatus.PAID.equals(c.getStatus())) {
                return false;
              }

              // 根据收款日期判断是否属于指定月份
              if (c.getPaymentId() != null) {
                try {
                  com.lawfirm.domain.finance.entity.Payment payment =
                      paymentRepository.getById(c.getPaymentId());
                  if (payment != null && payment.getPaymentDate() != null) {
                    LocalDate paymentDate = payment.getPaymentDate();
                    // 判断收款日期是否在指定月份范围内
                    return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                  }
                } catch (Exception e) {
                  log.warn("查询收款记录失败: paymentId={}", c.getPaymentId(), e);
                }
              }

              // 如果没有收款记录，根据提成的创建时间判断（降级方案）
              if (c.getCreatedAt() != null) {
                LocalDate createdAt = c.getCreatedAt().toLocalDate();
                return !createdAt.isBefore(startDate) && !createdAt.isAfter(endDate);
              }

              return false;
            })
        .collect(Collectors.toList());
  }

  /**
   * 计算个人所得税（累进税率） 依据中国个税七级超额累进税率.
   *
   * @param taxableIncome 应税收入（已扣除起征点5000元和五险一金）
   * @return 应纳个人所得税
   */
  private BigDecimal calculateIncomeTax(final BigDecimal taxableIncome) {
    if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal tax;
    double income = taxableIncome.doubleValue();

    if (income <= INCOME_TAX_THRESHOLD_1) {
      // 3% 税率，速算扣除数 0
      tax = taxableIncome.multiply(new BigDecimal("0.03"));
    } else if (income <= INCOME_TAX_THRESHOLD_2) {
      // 10% 税率，速算扣除数 210
      tax = taxableIncome.multiply(new BigDecimal("0.10")).subtract(new BigDecimal("210"));
    } else if (income <= INCOME_TAX_THRESHOLD_3) {
      // 20% 税率，速算扣除数 1410
      tax = taxableIncome.multiply(new BigDecimal("0.20")).subtract(new BigDecimal("1410"));
    } else if (income <= INCOME_TAX_THRESHOLD_4) {
      // 25% 税率，速算扣除数 2660
      tax = taxableIncome.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("2660"));
    } else if (income <= INCOME_TAX_THRESHOLD_5) {
      // 30% 税率，速算扣除数 4410
      tax = taxableIncome.multiply(new BigDecimal("0.30")).subtract(new BigDecimal("4410"));
    } else if (income <= INCOME_TAX_THRESHOLD_6) {
      // 35% 税率，速算扣除数 7160
      tax = taxableIncome.multiply(new BigDecimal("0.35")).subtract(new BigDecimal("7160"));
    } else {
      // 45% 税率，速算扣除数 15160
      tax = taxableIncome.multiply(new BigDecimal("0.45")).subtract(new BigDecimal("15160"));
    }

    // 四舍五入保留两位小数
    return tax.setScale(2, java.math.RoundingMode.HALF_UP);
  }

  /**
   * 转换为收入项DTO。
   *
   * @param income 收入项实体
   * @return 收入项DTO
   */
  private com.lawfirm.application.hr.dto.PayrollIncomeDTO toIncomeDTO(final PayrollIncome income) {
    com.lawfirm.application.hr.dto.PayrollIncomeDTO dto =
        new com.lawfirm.application.hr.dto.PayrollIncomeDTO();
    dto.setId(income.getId());
    dto.setPayrollItemId(income.getPayrollItemId());
    dto.setIncomeType(income.getIncomeType());
    dto.setIncomeTypeName(getIncomeTypeName(income.getIncomeType()));
    dto.setAmount(income.getAmount());
    dto.setRemark(income.getRemark());
    dto.setSourceType(income.getSourceType());
    dto.setSourceTypeName(getSourceTypeName(income.getSourceType()));
    dto.setSourceId(income.getSourceId());
    return dto;
  }

  /**
   * 转换为扣减项DTO。
   *
   * @param deduction 扣减项实体
   * @return 扣减项DTO
   */
  private com.lawfirm.application.hr.dto.PayrollDeductionDTO toDeductionDTO(
      final PayrollDeduction deduction) {
    com.lawfirm.application.hr.dto.PayrollDeductionDTO dto =
        new com.lawfirm.application.hr.dto.PayrollDeductionDTO();
    dto.setId(deduction.getId());
    dto.setPayrollItemId(deduction.getPayrollItemId());
    dto.setDeductionType(deduction.getDeductionType());
    dto.setDeductionTypeName(getDeductionTypeName(deduction.getDeductionType()));
    dto.setAmount(deduction.getAmount());
    dto.setRemark(deduction.getRemark());
    dto.setSourceType(deduction.getSourceType());
    dto.setSourceTypeName(getSourceTypeName(deduction.getSourceType()));
    return dto;
  }

  /**
   * 判断是否为税费类扣减项 税费类：个人所得税、社保、公积金 其他类：其他扣款、预支等。
   *
   * @param deductionType 扣减类型
   * @return 是否为税费类扣减项
   */
  private boolean isTaxDeduction(final String deductionType) {
    if (deductionType == null) {
      return false;
    }
    return "INCOME_TAX".equals(deductionType)
        || "SOCIAL_INSURANCE".equals(deductionType)
        || "HOUSING_FUND".equals(deductionType)
        || deductionType.contains("税")
        || deductionType.contains("社保")
        || deductionType.contains("公积金");
  }

  /**
   * 发送工资更新通知给员工。
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @param payrollItemId 工资明细ID
   */
  private void sendPayrollUpdateNotification(
      final Long userId, final Integer year, final Integer month, final Long payrollItemId) {
    try {
      if (userId == null) {
        return;
      }

      String title = String.format("%d年%d月工资已更新", year, month);
      String content = String.format("您的%d年%d月工资明细已更新，请前往【我的工资】页面查看并确认。", year, month);

      notificationAppService.sendSystemNotification(
          userId, title, content, "PAYROLL", payrollItemId);

      log.info(
          "工资更新通知已发送: userId={}, year={}, month={}, payrollItemId={}",
          userId,
          year,
          month,
          payrollItemId);
    } catch (Exception e) {
      log.warn("发送工资更新通知失败: userId={}, payrollItemId={}", userId, payrollItemId, e);
    }
  }

  /**
   * 获取确认状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getConfirmStatusName(final String status) {
    return PayrollStatus.getConfirmStatusName(status);
  }

  /**
   * 获取收入类型名称
   *
   * @param type 收入类型代码
   * @return 收入类型名称
   */
  private String getIncomeTypeName(final String type) {
    if (type == null) {
      return "";
    }
    return switch (type) {
      case "BASE_SALARY" -> "基本工资";
      case "COMMISSION" -> "提成";
      case "PERFORMANCE_BONUS" -> "绩效奖金";
      case "OTHER_ALLOWANCE" -> "其他津贴";
      default -> type;
    };
  }

  /**
   * 获取扣减类型名称
   *
   * @param type 扣减类型代码
   * @return 扣减类型名称
   */
  private String getDeductionTypeName(final String type) {
    if (type == null) {
      return "";
    }
    return switch (type) {
      case "INCOME_TAX" -> "个人所得税";
      case "SOCIAL_INSURANCE" -> "社保个人部分";
      case "HOUSING_FUND" -> "公积金个人部分";
      case "OTHER_DEDUCTION" -> "其他扣款";
      default -> type;
    };
  }

  /**
   * 获取来源类型名称
   *
   * @param type 来源类型代码
   * @return 来源类型名称
   */
  private String getSourceTypeName(final String type) {
    if (type == null) {
      return "";
    }
    return switch (type) {
      case "AUTO" -> "自动汇总";
      case "MANUAL" -> "手动输入";
      case "IMPORT" -> "导入";
      default -> type;
    };
  }

  /** 检查是否有工资表编辑权限（ADMIN/FINANCE） */
  private void checkPayrollEditPermission() {
    if (SecurityUtils.isAdmin()) {
      return;
    }
    java.util.Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("FINANCE")) {
      throw new BusinessException("只有管理员和财务可以操作工资表");
    }
  }
}
