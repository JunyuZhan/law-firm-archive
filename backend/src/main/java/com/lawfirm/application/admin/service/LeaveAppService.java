package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.ApplyLeaveCommand;
import com.lawfirm.application.admin.command.ApproveLeaveCommand;
import com.lawfirm.application.admin.dto.LeaveApplicationDTO;
import com.lawfirm.application.admin.dto.LeaveApplicationQueryDTO;
import com.lawfirm.application.admin.dto.LeaveBalanceDTO;
import com.lawfirm.application.admin.dto.LeaveTypeDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.LeaveApplication;
import com.lawfirm.domain.admin.entity.LeaveBalance;
import com.lawfirm.domain.admin.entity.LeaveType;
import com.lawfirm.domain.admin.repository.LeaveApplicationRepository;
import com.lawfirm.domain.admin.repository.LeaveBalanceRepository;
import com.lawfirm.domain.admin.repository.LeaveTypeRepository;
import com.lawfirm.infrastructure.persistence.mapper.LeaveApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveBalanceMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveTypeMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 请假应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveAppService {

  /** 请假类型仓储 */
  private final LeaveTypeRepository leaveTypeRepository;

  /** 请假类型Mapper */
  private final LeaveTypeMapper leaveTypeMapper;

  /** 请假申请仓储 */
  private final LeaveApplicationRepository leaveApplicationRepository;

  /** 请假申请Mapper */
  private final LeaveApplicationMapper leaveApplicationMapper;

  /** 请假余额仓储 */
  private final LeaveBalanceRepository leaveBalanceRepository;

  /** 请假余额Mapper */
  private final LeaveBalanceMapper leaveBalanceMapper;

  /**
   * 获取所有请假类型
   *
   * @return 请假类型列表
   */
  public List<LeaveTypeDTO> listLeaveTypes() {
    return leaveTypeMapper.selectEnabledTypes().stream()
        .map(this::toLeaveTypeDTO)
        .collect(Collectors.toList());
  }

  /**
   * 分页查询请假申请 ✅ 优化：使用批量加载避免N+1查询
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<LeaveApplicationDTO> listApplications(final LeaveApplicationQueryDTO query) {
    IPage<LeaveApplication> page =
        leaveApplicationMapper.selectApplicationPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getUserId(),
            query.getLeaveTypeId(),
            query.getStatus(),
            query.getStartTime(),
            query.getEndTime());

    List<LeaveApplication> applications = page.getRecords();
    if (applications.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 批量加载请假类型
    Map<Long, LeaveType> typeMap = batchLoadLeaveTypes(applications);

    List<LeaveApplicationDTO> records =
        applications.stream()
            .map(app -> toApplicationDTO(app, typeMap))
            .collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 批量加载请假类型
   *
   * @param applications 请假申请列表
   * @return 请假类型Map
   */
  private Map<Long, LeaveType> batchLoadLeaveTypes(final List<LeaveApplication> applications) {
    Set<Long> typeIds =
        applications.stream()
            .map(LeaveApplication::getLeaveTypeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (typeIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return leaveTypeRepository.listByIds(new ArrayList<>(typeIds)).stream()
        .collect(Collectors.toMap(LeaveType::getId, t -> t));
  }

  /**
   * 提交请假申请
   *
   * @param command 申请命令
   * @return 请假申请DTO
   */
  @Transactional
  public LeaveApplicationDTO applyLeave(final ApplyLeaveCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 验证请假类型
    LeaveType leaveType = leaveTypeRepository.getByIdOrThrow(command.getLeaveTypeId(), "请假类型不存在");
    if (!leaveType.getEnabled()) {
      throw new BusinessException("该请假类型已禁用");
    }

    // 验证时间
    if (command.getStartTime().isAfter(command.getEndTime())) {
      throw new BusinessException("开始时间不能晚于结束时间");
    }
    if (command.getStartTime().isBefore(LocalDateTime.now())) {
      throw new BusinessException("开始时间不能早于当前时间");
    }

    // 检查时间段是否有重叠
    int overlapping =
        leaveApplicationMapper.countOverlapping(
            userId, command.getStartTime(), command.getEndTime());
    if (overlapping > 0) {
      throw new BusinessException("该时间段已有请假申请");
    }

    // 检查假期余额（如果有限额）
    if (leaveType.getAnnualLimit() != null) {
      int year = command.getStartTime().getYear();
      LeaveBalance balance =
          leaveBalanceMapper.selectByUserTypeYear(userId, leaveType.getId(), year);
      if (balance == null) {
        throw new BusinessException("您没有该类型的假期余额，请联系管理员");
      }
      if (balance.getRemainingDays().compareTo(command.getDuration()) < 0) {
        throw new BusinessException("假期余额不足，剩余" + balance.getRemainingDays() + "天");
      }
    }

    // 生成申请编号
    String applicationNo = generateApplicationNo();

    // 创建申请
    LeaveApplication application =
        LeaveApplication.builder()
            .applicationNo(applicationNo)
            .userId(userId)
            .leaveTypeId(command.getLeaveTypeId())
            .startTime(command.getStartTime())
            .endTime(command.getEndTime())
            .duration(command.getDuration())
            .reason(command.getReason())
            .attachmentUrl(command.getAttachmentUrl())
            .status(LeaveApplication.STATUS_PENDING)
            .build();

    leaveApplicationRepository.save(application);
    log.info("请假申请提交成功: {} ({})", applicationNo, leaveType.getName());
    return toApplicationDTO(application);
  }

  /**
   * 审批请假申请 ✅ 修复：先扣减余额再更新申请状态，避免并发问题
   *
   * @param command 审批命令
   * @return 请假申请DTO
   */
  @Transactional
  public LeaveApplicationDTO approveLeave(final ApproveLeaveCommand command) {
    Long approverId = SecurityUtils.getUserId();

    LeaveApplication application =
        leaveApplicationRepository.getByIdOrThrow(command.getApplicationId(), "请假申请不存在");

    if (!LeaveApplication.STATUS_PENDING.equals(application.getStatus())) {
      throw new BusinessException("该申请已处理");
    }

    // ✅ 先扣减余额，再更新申请状态
    if (command.getApproved()) {
      LeaveType leaveType = leaveTypeRepository.getById(application.getLeaveTypeId());
      if (leaveType != null && leaveType.getAnnualLimit() != null) {
        int year = application.getStartTime().getYear();
        int updated =
            leaveBalanceMapper.deductBalance(
                application.getUserId(),
                application.getLeaveTypeId(),
                year,
                application.getDuration());
        if (updated == 0) {
          throw new BusinessException("扣减假期余额失败，余额不足");
        }
      }

      // 余额扣减成功后，才更新申请状态
      application.setStatus(LeaveApplication.STATUS_APPROVED);
      log.info("请假申请已批准: {}", application.getApplicationNo());
    } else {
      application.setStatus(LeaveApplication.STATUS_REJECTED);
      log.info("请假申请已拒绝: {}", application.getApplicationNo());
    }

    application.setApproverId(approverId);
    application.setApprovedAt(LocalDateTime.now());
    application.setApprovalComment(command.getComment());

    leaveApplicationRepository.updateById(application);

    // 使用批量加载方式获取类型信息
    Map<Long, LeaveType> typeMap = new HashMap<>();
    LeaveType type = leaveTypeRepository.getById(application.getLeaveTypeId());
    if (type != null) {
      typeMap.put(type.getId(), type);
    }
    return toApplicationDTO(application, typeMap);
  }

  /**
   * 取消请假申请
   *
   * @param applicationId 申请ID
   */
  @Transactional
  public void cancelApplication(final Long applicationId) {
    Long userId = SecurityUtils.getUserId();

    LeaveApplication application =
        leaveApplicationRepository.getByIdOrThrow(applicationId, "请假申请不存在");

    if (!application.getUserId().equals(userId)) {
      throw new BusinessException("只能取消自己的申请");
    }
    if (!LeaveApplication.STATUS_PENDING.equals(application.getStatus())) {
      throw new BusinessException("只能取消待审批的申请");
    }

    application.setStatus(LeaveApplication.STATUS_CANCELLED);
    leaveApplicationRepository.updateById(application);
    log.info("请假申请已取消: {}", application.getApplicationNo());
  }

  /**
   * 获取用户假期余额 ✅ 优化：使用批量加载避免N+1查询
   *
   * @param userId 用户ID
   * @param year 年份
   * @return 假期余额列表
   */
  public List<LeaveBalanceDTO> getUserBalance(final Long userId, final Integer year) {
    Long finalUserId = userId;
    if (finalUserId == null) {
      finalUserId = SecurityUtils.getUserId();
    }
    Integer finalYear = year;
    if (finalYear == null) {
      finalYear = LocalDate.now().getYear();
    }

    List<LeaveBalance> balances = leaveBalanceMapper.selectByUserAndYear(finalUserId, finalYear);
    if (balances.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载请假类型
    Map<Long, LeaveType> typeMap = batchLoadLeaveTypesForBalances(balances);

    return balances.stream()
        .map(balance -> toBalanceDTO(balance, typeMap))
        .collect(Collectors.toList());
  }

  /**
   * 批量加载请假类型（用于余额查询）
   *
   * @param balances 假期余额列表
   * @return 请假类型Map
   */
  private Map<Long, LeaveType> batchLoadLeaveTypesForBalances(final List<LeaveBalance> balances) {
    Set<Long> typeIds =
        balances.stream()
            .map(LeaveBalance::getLeaveTypeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (typeIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return leaveTypeRepository.listByIds(new ArrayList<>(typeIds)).stream()
        .collect(Collectors.toMap(LeaveType::getId, t -> t));
  }

  /**
   * 初始化用户年度假期余额 ✅ 优化：使用批量插入替代循环插入
   *
   * @param userId 用户ID
   * @param year 年份
   */
  @Transactional
  public void initUserBalance(final Long userId, final Integer year) {
    List<LeaveType> types = leaveTypeMapper.selectEnabledTypes();

    // 批量查询已有余额
    List<LeaveBalance> existingBalances = leaveBalanceMapper.selectByUserAndYear(userId, year);
    Set<Long> existingTypeIds =
        existingBalances.stream().map(LeaveBalance::getLeaveTypeId).collect(Collectors.toSet());

    // 收集需要创建的余额
    List<LeaveBalance> toCreate = new ArrayList<>();
    for (LeaveType type : types) {
      if (type.getAnnualLimit() != null && !existingTypeIds.contains(type.getId())) {
        LeaveBalance balance =
            LeaveBalance.builder()
                .userId(userId)
                .leaveTypeId(type.getId())
                .year(year)
                .totalDays(type.getAnnualLimit())
                .usedDays(BigDecimal.ZERO)
                .remainingDays(type.getAnnualLimit())
                .build();
        toCreate.add(balance);
      }
    }

    // 批量插入
    if (!toCreate.isEmpty()) {
      leaveBalanceRepository.saveBatch(toCreate);
    }

    log.info("用户假期余额初始化完成: userId={}, year={}, created={}", userId, year, toCreate.size());
  }

  /**
   * 获取待审批列表 ✅ 优化：使用批量加载
   *
   * @return 请假申请列表
   */
  public List<LeaveApplicationDTO> getPendingApplications() {
    List<LeaveApplication> applications = leaveApplicationMapper.selectPendingApplications();
    if (applications.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, LeaveType> typeMap = batchLoadLeaveTypes(applications);
    return applications.stream()
        .map(app -> toApplicationDTO(app, typeMap))
        .collect(Collectors.toList());
  }

  /**
   * 生成申请编号
   *
   * @return 申请编号
   */
  private String generateApplicationNo() {
    String prefix = "LV" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case LeaveApplication.STATUS_PENDING -> "待审批";
      case LeaveApplication.STATUS_APPROVED -> "已批准";
      case LeaveApplication.STATUS_REJECTED -> "已拒绝";
      case LeaveApplication.STATUS_CANCELLED -> "已取消";
      default -> status;
    };
  }

  private LeaveTypeDTO toLeaveTypeDTO(final LeaveType type) {
    LeaveTypeDTO dto = new LeaveTypeDTO();
    dto.setId(type.getId());
    dto.setName(type.getName());
    dto.setCode(type.getCode());
    dto.setPaid(type.getPaid());
    dto.setAnnualLimit(type.getAnnualLimit());
    dto.setNeedApproval(type.getNeedApproval());
    dto.setDescription(type.getDescription());
    dto.setSortOrder(type.getSortOrder());
    dto.setEnabled(type.getEnabled());
    return dto;
  }

  /**
   * 转换为DTO（单条查询使用，会触发数据库查询）
   *
   * @param app 请假申请实体
   * @return 请假申请DTO
   */
  private LeaveApplicationDTO toApplicationDTO(final LeaveApplication app) {
    Map<Long, LeaveType> typeMap = new HashMap<>();
    if (app.getLeaveTypeId() != null) {
      LeaveType type = leaveTypeRepository.getById(app.getLeaveTypeId());
      if (type != null) {
        typeMap.put(type.getId(), type);
      }
    }
    return toApplicationDTO(app, typeMap);
  }

  /**
   * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）
   *
   * @param app 请假申请实体
   * @param typeMap 请假类型Map
   * @return 请假申请DTO
   */
  private LeaveApplicationDTO toApplicationDTO(
      final LeaveApplication app, final Map<Long, LeaveType> typeMap) {
    LeaveApplicationDTO dto = new LeaveApplicationDTO();
    dto.setId(app.getId());
    dto.setApplicationNo(app.getApplicationNo());
    dto.setUserId(app.getUserId());
    dto.setLeaveTypeId(app.getLeaveTypeId());
    dto.setStartTime(app.getStartTime());
    dto.setEndTime(app.getEndTime());
    dto.setDuration(app.getDuration());
    dto.setReason(app.getReason());
    dto.setAttachmentUrl(app.getAttachmentUrl());
    dto.setStatus(app.getStatus());
    dto.setStatusName(getStatusName(app.getStatus()));
    dto.setApproverId(app.getApproverId());
    dto.setApprovedAt(app.getApprovedAt());
    dto.setApprovalComment(app.getApprovalComment());
    dto.setCreatedAt(app.getCreatedAt());

    // 从Map获取请假类型名称（避免N+1）
    if (app.getLeaveTypeId() != null) {
      LeaveType type = typeMap.get(app.getLeaveTypeId());
      if (type != null) {
        dto.setLeaveTypeName(type.getName());
      }
    }

    return dto;
  }

  /**
   * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）
   *
   * @param balance 假期余额实体
   * @param typeMap 请假类型Map
   * @return 假期余额DTO
   */
  private LeaveBalanceDTO toBalanceDTO(
      final LeaveBalance balance, final Map<Long, LeaveType> typeMap) {
    LeaveBalanceDTO dto = new LeaveBalanceDTO();
    dto.setId(balance.getId());
    dto.setUserId(balance.getUserId());
    dto.setLeaveTypeId(balance.getLeaveTypeId());
    dto.setYear(balance.getYear());
    dto.setTotalDays(balance.getTotalDays());
    dto.setUsedDays(balance.getUsedDays());
    dto.setRemainingDays(balance.getRemainingDays());

    // 从Map获取请假类型名称（避免N+1）
    if (balance.getLeaveTypeId() != null) {
      LeaveType type = typeMap.get(balance.getLeaveTypeId());
      if (type != null) {
        dto.setLeaveTypeName(type.getName());
      }
    }

    return dto;
  }
}
