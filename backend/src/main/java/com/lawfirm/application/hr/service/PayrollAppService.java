package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.AddPayrollItemCommand;
import com.lawfirm.application.hr.command.ApprovePayrollCommand;
import com.lawfirm.application.hr.command.ConfirmPayrollCommand;
import com.lawfirm.application.hr.command.CreatePayrollSheetCommand;
import com.lawfirm.application.hr.command.IssuePayrollCommand;
import com.lawfirm.application.hr.command.SubmitApprovalCommand;
import com.lawfirm.application.hr.command.UpdatePayrollItemCommand;
import com.lawfirm.application.hr.dto.PayrollItemDTO;
import com.lawfirm.application.hr.dto.PayrollSheetDTO;
import com.lawfirm.application.hr.dto.PayrollSheetQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.constant.CommissionStatus;
import com.lawfirm.common.constant.EmployeeStatus;
import com.lawfirm.common.constant.PayrollStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
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
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.PayrollSheetMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工资管理应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollAppService {

  /** 默认自动确认日期（每月27日） */
  private static final int DEFAULT_AUTO_CONFIRM_DAY = 27;

  /** 默认自动确认时间（23:59:59） */
  private static final int DEFAULT_AUTO_CONFIRM_HOUR = 23;

  /** 默认自动确认分钟和秒（59） */
  private static final int DEFAULT_AUTO_CONFIRM_MINUTE_SECOND = 59;

  // 个税起征点常量已迁移到 PayrollItemService

  /** 工资表仓储 */
  private final PayrollSheetRepository payrollSheetRepository;

  /** 工资表Mapper */
  private final PayrollSheetMapper payrollSheetMapper;

  /** 工资项仓储 */
  private final PayrollItemRepository payrollItemRepository;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

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
  private final com.lawfirm.domain.finance.repository.PaymentRepository paymentRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** Excel导入导出服务 */
  private final ExcelImportExportService excelImportExportService;

  /** 工资明细服务 */
  private final PayrollItemService payrollItemService;

  /**
   * 分页查询工资表 权限：只有 ADMIN/DIRECTOR/FINANCE 可以查看工资表列表.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<PayrollSheetDTO> listPayrollSheets(final PayrollSheetQueryDTO query) {
    // 权限检查：只有管理层和财务可以查看工资表列表
    checkPayrollViewPermission();

    IPage<PayrollSheet> page =
        payrollSheetMapper.selectPayrollSheetPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getPayrollYear(),
            query.getPayrollMonth(),
            query.getStatus(),
            query.getPayrollNo());

    return PageResult.of(
        page.getRecords().stream().map(this::toSheetDTO).collect(Collectors.toList()),
        page.getTotal(),
        query.getPageNum(),
        query.getPageSize());
  }

  /**
   * 根据ID查询工资表详情 权限：只有 ADMIN/DIRECTOR/FINANCE 可以查看工资表详情.
   *
   * @param id 工资表ID
   * @return 工资表DTO
   */
  public PayrollSheetDTO getPayrollSheetById(final Long id) {
    // 权限检查：只有管理层和财务可以查看工资表详情
    checkPayrollViewPermission();

    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(id, "工资表不存在");
    PayrollSheetDTO dto = toSheetDTO(sheet);

    // 加载工资明细
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(id);
    dto.setItems(items.stream().map(payrollItemService::toItemDTO).collect(Collectors.toList()));

    return dto;
  }

  /**
   * 查询工资表的所有员工工资明细列表（用于列表展示）.
   *
   * @param sheetId 工资表ID
   * @return 工资明细列表
   */
  public List<PayrollItemDTO> getPayrollItemsBySheetId(final Long sheetId) {
    payrollSheetRepository.getByIdOrThrow(sheetId, "工资表不存在");
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(sheetId);
    return items.stream().map(payrollItemService::toItemDTO).collect(Collectors.toList());
  }

  /**
   * 根据年月查询员工工资明细列表 显示规则： 1. 员工属性确认后，自动在当月的工资管理列表中显示 2. 员工离职后，次月开始不再在工资管理列表中显示
   *
   * <p>性能优化：使用批量查询替代N+1查询.
   *
   * @param year 年份
   * @param month 月份
   * @return 工资明细列表
   */
  public List<PayrollItemDTO> getPayrollItemsByYearMonth(final Integer year, final Integer month) {
    // 先查找该年月的工资表
    PayrollSheet sheet = payrollSheetRepository.findByYearAndMonth(year, month).orElse(null);

    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
    LocalDate queryMonthStart = LocalDate.of(year, month, 1);
    LocalDate queryMonthEnd = queryMonthStart.withDayOfMonth(queryMonthStart.lengthOfMonth());

    // 1. 批量获取符合条件的员工（1次查询）
    List<Employee> allEmployees =
        employeeRepository.lambdaQuery().list().stream()
            .filter(
                employee -> {
                  if (employee == null || employee.getUserId() == null) {
                    return false;
                  }

                  LocalDateTime createdAt = employee.getCreatedAt();
                  if (createdAt == null) {
                    return false;
                  }
                  LocalDate employeeCreatedDate = createdAt.toLocalDate();

                  if (employeeCreatedDate.isAfter(queryMonthEnd)) {
                    return false;
                  }

                  if (EmployeeStatus.RESIGNED.equals(employee.getWorkStatus())
                      && employee.getResignationDate() != null) {
                    LocalDate resignationDate = employee.getResignationDate();
                    if (resignationDate.isBefore(queryMonthStart)) {
                      return false;
                    }
                  }

                  return true;
                })
            .collect(Collectors.toList());

    if (allEmployees.isEmpty()) {
      return new ArrayList<>();
    }

    // 2. 批量获取所有员工对应的用户信息（1次查询）
    List<Long> userIds =
        allEmployees.stream()
            .map(Employee::getUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

    Map<Long, User> userMap =
        userRepository.listByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

    if (sheet == null || sheet.getId() == null) {
      // 如果工资表不存在，返回符合条件的员工列表，实时计算提成
      return buildPayrollItemsWithCommissions(allEmployees, userMap, startDate, endDate);
    }

    // 查询该工资表的所有员工工资明细
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(sheet.getId());
    if (items == null) {
      items = new ArrayList<>();
    }

    // 将工资明细转换为Map，以employeeId为key
    Map<Long, PayrollItemDTO> itemMap =
        items.stream()
            .filter(item -> item != null && item.getId() != null && item.getEmployeeId() != null)
            .map(payrollItemService::toItemDTO)
            .filter(dto -> dto != null)
            .collect(
                Collectors.toMap(
                    PayrollItemDTO::getEmployeeId,
                    dto -> dto,
                    (existing, replacement) -> existing));

    // 为所有在职员工创建DTO，如果已有工资明细则使用，否则创建空的（工资为0）
    return allEmployees.stream()
        .filter(employee -> employee != null && employee.getUserId() != null)
        .map(
            employee -> {
              PayrollItemDTO dto = itemMap.get(employee.getId());
              if (dto != null) {
                return dto;
              }
              // 如果没有工资明细，创建空的DTO
              User user = userMap.get(employee.getUserId());
              if (user == null) {
                return null;
              }
              dto = new PayrollItemDTO();
              dto.setEmployeeId(employee.getId());
              dto.setUserId(employee.getUserId());
              dto.setEmployeeNo(employee.getEmployeeNo());
              dto.setEmployeeName(user.getRealName());
              dto.setGrossAmount(BigDecimal.ZERO);
              dto.setDeductionAmount(BigDecimal.ZERO);
              dto.setNetAmount(BigDecimal.ZERO);
              dto.setConfirmStatus(PayrollStatus.ITEM_PENDING);
              dto.setConfirmStatusName(
                  PayrollStatus.getConfirmStatusName(PayrollStatus.ITEM_PENDING));
              dto.setIncomes(new ArrayList<>());
              dto.setDeductions(new ArrayList<>());
              return dto;
            })
        .filter(dto -> dto != null)
        .collect(Collectors.toList());
  }

  /**
   * 批量构建工资明细（带提成计算）- 优化版 使用批量查询替代N+1查询，性能提升显著
   *
   * <p>优化策略： 1. 用户信息已通过参数传入（批量查询） 2. 提成查询保持使用Repository方法（内部使用JOIN查询） 3. 批量获取合同信息（1次查询） 4.
   * 批量获取收款信息（1次查询）.
   *
   * @param employees 员工列表
   * @param userMap 用户Map
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工资明细列表
   */
  private List<PayrollItemDTO> buildPayrollItemsWithCommissions(
      final List<Employee> employees,
      final Map<Long, User> userMap,
      final LocalDate startDate,
      final LocalDate endDate) {

    if (employees.isEmpty()) {
      return new ArrayList<>();
    }

    List<Long> userIds =
        employees.stream()
            .map(Employee::getUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

    // 3. 批量获取所有用户的提成（避免N+1查询）
    // 注意：Commission通过CommissionDetail与用户关联，使用批量查询方法
    Map<Long, List<Commission>> commissionsByUser = commissionRepository.findByUserIdsGrouped(userIds);

    // 过滤只保留已审批或已发放状态
    for (Map.Entry<Long, List<Commission>> entry : commissionsByUser.entrySet()) {
      List<Commission> filtered =
          entry.getValue().stream()
              .filter(
                  c ->
                      CommissionStatus.APPROVED.equals(c.getStatus())
                          || CommissionStatus.PAID.equals(c.getStatus()))
              .collect(Collectors.toList());
      entry.setValue(filtered);
    }
    // 移除空列表的条目
    commissionsByUser.entrySet().removeIf(e -> e.getValue().isEmpty());

    // 收集所有提成记录
    List<Commission> allCommissions =
        commissionsByUser.values().stream().flatMap(List::stream).collect(Collectors.toList());

    // 4. 批量获取提成相关的收款记录（1次查询）
    Set<Long> paymentIds =
        allCommissions.stream()
            .map(Commission::getPaymentId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());

    Map<Long, com.lawfirm.domain.finance.entity.Payment> paymentMap =
        paymentIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : paymentRepository.listByIds(paymentIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p, (a, b) -> a));

    // 按用户重新过滤出指定月份的提成
    Map<Long, List<Commission>> filteredCommissionsByUser = new java.util.HashMap<>();
    for (Map.Entry<Long, List<Commission>> entry : commissionsByUser.entrySet()) {
      List<Commission> filtered =
          entry.getValue().stream()
              .filter(
                  c -> {
                    if (c.getPaymentId() == null) {
                      return false;
                    }
                    com.lawfirm.domain.finance.entity.Payment payment =
                        paymentMap.get(c.getPaymentId());
                    if (payment == null || payment.getPaymentDate() == null) {
                      return false;
                    }
                    LocalDate paymentDate = payment.getPaymentDate();
                    return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                  })
              .collect(Collectors.toList());
      if (!filtered.isEmpty()) {
        filteredCommissionsByUser.put(entry.getKey(), filtered);
      }
    }

    // 5. 批量获取提成相关的合同（1次查询）
    Set<Long> contractIds =
        filteredCommissionsByUser.values().stream()
            .flatMap(List::stream)
            .map(Commission::getContractId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());

    Map<Long, Contract> contractMap =
        contractIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : financeContractRepository.listByIds(contractIds).stream()
                .collect(Collectors.toMap(Contract::getId, c -> c, (a, b) -> a));

    // 构建员工工资明细DTO
    return employees.stream()
        .filter(employee -> employee != null && employee.getUserId() != null)
        .map(
            employee -> {
              User user = userMap.get(employee.getUserId());
              if (user == null) {
                return null;
              }

              // 获取该用户的提成列表
              List<Commission> userCommissions =
                  filteredCommissionsByUser.getOrDefault(
                      employee.getUserId(), java.util.Collections.emptyList());

              BigDecimal commissionTotal =
                  userCommissions.stream()
                      .map(Commission::getCommissionAmount)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);

              // 创建收入项列表
              List<com.lawfirm.application.hr.dto.PayrollIncomeDTO> incomes =
                  userCommissions.stream()
                      .map(
                          comm -> {
                            String contractName = "未知合同";
                            if (comm.getContractId() != null) {
                              Contract contract = contractMap.get(comm.getContractId());
                              if (contract != null) {
                                contractName =
                                    contract.getName() != null && !contract.getName().isEmpty()
                                        ? contract.getName()
                                        : (contract.getContractNo() != null
                                            ? contract.getContractNo()
                                            : "未知合同");
                              }
                            }

                            com.lawfirm.application.hr.dto.PayrollIncomeDTO incomeDto =
                                new com.lawfirm.application.hr.dto.PayrollIncomeDTO();
                            incomeDto.setIncomeType("COMMISSION");
                            incomeDto.setAmount(comm.getCommissionAmount());
                            incomeDto.setSourceType("AUTO");
                            incomeDto.setSourceId(comm.getContractId());
                            incomeDto.setRemark(String.format("合同提成：%s", contractName));
                            return incomeDto;
                          })
                      .collect(Collectors.toList());

              PayrollItemDTO dto = new PayrollItemDTO();
              dto.setEmployeeId(employee.getId());
              dto.setUserId(employee.getUserId());
              dto.setEmployeeNo(employee.getEmployeeNo());
              dto.setEmployeeName(user.getRealName());
              dto.setGrossAmount(commissionTotal);
              dto.setDeductionAmount(BigDecimal.ZERO);
              dto.setNetAmount(commissionTotal);
              dto.setConfirmStatus(PayrollStatus.ITEM_PENDING);
              dto.setConfirmStatusName(
                  PayrollStatus.getConfirmStatusName(PayrollStatus.ITEM_PENDING));
              dto.setIncomes(incomes);
              dto.setDeductions(new ArrayList<>());
              return dto;
            })
        .filter(dto -> dto != null)
        .collect(Collectors.toList());
  }

  /**
   * 创建工资表.
   *
   * @param command 创建命令
   * @return 工资表DTO
   */
  @Transactional
  public PayrollSheetDTO createPayrollSheet(final CreatePayrollSheetCommand command) {
    // 权限检查：只有财务角色可以创建工资表
    Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("FINANCE") && !roles.contains("ADMIN") && !roles.contains("DIRECTOR")) {
      throw new BusinessException("只有财务角色可以创建工资表");
    }

    // 检查是否已存在该年月的工资表
    if (payrollSheetRepository
        .findByYearAndMonth(command.getPayrollYear(), command.getPayrollMonth())
        .isPresent()) {
      throw new BusinessException("该年月的工资表已存在");
    }

    // 生成工资表编号
    String payrollNo = generatePayrollNo(command.getPayrollYear(), command.getPayrollMonth());

    // 设置自动确认截止时间（默认每月27日24时）
    LocalDateTime autoConfirmDeadline = command.getAutoConfirmDeadline();
    if (autoConfirmDeadline == null) {
      // 默认每月27日24时自动确认
      autoConfirmDeadline =
          LocalDateTime.of(
              command.getPayrollYear(),
              command.getPayrollMonth(),
              DEFAULT_AUTO_CONFIRM_DAY,
              DEFAULT_AUTO_CONFIRM_HOUR,
              DEFAULT_AUTO_CONFIRM_MINUTE_SECOND,
              DEFAULT_AUTO_CONFIRM_MINUTE_SECOND);
    }

    // 创建工资表
    PayrollSheet sheet =
        PayrollSheet.builder()
            .payrollNo(payrollNo)
            .payrollYear(command.getPayrollYear())
            .payrollMonth(command.getPayrollMonth())
            .status(PayrollStatus.DRAFT)
            .totalEmployees(0)
            .totalGrossAmount(BigDecimal.ZERO)
            .totalDeductionAmount(BigDecimal.ZERO)
            .totalNetAmount(BigDecimal.ZERO)
            .confirmedCount(0)
            .autoConfirmDeadline(autoConfirmDeadline)
            .build();

    payrollSheetRepository.save(sheet);

    // 自动为所有在职员工生成工资明细（从提成数据自动载入）
    generatePayrollItemsAuto(sheet);

    // 重新加载工资表以获取更新后的汇总数据
    PayrollSheet updatedSheet = payrollSheetRepository.getByIdOrThrow(sheet.getId(), "工资表不存在");

    log.info(
        "创建工资表成功: {} ({}-{})，已自动生成 {} 个员工的工资明细",
        payrollNo,
        command.getPayrollYear(),
        command.getPayrollMonth(),
        updatedSheet.getTotalEmployees());
    return toSheetDTO(updatedSheet);
  }

  /**
   * 自动生成工资明细（汇总提成和固定工资） 问题245修复：收集失败记录并在最后统一报告，确保数据一致性.
   *
   * @param sheet 工资表
   */
  @Transactional
  public void generatePayrollItemsAuto(final PayrollSheet sheet) {
    // 获取所有在职员工
    List<Employee> employees =
        employeeRepository.lambdaQuery().eq(Employee::getWorkStatus, EmployeeStatus.ACTIVE).list();

    LocalDate startDate = LocalDate.of(sheet.getPayrollYear(), sheet.getPayrollMonth(), 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

    BigDecimal totalGross = BigDecimal.ZERO;
    BigDecimal totalDeduction = BigDecimal.ZERO;
    BigDecimal totalNet = BigDecimal.ZERO;

    // 收集失败的员工记录
    List<String> failedEmployees = new ArrayList<>();
    int successCount = 0;

    for (Employee employee : employees) {
      try {
        PayrollItem item =
            payrollItemService.createPayrollItemForEmployee(sheet, employee, startDate, endDate);
        if (item != null) {
          totalGross = totalGross.add(item.getGrossAmount());
          totalDeduction = totalDeduction.add(item.getDeductionAmount());
          totalNet = totalNet.add(item.getNetAmount());
          successCount++;
        }
      } catch (Exception e) {
        String employeeInfo =
            employee.getEmployeeNo() != null
                ? employee.getEmployeeNo()
                : String.valueOf(employee.getId());
        failedEmployees.add(employeeInfo);
        log.error(
            "为员工生成工资明细失败: employeeId={}, employeeNo={}",
            employee.getId(),
            employee.getEmployeeNo(),
            e);
      }
    }

    // 更新工资表汇总（使用实际成功的数量）
    sheet.setTotalEmployees(successCount);
    sheet.setTotalGrossAmount(totalGross);
    sheet.setTotalDeductionAmount(totalDeduction);
    sheet.setTotalNetAmount(totalNet);
    payrollSheetRepository.updateById(sheet);

    // 如果有失败的记录，记录警告日志
    if (!failedEmployees.isEmpty()) {
      log.warn(
          "工资明细生成完成，但有{}个员工生成失败: {}", failedEmployees.size(), String.join(", ", failedEmployees));
    }
  }

  // createPayrollItemForEmployee 已迁移到 PayrollItemService

  /**
   * 更新工资明细 权限：只有 ADMIN/DIRECTOR/FINANCE 可以更新工资明细.
   *
   * @param command 更新命令
   * @return 工资明细DTO
   */
  @Transactional
  public PayrollItemDTO updatePayrollItem(final UpdatePayrollItemCommand command) {
    return payrollItemService.updatePayrollItem(command);
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
    return payrollItemService.updateOrCreatePayrollItemByEmployee(year, month, employeeId, command);
  }

  // 工资明细相关方法已迁移到 PayrollItemService

  /**
   * 提交工资表（待确认）。
   *
   * @param id 工资表ID
   */
  @Transactional
  public void submitPayrollSheet(final Long id) {
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(id, "工资表不存在");

    if (!PayrollStatus.DRAFT.equals(sheet.getStatus())) {
      throw new BusinessException("只有草稿状态的工资表才能提交");
    }

    sheet.setStatus(PayrollStatus.PENDING_CONFIRM);
    sheet.setSubmittedAt(LocalDateTime.now());
    sheet.setSubmittedBy(SecurityUtils.getUserId());
    payrollSheetRepository.updateById(sheet);

    log.info("提交工资表: {}", sheet.getPayrollNo());
  }

  /**
   * 员工确认工资表。
   *
   * @param command 确认命令
   */
  @Transactional
  public void confirmPayrollItem(final ConfirmPayrollCommand command) {
    payrollItemService.confirmPayrollItem(command);
  }

  /**
   * 财务确认工资表 确认所有员工都已确认后，更新工资表状态为财务已确认。
   *
   * @param id 工资表ID
   */
  @Transactional
  public void financeConfirmPayrollSheet(final Long id) {
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(id, "工资表不存在");

    if (!PayrollStatus.PENDING_CONFIRM.equals(sheet.getStatus())
        && !PayrollStatus.CONFIRMED.equals(sheet.getStatus())) {
      throw new BusinessException("工资表状态不允许财务确认");
    }

    // 检查是否所有员工都已确认
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(id);
    long pendingCount =
        items.stream()
            .filter(item -> PayrollStatus.ITEM_PENDING.equals(item.getConfirmStatus()))
            .count();

    if (pendingCount > 0) {
      throw new BusinessException("还有员工未确认工资，无法进行财务确认");
    }

    sheet.setStatus(PayrollStatus.FINANCE_CONFIRMED);
    sheet.setFinanceConfirmedAt(LocalDateTime.now());
    sheet.setFinanceConfirmedBy(SecurityUtils.getUserId());
    payrollSheetRepository.updateById(sheet);

    log.info("财务确认工资表: {}", sheet.getPayrollNo());
  }

  /**
   * 提交审批（财务确认所有员工已确认后，提交给主任或团队负责人审批） 权限：只有 ADMIN/FINANCE 可以提交审批。
   *
   * @param command 提交审批命令
   */
  @Transactional
  public void submitApproval(final SubmitApprovalCommand command) {
    // 权限检查：只有管理员和财务可以提交审批
    checkPayrollEditPermission();

    PayrollSheet sheet =
        payrollSheetRepository.getByIdOrThrow(command.getPayrollSheetId(), "工资表不存在");

    // 只有已确认状态才能提交审批
    if (!PayrollStatus.CONFIRMED.equals(sheet.getStatus())) {
      throw new BusinessException("只有所有员工都已确认的工资表才能提交审批");
    }

    // 检查是否所有员工都已确认
    List<PayrollItem> items =
        payrollItemRepository.findByPayrollSheetId(command.getPayrollSheetId());
    long pendingCount =
        items.stream()
            .filter(item -> PayrollStatus.ITEM_PENDING.equals(item.getConfirmStatus()))
            .count();

    if (pendingCount > 0) {
      throw new BusinessException("还有员工未确认工资，无法提交审批");
    }

    // 验证审批人是否存在且是主任或团队负责人角色
    userRepository.getByIdOrThrow(command.getApproverId(), "审批人不存在");
    List<String> approverRoles = userRepository.findRoleCodesByUserId(command.getApproverId());
    if (!approverRoles.contains("DIRECTOR")
        && !approverRoles.contains("TEAM_LEADER")
        && !approverRoles.contains("ADMIN")) {
      throw new BusinessException("审批人必须是主任、团队负责人或管理员");
    }

    sheet.setStatus(PayrollStatus.PENDING_APPROVAL);
    sheet.setApproverId(command.getApproverId());
    sheet.setSubmittedAt(LocalDateTime.now());
    sheet.setSubmittedBy(SecurityUtils.getUserId());
    payrollSheetRepository.updateById(sheet);

    // 发送通知给审批人
    try {
      String title = String.format("%d年%d月工资表待审批", sheet.getPayrollYear(), sheet.getPayrollMonth());
      String content =
          String.format(
              "财务已提交%d年%d月工资表，请审批。工资表编号：%s",
              sheet.getPayrollYear(), sheet.getPayrollMonth(), sheet.getPayrollNo());
      notificationAppService.sendSystemNotification(
          command.getApproverId(), title, content, "PAYROLL_APPROVAL", sheet.getId());
      log.info(
          "已发送审批通知给审批人: approverId={}, payrollSheetId={}", command.getApproverId(), sheet.getId());
    } catch (Exception e) {
      log.error(
          "发送审批通知失败: approverId={}, payrollSheetId={}", command.getApproverId(), sheet.getId(), e);
    }

    log.info("提交工资表审批: payrollNo={}, approverId={}", sheet.getPayrollNo(), command.getApproverId());
  }

  /**
   * 审批工资表（主任或团队负责人审批）。
   *
   * @param command 审批命令
   */
  @Transactional
  public void approvePayrollSheet(final ApprovePayrollCommand command) {
    PayrollSheet sheet =
        payrollSheetRepository.getByIdOrThrow(command.getPayrollSheetId(), "工资表不存在");

    // 只有待审批状态才能审批
    if (!PayrollStatus.PENDING_APPROVAL.equals(sheet.getStatus())) {
      throw new BusinessException("工资表状态不允许审批");
    }

    // 权限检查：只能审批分配给自己的工资表
    Long currentUserId = SecurityUtils.getUserId();
    if (!currentUserId.equals(sheet.getApproverId())) {
      throw new BusinessException("只能审批分配给自己的工资表");
    }

    if (PayrollStatus.APPROVED.equals(command.getApprovalStatus())) {
      // 审批通过
      sheet.setStatus(PayrollStatus.APPROVED);
      sheet.setApprovedAt(LocalDateTime.now());
      sheet.setApprovedBy(currentUserId);
      sheet.setApprovalComment(command.getApprovalComment());
      log.info("审批通过工资表: payrollNo={}", sheet.getPayrollNo());
    } else if (PayrollStatus.REJECTED.equals(command.getApprovalStatus())) {
      // 审批拒绝
      if (command.getApprovalComment() == null || command.getApprovalComment().trim().isEmpty()) {
        throw new BusinessException("审批拒绝时必须填写意见");
      }
      sheet.setStatus(PayrollStatus.REJECTED);
      sheet.setApprovedAt(LocalDateTime.now());
      sheet.setApprovedBy(currentUserId);
      sheet.setApprovalComment(command.getApprovalComment());
      log.info(
          "审批拒绝工资表: payrollNo={}, comment={}", sheet.getPayrollNo(), command.getApprovalComment());
    } else {
      throw new BusinessException("无效的审批状态");
    }

    payrollSheetRepository.updateById(sheet);
  }

  /**
   * 发放工资（出纳发放） 权限：只有 ADMIN/FINANCE 可以发放工资 问题253修复：添加明确的状态检查防止重复发放。
   *
   * @param command 发放命令
   */
  @Transactional
  public void issuePayroll(final IssuePayrollCommand command) {
    // 权限检查：只有管理员和财务可以发放工资
    checkPayrollEditPermission();

    PayrollSheet sheet =
        payrollSheetRepository.getByIdOrThrow(command.getPayrollSheetId(), "工资表不存在");

    // 检查是否已发放，防止重复发放
    if (PayrollStatus.ISSUED.equals(sheet.getStatus())) {
      throw new BusinessException(
          "该工资表已发放，请勿重复操作。发放时间："
              + (sheet.getIssuedAt() != null
                  ? sheet
                      .getIssuedAt()
                      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  : "未知"));
    }

    // 只有已审批的工资表才能发放
    if (!PayrollStatus.APPROVED.equals(sheet.getStatus())) {
      throw new BusinessException("只有已审批通过的工资表才能发放，当前状态：" + sheet.getStatus());
    }

    sheet.setStatus(PayrollStatus.ISSUED);
    sheet.setIssuedAt(LocalDateTime.now());
    sheet.setIssuedBy(SecurityUtils.getUserId());
    sheet.setPaymentMethod(command.getPaymentMethod());
    sheet.setPaymentVoucherUrl(command.getPaymentVoucherUrl());
    if (command.getRemark() != null) {
      sheet.setRemark(command.getRemark());
    }
    payrollSheetRepository.updateById(sheet);

    log.info("发放工资: {}", sheet.getPayrollNo());
  }

  /**
   * 查询我的工资表 仅显示： 1. 上月及之前的已确认/已发放的工资（CONFIRMED, FINANCE_CONFIRMED, ISSUED状态） 2.
   * 当前待确认的工资（PENDING_CONFIRM状态） 不显示草稿状态的工资（DRAFT）。
   *
   * @param year 年份
   * @param month 月份
   * @return 工资表列表
   */
  public List<PayrollSheetDTO> getMyPayrollSheets(final Integer year, final Integer month) {
    try {
      Long currentUserId = SecurityUtils.getUserId();
      log.debug("查询我的工资表: userId={}, year={}, month={}", currentUserId, year, month);

      // 查询当前用户的员工档案
      Optional<Employee> employeeOpt = employeeRepository.findByUserId(currentUserId);
      if (!employeeOpt.isPresent()) {
        // 如果不是员工，返回空列表
        // 注意：财务角色如果需要发工资，应该创建员工档案
        log.info("用户没有员工档案，返回空工资列表: userId={}", currentUserId);
        return new ArrayList<>();
      }

      Employee employee = employeeOpt.get();
      log.debug("找到员工档案: employeeId={}, employeeNo={}", employee.getId(), employee.getEmployeeNo());

      // 查询工资明细
      List<PayrollItem> items;
      if (year != null && month != null) {
        PayrollItem item =
            payrollItemRepository
                .findByEmployeeIdAndYearMonth(employee.getId(), year, month)
                .orElse(null);
        items = item != null ? List.of(item) : new ArrayList<>();
        log.debug("按年月查询工资明细: year={}, month={}, found={}", year, month, items.size());
      } else {
        // 查询该员工的所有工资明细
        items =
            payrollItemRepository
                .lambdaQuery()
                .eq(PayrollItem::getEmployeeId, employee.getId())
                .eq(PayrollItem::getDeleted, false)
                .orderByDesc(PayrollItem::getCreatedAt)
                .list();
        log.debug("查询所有工资明细: employeeId={}, found={}", employee.getId(), items.size());
      }

      // 获取上月年月（用于判断上月）
      LocalDate now = LocalDate.now();
      LocalDate lastMonth = now.minusMonths(1);
      int lastYear = lastMonth.getYear();
      int lastMonthValue = lastMonth.getMonthValue();

      // 转换为DTO并过滤
      return items.stream()
          .filter(item -> item != null && item.getPayrollSheetId() != null)
          .map(
              item -> {
                try {
                  PayrollSheet sheet =
                      payrollSheetRepository.getByIdOrThrow(item.getPayrollSheetId(), "工资表不存在");
                  PayrollSheetDTO dto = toSheetDTO(sheet);
                  if (dto == null) {
                    return null;
                  }
                  PayrollItemDTO itemDto = payrollItemService.toItemDTO(item);
                  if (itemDto != null) {
                    dto.setItems(List.of(itemDto));
                  } else {
                    dto.setItems(new ArrayList<>());
                  }
                  return dto;
                } catch (Exception e) {
                  log.warn(
                      "转换工资明细DTO失败: payrollItemId={}, payrollSheetId={}",
                      item.getId(),
                      item.getPayrollSheetId(),
                      e);
                  return null;
                }
              })
          .filter(dto -> dto != null)
          .filter(
              dto -> {
                // 过滤条件：
                // 1. 不显示草稿状态的工资（DRAFT）
                if (PayrollStatus.DRAFT.equals(dto.getStatus())) {
                  return false;
                }

                // 2. 显示待确认状态的工资（PENDING_CONFIRM）- 财务发过来要求确认的
                if (PayrollStatus.PENDING_CONFIRM.equals(dto.getStatus())) {
                  return true;
                }

                // 3. 显示上月及之前的已确认/已发放的工资
                Integer sheetYear = dto.getPayrollYear();
                Integer sheetMonth = dto.getPayrollMonth();

                if (sheetYear == null || sheetMonth == null) {
                  return false;
                }

                // 判断是否是上月或之前
                boolean isLastMonthOrEarlier =
                    (sheetYear < lastYear)
                        || (sheetYear == lastYear && sheetMonth <= lastMonthValue);

                // 已确认、财务已确认、已发放状态的工资，且是上月或之前的，才显示
                if (isLastMonthOrEarlier
                    && (PayrollStatus.CONFIRMED.equals(dto.getStatus())
                        || PayrollStatus.FINANCE_CONFIRMED.equals(dto.getStatus())
                        || PayrollStatus.ISSUED.equals(dto.getStatus()))) {
                  return true;
                }

                return false;
              })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error(
          "查询我的工资表失败: userId={}, year={}, month={}", SecurityUtils.getUserId(), year, month, e);
      throw new BusinessException("查询工资表失败: " + e.getMessage());
    }
  }

  /**
   * 查询我的工资表详情（带权限检查） 员工只能查看自己所属的工资表。
   *
   * @param id 工资表ID
   * @return 工资表DTO
   */
  public PayrollSheetDTO getMyPayrollSheetById(final Long id) {
    Long currentUserId = SecurityUtils.getUserId();
    log.debug("查询我的工资表详情: userId={}, payrollSheetId={}", currentUserId, id);

    // 查询当前用户的员工档案
    Optional<Employee> employeeOpt = employeeRepository.findByUserId(currentUserId);
    if (!employeeOpt.isPresent()) {
      throw new BusinessException("您没有员工档案，无法查看工资表");
    }

    Employee employee = employeeOpt.get();

    // 获取工资表
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(id, "工资表不存在");

    // 权限检查：普通员工只能查看包含自己工资明细的工资表
    if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("DIRECTOR", "FINANCE")) {
      // 检查该工资表是否包含当前用户的工资明细
      boolean hasMyPayrollItem =
          payrollItemRepository
              .lambdaQuery()
              .eq(PayrollItem::getPayrollSheetId, id)
              .eq(PayrollItem::getEmployeeId, employee.getId())
              .eq(PayrollItem::getDeleted, false)
              .exists();

      if (!hasMyPayrollItem) {
        log.warn(
            "员工尝试查看不属于自己的工资表: userId={}, employeeId={}, payrollSheetId={}",
            currentUserId,
            employee.getId(),
            id);
        throw new BusinessException("无权查看该工资表");
      }
    }

    // 加载工资表详情
    PayrollSheetDTO dto = toSheetDTO(sheet);

    // 只加载属于当前员工的工资明细
    List<PayrollItem> items;
    if (SecurityUtils.isAdmin() || SecurityUtils.hasAnyRole("DIRECTOR", "FINANCE")) {
      // 管理人员可以看到所有工资明细
      items = payrollItemRepository.findByPayrollSheetId(id);
    } else {
      // 普通员工只能看到自己的工资明细
      items =
          payrollItemRepository
              .lambdaQuery()
              .eq(PayrollItem::getPayrollSheetId, id)
              .eq(PayrollItem::getEmployeeId, employee.getId())
              .eq(PayrollItem::getDeleted, false)
              .list();
    }

    dto.setItems(items.stream().map(payrollItemService::toItemDTO).collect(Collectors.toList()));

    return dto;
  }

  /**
   * 重新计算工资表汇总。
   *
   * @param sheetId 工资表ID
   */
  public void recalculatePayrollSheetSummary(final Long sheetId) {
    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(sheetId);

    BigDecimal totalGross =
        items.stream().map(PayrollItem::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalDeduction =
        items.stream()
            .map(PayrollItem::getDeductionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalNet =
        items.stream().map(PayrollItem::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(sheetId, "工资表不存在");
    sheet.setTotalEmployees(items.size());
    sheet.setTotalGrossAmount(totalGross);
    sheet.setTotalDeductionAmount(totalDeduction);
    sheet.setTotalNetAmount(totalNet);
    payrollSheetRepository.updateById(sheet);
  }

  /**
   * 生成工资表编号。
   *
   * @param year 年份
   * @param month 月份
   * @return 工资表编号
   */
  public String generatePayrollNo(final Integer year, final Integer month) {
    return String.format("PAY-%d%02d", year, month);
  }

  /**
   * 转换为工资表DTO。
   *
   * @param sheet 工资表实体
   * @return 工资表DTO
   */
  private PayrollSheetDTO toSheetDTO(final PayrollSheet sheet) {
    if (sheet == null) {
      return null;
    }

    PayrollSheetDTO dto = new PayrollSheetDTO();
    dto.setId(sheet.getId());
    dto.setPayrollNo(sheet.getPayrollNo());
    dto.setPayrollYear(sheet.getPayrollYear());
    dto.setPayrollMonth(sheet.getPayrollMonth());
    dto.setStatus(sheet.getStatus());
    dto.setStatusName(getStatusName(sheet.getStatus()));
    dto.setTotalEmployees(sheet.getTotalEmployees());
    dto.setTotalGrossAmount(sheet.getTotalGrossAmount());
    dto.setTotalDeductionAmount(sheet.getTotalDeductionAmount());
    dto.setTotalNetAmount(sheet.getTotalNetAmount());
    dto.setConfirmedCount(sheet.getConfirmedCount());
    dto.setSubmittedAt(sheet.getSubmittedAt());
    dto.setSubmittedBy(sheet.getSubmittedBy());
    dto.setFinanceConfirmedAt(sheet.getFinanceConfirmedAt());
    dto.setFinanceConfirmedBy(sheet.getFinanceConfirmedBy());
    dto.setIssuedAt(sheet.getIssuedAt());
    dto.setIssuedBy(sheet.getIssuedBy());

    // 审批相关字段
    dto.setApproverId(sheet.getApproverId());
    if (sheet.getApproverId() != null) {
      try {
        User approver = userRepository.getById(sheet.getApproverId());
        if (approver != null) {
          dto.setApproverName(
              approver.getRealName() != null ? approver.getRealName() : approver.getUsername());
        }
      } catch (Exception e) {
        log.warn("获取审批人信息失败: approverId={}", sheet.getApproverId(), e);
      }
    }
    dto.setApprovedAt(sheet.getApprovedAt());
    dto.setApprovedBy(sheet.getApprovedBy());
    if (sheet.getApprovedBy() != null) {
      try {
        User approvedByUser = userRepository.getById(sheet.getApprovedBy());
        if (approvedByUser != null) {
          dto.setApprovedByName(
              approvedByUser.getRealName() != null
                  ? approvedByUser.getRealName()
                  : approvedByUser.getUsername());
        }
      } catch (Exception e) {
        log.warn("获取审批人信息失败: approvedBy={}", sheet.getApprovedBy(), e);
      }
    }
    dto.setApprovalComment(sheet.getApprovalComment());
    dto.setPaymentMethod(sheet.getPaymentMethod());
    dto.setPaymentMethodName(getPaymentMethodName(sheet.getPaymentMethod()));
    dto.setPaymentVoucherUrl(sheet.getPaymentVoucherUrl());
    dto.setRemark(sheet.getRemark());
    dto.setAutoConfirmDeadline(sheet.getAutoConfirmDeadline());
    dto.setCreatedAt(sheet.getCreatedAt());
    dto.setUpdatedAt(sheet.getUpdatedAt());

    // 加载被拒绝的工资明细（财务可以查看未确认的理由）
    List<PayrollItem> rejectedItems = new ArrayList<>();
    if (sheet.getId() != null) {
      try {
        rejectedItems =
            payrollItemRepository
                .lambdaQuery()
                .eq(PayrollItem::getPayrollSheetId, sheet.getId())
                .eq(PayrollItem::getConfirmStatus, PayrollStatus.ITEM_REJECTED)
                .eq(PayrollItem::getDeleted, false)
                .list();
      } catch (Exception e) {
        log.warn("查询被拒绝的工资明细失败: sheetId={}", sheet.getId(), e);
      }
    }
    dto.setRejectedItems(
        rejectedItems.stream()
            .map(payrollItemService::toItemDTO)
            .filter(item -> item != null)
            .collect(Collectors.toList()));

    return dto;
  }

  // toItemDTO、toIncomeDTO、toDeductionDTO 已迁移到 PayrollItemService

  private String getStatusName(final String status) {
    return PayrollStatus.getStatusName(status);
  }

  /**
   * 导出工资表为Excel（仅已审批通过的工资表可以导出）。
   *
   * @param id 工资表ID
   * @return Excel文件输入流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream exportPayrollSheet(final Long id) throws IOException {
    PayrollSheet sheet = payrollSheetRepository.getByIdOrThrow(id, "工资表不存在");

    // 只有已审批或已发放的工资表才能导出
    if (!PayrollStatus.APPROVED.equals(sheet.getStatus())
        && !PayrollStatus.ISSUED.equals(sheet.getStatus())) {
      throw new BusinessException("只有已审批通过的工资表才能导出");
    }

    List<PayrollItem> items = payrollItemRepository.findByPayrollSheetId(id);

    // 批量获取所有工资明细的ID
    List<Long> itemIds = items.stream().map(PayrollItem::getId).collect(Collectors.toList());

    // 批量查询所有扣减项和收入项（避免N+1查询）
    Map<Long, List<PayrollDeduction>> deductionsByItemId =
        payrollDeductionRepository.findByPayrollItemIdsGrouped(itemIds);
    Map<Long, List<PayrollIncome>> incomesByItemId =
        payrollIncomeRepository.findByPayrollItemIdsGrouped(itemIds);

    // 构建表头
    List<String> headers =
        List.of("工号", "姓名", "收入（提成总额）", "税费扣减", "应发工资（税后）", "其他扣减", "实发工资", "确认状态", "确认时间", "确认意见");

    // 构建数据
    List<List<Object>> data = new ArrayList<>();
    for (PayrollItem item : items) {
      // 从批量查询结果中获取扣减项
      List<PayrollDeduction> deductions =
          deductionsByItemId.getOrDefault(item.getId(), java.util.Collections.emptyList());
      BigDecimal taxDeductionAmount =
          deductions.stream()
              .filter(d -> isTaxDeduction(d.getDeductionType()))
              .map(PayrollDeduction::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal otherDeductionAmount =
          deductions.stream()
              .filter(d -> !isTaxDeduction(d.getDeductionType()))
              .map(PayrollDeduction::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      // 从批量查询结果中获取收入项
      List<PayrollIncome> incomes =
          incomesByItemId.getOrDefault(item.getId(), java.util.Collections.emptyList());
      BigDecimal incomeTotal =
          incomes.stream().map(PayrollIncome::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

      List<Object> row = new ArrayList<>();
      row.add(item.getEmployeeNo());
      row.add(item.getEmployeeName());
      row.add(incomeTotal);
      row.add(taxDeductionAmount);
      row.add(item.getGrossAmount());
      row.add(otherDeductionAmount);
      row.add(item.getNetAmount());
      row.add(getConfirmStatusName(item.getConfirmStatus()));
      row.add(item.getConfirmedAt());
      row.add(item.getConfirmComment());
      data.add(row);
    }

    String sheetName = String.format("%d年%d月工资表", sheet.getPayrollYear(), sheet.getPayrollMonth());
    return excelImportExportService.createExcel(headers, data, sheetName);
  }

  private String getConfirmStatusName(final String status) {
    return PayrollStatus.getConfirmStatusName(status);
  }

  private String getPaymentMethodName(final String method) {
    if (method == null) {
      return "";
    }
    return switch (method) {
      case "BANK_TRANSFER" -> "银行转账";
      case "CASH" -> "现金";
      case "OTHER" -> "其他";
      default -> method;
    };
  }

  /**
   * 判断是否为税费类扣减项 税费类：个人所得税、社保、公积金 其他类：其他扣款、预支等。
   *
   * @param deductionType 扣减类型
   * @return 是否为税费类扣减项
   */
  private boolean isTaxDeduction(final String deductionType) {
    // 此方法在 exportPayrollSheet 中使用，暂时保留
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

  // ========== 权限检查方法 ==========

  /** 检查是否有工资表查看权限（ADMIN/DIRECTOR/FINANCE） */
  private void checkPayrollViewPermission() {
    if (SecurityUtils.isAdmin()) {
      return;
    }
    Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("DIRECTOR") && !roles.contains("FINANCE")) {
      throw new BusinessException("只有管理员、主任和财务可以查看工资表");
    }
  }

  /** 检查是否有工资表编辑权限（ADMIN/FINANCE） */
  private void checkPayrollEditPermission() {
    if (SecurityUtils.isAdmin()) {
      return;
    }
    Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("FINANCE")) {
      throw new BusinessException("只有管理员和财务可以操作工资表");
    }
  }

  // sendPayrollUpdateNotification 已迁移到 PayrollItemService

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
    return payrollItemService.addPayrollItemForEmployee(payrollSheetId, command);
  }
}
