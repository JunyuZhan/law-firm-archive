package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.dto.TimesheetDTO;
import com.lawfirm.application.matter.dto.TimesheetQueryDTO;
import com.lawfirm.application.matter.dto.TimesheetSummaryDTO;
import com.lawfirm.common.constant.TimesheetStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.HourlyRate;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.domain.matter.repository.HourlyRateRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TimesheetRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 工时应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetAppService {

  /** 工时仓储. */
  private final TimesheetRepository timesheetRepository;

  /** 小时费率仓储. */
  private final HourlyRateRepository hourlyRateRepository;

  /** 工时Mapper. */
  private final TimesheetMapper timesheetMapper;

  /** 项目仓储. */
  private final MatterRepository matterRepository;

  /** 项目参与人仓储. */
  private final MatterParticipantRepository matterParticipantRepository;

  /** 部门Mapper. */
  private final DepartmentMapper departmentMapper;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /**
   * 分页查询工时.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<TimesheetDTO> listTimesheets(final TimesheetQueryDTO query) {
    // 根据用户权限过滤数据
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    // 获取可访问的项目ID列表
    List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

    // 如果返回空列表，表示没有权限，返回空结果
    if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    // 如果query中指定了matterId，需要验证是否有权限访问该项目
    if (query.getMatterId() != null && accessibleMatterIds != null) {
      if (!accessibleMatterIds.contains(query.getMatterId())) {
        // 没有权限访问指定的项目，返回空结果
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
      }
    }

    // 如果query中指定了userId，需要验证权限
    // SELF权限时，只能查看自己的工时
    if (query.getUserId() != null && "SELF".equals(dataScope)) {
      if (!query.getUserId().equals(currentUserId)) {
        // 没有权限查看他人的工时，返回空结果
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
      }
    }

    IPage<Timesheet> page =
        timesheetMapper.selectTimesheetPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getMatterId(),
            query.getUserId(),
            query.getWorkType(),
            query.getStatus(),
            query.getStartDate(),
            query.getEndDate(),
            query.getBillable(),
            accessibleMatterIds // null表示可以访问所有项目的工时（ALL权限）
            );

    List<TimesheetDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建工时记录.
   *
   * @param command 创建命令
   * @return 工时记录DTO
   */
  @Transactional
  public TimesheetDTO createTimesheet(final CreateTimesheetCommand command) {
    Long userId = SecurityUtils.getUserId();
    String timesheetNo = generateTimesheetNo();

    // 获取小时费率
    BigDecimal hourlyRate = command.getHourlyRate();
    if (hourlyRate == null) {
      HourlyRate rate = hourlyRateRepository.findCurrentRate(userId, command.getWorkDate());
      hourlyRate = rate != null ? rate.getRate() : BigDecimal.ZERO;
    }

    // 计算金额
    boolean billable = command.getBillable() != null ? command.getBillable() : true;
    BigDecimal amount = billable ? command.getHours().multiply(hourlyRate) : BigDecimal.ZERO;

    Timesheet timesheet =
        Timesheet.builder()
            .timesheetNo(timesheetNo)
            .matterId(command.getMatterId())
            .userId(userId)
            .workDate(command.getWorkDate())
            .hours(command.getHours())
            .workType(command.getWorkType())
            .workContent(command.getWorkContent())
            .billable(billable)
            .hourlyRate(hourlyRate)
            .amount(amount)
            .status(TimesheetStatus.DRAFT)
            .build();

    timesheetRepository.save(timesheet);
    log.info("工时记录创建成功: {} ({}小时)", timesheet.getTimesheetNo(), timesheet.getHours());
    return toDTO(timesheet);
  }

  /**
   * 获取工时详情.
   *
   * @param id 工时ID
   * @return 工时记录DTO
   */
  public TimesheetDTO getTimesheetById(final Long id) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");
    return toDTO(timesheet);
  }

  /**
   * 更新工时记录.
   *
   * @param id 工时ID
   * @param workDate 工作日期
   * @param hours 工时数
   * @param workType 工作类型
   * @param workContent 工作内容
   * @param billable 是否计费
   * @return 工时记录DTO
   */
  @Transactional
  public TimesheetDTO updateTimesheet(
      final Long id,
      final LocalDate workDate,
      final BigDecimal hours,
      final String workType,
      final String workContent,
      final Boolean billable) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

    // 只有草稿状态可以修改
    if (!TimesheetStatus.canModify(timesheet.getStatus())) {
      throw new BusinessException("只有草稿状态的工时记录可以修改");
    }

    // 验证是否是本人的记录
    if (!timesheet.getUserId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能修改自己的工时记录");
    }

    if (workDate != null) {
      timesheet.setWorkDate(workDate);
    }
    if (hours != null) {
      timesheet.setHours(hours);
      // 重新计算金额
      if (timesheet.getBillable() && timesheet.getHourlyRate() != null) {
        timesheet.setAmount(hours.multiply(timesheet.getHourlyRate()));
      }
    }
    if (StringUtils.hasText(workType)) {
      timesheet.setWorkType(workType);
    }
    if (StringUtils.hasText(workContent)) {
      timesheet.setWorkContent(workContent);
    }
    if (billable != null) {
      timesheet.setBillable(billable);
      if (!billable) {
        timesheet.setAmount(BigDecimal.ZERO);
      } else if (timesheet.getHourlyRate() != null) {
        timesheet.setAmount(timesheet.getHours().multiply(timesheet.getHourlyRate()));
      }
    }

    timesheetRepository.updateById(timesheet);
    log.info("工时记录更新成功: {}", timesheet.getTimesheetNo());
    return toDTO(timesheet);
  }

  /**
   * 删除工时记录.
   *
   * @param id 工时ID
   */
  @Transactional
  public void deleteTimesheet(final Long id) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

    // 只有草稿状态可以删除
    if (!TimesheetStatus.canModify(timesheet.getStatus())) {
      throw new BusinessException("只有草稿状态的工时记录可以删除");
    }

    // 验证是否是本人的记录
    if (!timesheet.getUserId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能删除自己的工时记录");
    }

    timesheetRepository.removeById(id);
    log.info("工时记录删除成功: {}", timesheet.getTimesheetNo());
  }

  /**
   * 提交工时.
   *
   * @param id 工时ID
   * @return 工时记录DTO
   */
  @Transactional
  public TimesheetDTO submitTimesheet(final Long id) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

    if (!TimesheetStatus.canSubmit(timesheet.getStatus())) {
      throw new BusinessException("只有草稿状态的工时记录可以提交");
    }

    timesheet.setStatus(TimesheetStatus.SUBMITTED);
    timesheet.setSubmittedAt(LocalDateTime.now());
    timesheetRepository.updateById(timesheet);

    log.info("工时记录已提交: {}", timesheet.getTimesheetNo());
    return toDTO(timesheet);
  }

  /**
   * 批量提交工时（全部成功或全部失败） 修复：先验证所有记录，再批量更新，保证事务原子性.
   *
   * @param ids 工时ID列表
   * @return 批量提交结果
   */
  @Transactional(rollbackFor = Exception.class)
  public BatchSubmitResult batchSubmit(final List<Long> ids) {
    Long userId = SecurityUtils.getUserId();

    if (ids == null || ids.isEmpty()) {
      throw new BusinessException("请选择要提交的工时记录");
    }

    // 第1阶段：验证所有工时记录
    List<Timesheet> timesheets = new ArrayList<>();
    for (Long id : ids) {
      Timesheet timesheet = timesheetRepository.findById(id);
      if (timesheet == null) {
        throw new BusinessException(String.format("工时记录%d不存在", id));
      }
      if (!TimesheetStatus.canSubmit(timesheet.getStatus())) {
        throw new BusinessException(String.format("工时记录%d状态不是草稿，无法提交", id));
      }
      if (!timesheet.getUserId().equals(userId)) {
        throw new BusinessException(String.format("工时记录%d不属于当前用户，无法提交", id));
      }
      timesheets.add(timesheet);
    }

    // 第2阶段：批量更新（所有验证通过后）
    List<Long> successIds = new ArrayList<>();
    for (Timesheet timesheet : timesheets) {
      timesheet.setStatus(TimesheetStatus.SUBMITTED);
      timesheet.setSubmittedAt(LocalDateTime.now());
      timesheetRepository.updateById(timesheet);
      successIds.add(timesheet.getId());
    }

    log.info("批量提交工时完成: 成功{}条", successIds.size());

    BatchSubmitResult result = new BatchSubmitResult();
    result.setSuccessCount(successIds.size());
    result.setSuccessIds(successIds);
    result.setFailureCount(0);
    result.setFailureReasons(java.util.Collections.emptyList());

    return result;
  }

  /** 批量提交结果. */
  @lombok.Data
  public static class BatchSubmitResult {
    /** 成功数量. */
    private int successCount;

    /** 成功的ID列表. */
    private List<Long> successIds;

    /** 失败数量. */
    private int failureCount;

    /** 失败原因列表. */
    private List<String> failureReasons;
  }

  /**
   * 审批通过.
   *
   * @param id 工时ID
   * @param comment 审批意见
   * @return 工时记录DTO
   */
  @Transactional
  public TimesheetDTO approveTimesheet(final Long id, final String comment) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

    if (!TimesheetStatus.canApprove(timesheet.getStatus())) {
      throw new BusinessException("只能审批已提交的工时记录");
    }

    // 验证审批权限
    validateApprovalPermission(timesheet);

    timesheet.setStatus(TimesheetStatus.APPROVED);
    timesheet.setApprovedBy(SecurityUtils.getUserId());
    timesheet.setApprovedAt(LocalDateTime.now());
    timesheet.setApprovalComment(comment);
    timesheetRepository.updateById(timesheet);

    log.info("工时记录审批通过: {}", timesheet.getTimesheetNo());
    return toDTO(timesheet);
  }

  /**
   * 审批拒绝.
   *
   * @param id 工时ID
   * @param comment 审批意见
   * @return 工时记录DTO
   */
  @Transactional
  public TimesheetDTO rejectTimesheet(final Long id, final String comment) {
    Timesheet timesheet = timesheetRepository.getByIdOrThrow(id, "工时记录不存在");

    if (!TimesheetStatus.canApprove(timesheet.getStatus())) {
      throw new BusinessException("只能审批已提交的工时记录");
    }

    // 验证审批权限
    validateApprovalPermission(timesheet);

    timesheet.setStatus(TimesheetStatus.REJECTED);
    timesheet.setApprovedBy(SecurityUtils.getUserId());
    timesheet.setApprovedAt(LocalDateTime.now());
    timesheet.setApprovalComment(comment);
    timesheetRepository.updateById(timesheet);

    log.info("工时记录审批拒绝: {}", timesheet.getTimesheetNo());
    return toDTO(timesheet);
  }

  /**
   * 获取待审批列表.
   *
   * @return 工时记录DTO列表
   */
  public List<TimesheetDTO> getPendingApproval() {
    List<Timesheet> timesheets = timesheetRepository.findPendingApproval();
    return timesheets.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取我的工时（按日期范围）.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工时记录DTO列表
   */
  public List<TimesheetDTO> getMyTimesheets(final LocalDate startDate, final LocalDate endDate) {
    Long userId = SecurityUtils.getUserId();
    List<Timesheet> timesheets =
        timesheetRepository.findByUserAndDateRange(userId, startDate, endDate);
    return timesheets.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取用户月度工时汇总.
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 汇总信息
   */
  public TimesheetSummaryDTO getUserMonthlySummary(
      final Long userId, final int year, final int month) {
    BigDecimal totalHours = timesheetRepository.sumHoursByUserAndMonth(userId, year, month);

    TimesheetSummaryDTO summary = new TimesheetSummaryDTO();
    summary.setUserId(userId);
    summary.setYear(year);
    summary.setMonth(month);
    summary.setTotalHours(totalHours);
    return summary;
  }

  /**
   * 获取案件工时汇总.
   *
   * @param matterId 案件ID
   * @return 汇总信息
   */
  public TimesheetSummaryDTO getMatterSummary(final Long matterId) {
    BigDecimal totalHours = timesheetRepository.sumHoursByMatter(matterId);

    TimesheetSummaryDTO summary = new TimesheetSummaryDTO();
    summary.setMatterId(matterId);
    summary.setTotalHours(totalHours);
    return summary;
  }

  /**
   * 生成工时编号.
   *
   * @return 工时编号
   */
  private String generateTimesheetNo() {
    String prefix = "TS" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取工作类型名称.
   *
   * @param type 类型
   * @return 名称
   */
  private String getWorkTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "RESEARCH" -> "法律研究";
      case "DRAFTING" -> "文书起草";
      case "MEETING" -> "会议";
      case "COURT" -> "出庭";
      case "NEGOTIATION" -> "谈判";
      case "COMMUNICATION" -> "沟通";
      case "TRAVEL" -> "差旅";
      case "OTHER" -> "其他";
      default -> type;
    };
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态
   * @return 名称
   */
  private String getStatusName(final String status) {
    return TimesheetStatus.getStatusName(status);
  }

  /**
   * 验证用户是否有审批工时的权限 可以审批的人员： 1. 管理员 - 可以审批所有工时 2. 主任(dataScope=ALL) - 可以审批所有工时 3.
   * 团队负责人(dataScope=DEPT_AND_CHILD) - 可以审批本团队项目的工时 4. 项目负责人 - 可以审批该项目成员的工时.
   *
   * @param timesheet 工时记录
   * @throws BusinessException 如果无权审批
   */
  private void validateApprovalPermission(final Timesheet timesheet) {
    // 管理员可以审批所有工时
    if (SecurityUtils.isAdmin()) {
      return;
    }

    Long currentUserId = SecurityUtils.getUserId();
    String dataScope = SecurityUtils.getDataScope();
    Long deptId = SecurityUtils.getDepartmentId();

    // 主任（ALL权限）可以审批所有工时
    if ("ALL".equals(dataScope)) {
      return;
    }

    // 获取工时对应的项目
    Matter matter = matterRepository.findById(timesheet.getMatterId());
    if (matter == null) {
      throw new BusinessException("工时对应的项目不存在");
    }

    // 项目负责人可以审批项目成员的工时
    if (currentUserId.equals(matter.getLeadLawyerId())) {
      return;
    }

    // 团队负责人可以审批本团队项目的工时
    if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
      if (deptId.equals(matter.getDepartmentId())) {
        return;
      }
    }

    throw new BusinessException("您没有审批此工时的权限，只有管理员、主任、团队负责人或项目负责人可以审批");
  }

  /** Entity 转 DTO. */
  /**
   * 获取部门及所有下级部门ID列表 使用递归CTE一次性查询.
   *
   * @param deptId 部门ID
   * @return ID列表
   */
  private List<Long> getAllDepartmentIds(final Long deptId) {
    if (deptId == null) {
      return new ArrayList<>();
    }
    List<Long> result = new ArrayList<>();
    result.add(deptId); // 包含自身

    try {
      List<Long> descendantIds = departmentMapper.selectAllDescendantDeptIds(deptId);
      if (descendantIds != null) {
        result.addAll(descendantIds);
      }
    } catch (Exception e) {
      log.warn("查询子部门失败: deptId={}, error={}", deptId, e.getMessage());
    }
    return result;
  }

  /**
   * 获取可访问的项目ID列表（根据数据权限）.
   *
   * @param dataScope 数据范围
   * @param currentUserId 当前用户ID
   * @param deptId 部门ID
   * @return null表示可以访问所有项目，否则返回可访问的项目ID列表
   */
  private List<Long> getAccessibleMatterIds(
      final String dataScope, final Long currentUserId, final Long deptId) {
    if ("ALL".equals(dataScope)) {
      return null; // null表示可以访问所有项目
    }

    List<Long> matterIds = new ArrayList<>();

    if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
      // 部门及下级部门：使用递归CTE查询所有下级部门的项目
      List<Long> allDeptIds = getAllDepartmentIds(deptId);
      matterIds =
          matterRepository
              .lambdaQuery()
              .select(Matter::getId)
              .eq(Matter::getDeleted, false)
              .in(Matter::getDepartmentId, allDeptIds)
              .list()
              .stream()
              .map(Matter::getId)
              .collect(Collectors.toList());
    } else if ("DEPT".equals(dataScope) && deptId != null) {
      // 本部门：查询本部门的项目
      matterIds =
          matterRepository
              .lambdaQuery()
              .select(Matter::getId)
              .eq(Matter::getDeleted, false)
              .eq(Matter::getDepartmentId, deptId)
              .list()
              .stream()
              .map(Matter::getId)
              .collect(Collectors.toList());
    } else {
      // SELF：只查看自己负责的项目或参与的项目
      // 查询自己负责的项目
      List<Long> leadMatterIds =
          matterRepository
              .lambdaQuery()
              .select(Matter::getId)
              .eq(Matter::getDeleted, false)
              .eq(Matter::getLeadLawyerId, currentUserId)
              .list()
              .stream()
              .map(Matter::getId)
              .collect(Collectors.toList());

      // 查询自己参与的项目
      var participantList =
          matterParticipantRepository
              .lambdaQuery()
              .select(MatterParticipant::getMatterId)
              .eq(MatterParticipant::getUserId, currentUserId)
              .eq(MatterParticipant::getDeleted, false)
              .list();

      List<Long> participantMatterIds =
          participantList.stream()
              .map(MatterParticipant::getMatterId)
              .distinct()
              .collect(Collectors.toList());

      // 合并去重
      matterIds.addAll(leadMatterIds);
      matterIds.addAll(participantMatterIds);
      matterIds = matterIds.stream().distinct().collect(Collectors.toList());
    }

    return matterIds.isEmpty() ? Collections.emptyList() : matterIds;
  }

  /**
   * Entity 转 DTO.
   *
   * @param timesheet 工时记录
   * @return DTO
   */
  private TimesheetDTO toDTO(final Timesheet timesheet) {
    TimesheetDTO dto = new TimesheetDTO();
    dto.setId(timesheet.getId());
    dto.setTimesheetNo(timesheet.getTimesheetNo());
    dto.setMatterId(timesheet.getMatterId());
    dto.setUserId(timesheet.getUserId());

    // 填充项目名称
    if (timesheet.getMatterId() != null) {
      Matter matter = matterRepository.findById(timesheet.getMatterId());
      if (matter != null) {
        dto.setMatterName(matter.getName());
        dto.setMatterNo(matter.getMatterNo());
      }
    }

    // 填充用户名称
    if (timesheet.getUserId() != null) {
      User user = userRepository.findById(timesheet.getUserId());
      if (user != null) {
        dto.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
      }
    }

    dto.setWorkDate(timesheet.getWorkDate());
    dto.setHours(timesheet.getHours());
    dto.setWorkType(timesheet.getWorkType());
    dto.setWorkTypeName(getWorkTypeName(timesheet.getWorkType()));
    dto.setWorkContent(timesheet.getWorkContent());
    dto.setBillable(timesheet.getBillable());
    dto.setHourlyRate(timesheet.getHourlyRate());
    dto.setAmount(timesheet.getAmount());
    dto.setStatus(timesheet.getStatus());
    dto.setStatusName(getStatusName(timesheet.getStatus()));
    dto.setSubmittedAt(timesheet.getSubmittedAt());
    dto.setApprovedBy(timesheet.getApprovedBy());
    dto.setApprovedAt(timesheet.getApprovedAt());
    dto.setApprovalComment(timesheet.getApprovalComment());
    dto.setCreatedAt(timesheet.getCreatedAt());
    dto.setUpdatedAt(timesheet.getUpdatedAt());
    return dto;
  }
}
