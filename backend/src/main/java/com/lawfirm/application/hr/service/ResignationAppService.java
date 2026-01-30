package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.ApproveResignationCommand;
import com.lawfirm.application.hr.command.CreateResignationCommand;
import com.lawfirm.application.hr.dto.ResignationDTO;
import com.lawfirm.application.hr.dto.ResignationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.EmployeeStatus;
import com.lawfirm.common.constant.ResignationStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.entity.Resignation;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.hr.repository.ResignationRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ResignationMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 离职申请应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResignationAppService {

  /** 离职申请仓储 */
  private final ResignationRepository resignationRepository;

  /** 离职申请Mapper */
  private final ResignationMapper resignationMapper;

  /** 员工仓储 */
  private final EmployeeRepository employeeRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 审批服务 */
  private final ApprovalService approvalService;

  /** 审批人服务 */
  private final ApproverService approverService;

  /**
   * 分页查询离职申请 ✅ 优化：使用批量加载避免N+1查询.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ResignationDTO> listResignations(final ResignationQueryDTO query) {
    IPage<Resignation> page =
        resignationMapper.selectResignationPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getEmployeeId(),
            query.getStatus());

    List<Resignation> resignations = page.getRecords();
    if (resignations.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 批量加载关联数据
    Map<Long, Employee> employeeMap = batchLoadEmployees(resignations);
    Map<Long, User> userMap = batchLoadUsers(resignations, employeeMap);

    return PageResult.of(
        resignations.stream().map(r -> toDTO(r, employeeMap, userMap)).collect(Collectors.toList()),
        page.getTotal(),
        query.getPageNum(),
        query.getPageSize());
  }

  /**
   * 批量加载员工信息.
   *
   * @param resignations 离职申请列表
   * @return 员工映射表
   */
  private Map<Long, Employee> batchLoadEmployees(final List<Resignation> resignations) {
    Set<Long> employeeIds =
        resignations.stream()
            .map(Resignation::getEmployeeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (employeeIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return employeeRepository.listByIds(new ArrayList<>(employeeIds)).stream()
        .collect(Collectors.toMap(Employee::getId, e -> e));
  }

  /**
   * 批量加载用户信息（员工对应的用户、交接人和审批人）.
   *
   * @param resignations 离职申请列表
   * @param employeeMap 员工映射表
   * @return 用户映射表
   */
  private Map<Long, User> batchLoadUsers(
      final List<Resignation> resignations, final Map<Long, Employee> employeeMap) {
    Set<Long> userIds = new HashSet<>();
    // 收集员工对应的用户ID
    employeeMap.values().stream()
        .map(Employee::getUserId)
        .filter(Objects::nonNull)
        .forEach(userIds::add);
    // 收集交接人ID
    resignations.stream()
        .map(Resignation::getHandoverPersonId)
        .filter(Objects::nonNull)
        .forEach(userIds::add);
    // 收集审批人ID
    resignations.stream()
        .map(Resignation::getApproverId)
        .filter(Objects::nonNull)
        .forEach(userIds::add);

    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return userRepository.listByIds(new ArrayList<>(userIds)).stream()
        .collect(Collectors.toMap(User::getId, u -> u));
  }

  /**
   * 根据ID查询离职申请.
   *
   * @param id 申请ID
   * @return 离职申请DTO
   */
  public ResignationDTO getResignationById(final Long id) {
    Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");
    return toDTO(resignation);
  }

  /**
   * 根据员工ID查询离职申请.
   *
   * @param employeeId 员工ID
   * @return 离职申请DTO列表
   */
  public List<ResignationDTO> getResignationsByEmployeeId(final Long employeeId) {
    List<Resignation> resignations = resignationRepository.findByEmployeeId(employeeId);
    return resignations.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 创建离职申请.
   *
   * @param command 创建命令
   * @return 创建后的离职申请DTO
   */
  @Transactional
  public ResignationDTO createResignation(final CreateResignationCommand command) {
    // 验证员工存在
    Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

    // 检查是否已有待审批的离职申请
    List<Resignation> pendingApplications =
        resignationRepository.findByEmployeeId(command.getEmployeeId()).stream()
            .filter(
                r ->
                    ResignationStatus.PENDING.equals(r.getStatus())
                        || ResignationStatus.APPROVED.equals(r.getStatus()))
            .collect(Collectors.toList());
    if (!pendingApplications.isEmpty()) {
      throw new BusinessException("该员工已有待处理或已审批的离职申请");
    }

    // 生成申请编号
    String applicationNo = generateApplicationNo();

    // 创建离职申请
    Resignation resignation =
        Resignation.builder()
            .employeeId(command.getEmployeeId())
            .applicationNo(applicationNo)
            .resignationType(command.getResignationType())
            .resignationDate(command.getResignationDate())
            .lastWorkDate(command.getLastWorkDate())
            .reason(command.getReason())
            .handoverPersonId(command.getHandoverPersonId())
            .handoverStatus(ResignationStatus.HANDOVER_PENDING)
            .handoverNote(command.getHandoverNote())
            .status(ResignationStatus.PENDING)
            .build();

    resignationRepository.save(resignation);

    // 创建审批记录
    Long approverId = approverService.findDefaultApprover();
    approvalService.createApproval(
        "RESIGNATION",
        resignation.getId(),
        applicationNo,
        "离职申请：" + (employee.getEmployeeNo() != null ? employee.getEmployeeNo() : ""),
        approverId,
        "MEDIUM",
        "NORMAL",
        null);

    log.info("创建离职申请成功: {} ({})", applicationNo, employee.getEmployeeNo());
    return toDTO(resignation);
  }

  /**
   * 审批离职申请.
   *
   * @param id 申请ID
   * @param command 审批命令
   * @return 审批后的离职申请DTO
   */
  @Transactional
  public ResignationDTO approveResignation(final Long id, final ApproveResignationCommand command) {
    Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");

    if (!ResignationStatus.PENDING.equals(resignation.getStatus())) {
      throw new BusinessException("当前状态不允许审批");
    }

    Long approverId = SecurityUtils.getUserId();
    resignation.setApproverId(approverId);
    resignation.setApprovedDate(LocalDate.now());
    resignation.setComment(command.getComment());
    resignation.setStatus(
        command.getApproved() ? ResignationStatus.APPROVED : ResignationStatus.REJECTED);

    resignationRepository.updateById(resignation);

    // 如果审批通过，更新员工档案
    if (command.getApproved()) {
      Employee employee = employeeRepository.getByIdOrThrow(resignation.getEmployeeId(), "员工不存在");
      employee.setResignationDate(resignation.getLastWorkDate());
      employee.setResignationReason(resignation.getReason());
      employee.setWorkStatus(EmployeeStatus.RESIGNED);
      employeeRepository.updateById(employee);
      log.info("离职申请审批通过，已更新员工档案: {}", employee.getEmployeeNo());
    }

    log.info("审批离职申请: {} ({})", id, command.getApproved() ? "通过" : "拒绝");
    return toDTO(resignation);
  }

  /**
   * 完成交接 ✅ 修复：添加权限验证，只有交接人或HR才能完成交接.
   *
   * @param id 申请ID
   * @param handoverNote 交接备注
   * @return 更新后的离职申请DTO
   */
  @Transactional
  public ResignationDTO completeHandover(final Long id, final String handoverNote) {
    Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");

    if (!ResignationStatus.APPROVED.equals(resignation.getStatus())) {
      throw new BusinessException("只有已审批通过的申请才能完成交接");
    }

    Long currentUserId = SecurityUtils.getUserId();

    // ✅ 权限验证：只有交接人或HR/管理员才能完成交接
    boolean isHandoverPerson =
        resignation.getHandoverPersonId() != null
            && resignation.getHandoverPersonId().equals(currentUserId);
    boolean isHROrAdmin = SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER", "HR");

    if (!isHandoverPerson && !isHROrAdmin) {
      throw new BusinessException("权限不足：只有指定的交接人或HR管理员才能完成交接");
    }

    // 记录日志（HR代操作）
    if (!isHandoverPerson && isHROrAdmin) {
      log.warn(
          "HR管理员代完成交接: resignationId={}, operator={}, handoverPerson={}",
          id,
          currentUserId,
          resignation.getHandoverPersonId());
    }

    resignation.setHandoverStatus(ResignationStatus.HANDOVER_COMPLETED);
    if (handoverNote != null) {
      resignation.setHandoverNote(handoverNote);
    }
    resignation.setStatus(ResignationStatus.COMPLETED);

    resignationRepository.updateById(resignation);
    log.info("完成离职交接: id={}, completedBy={}", id, currentUserId);
    return toDTO(resignation);
  }

  /**
   * 删除离职申请（软删除）.
   *
   * @param id 申请ID
   */
  @Transactional
  public void deleteResignation(final Long id) {
    Resignation resignation = resignationRepository.getByIdOrThrow(id, "离职申请不存在");
    if (!ResignationStatus.PENDING.equals(resignation.getStatus())) {
      throw new BusinessException("只有待审批状态的申请可以删除");
    }
    resignationRepository.softDelete(id);
    log.info("删除离职申请: {}", id);
  }

  /**
   * 转换为DTO（单条记录查询使用，会触发数据库查询）.
   *
   * @param resignation 离职申请实体
   * @return 离职申请DTO
   */
  private ResignationDTO toDTO(final Resignation resignation) {
    // 单条查询时构建临时Map
    Map<Long, Employee> employeeMap = new HashMap<>();
    Map<Long, User> userMap = new HashMap<>();

    if (resignation.getEmployeeId() != null) {
      Employee employee = employeeRepository.findById(resignation.getEmployeeId());
      if (employee != null) {
        employeeMap.put(employee.getId(), employee);
        if (employee.getUserId() != null) {
          User user = userRepository.findById(employee.getUserId());
          if (user != null) {
            userMap.put(user.getId(), user);
          }
        }
      }
    }
    if (resignation.getHandoverPersonId() != null) {
      User handover = userRepository.findById(resignation.getHandoverPersonId());
      if (handover != null) {
        userMap.put(handover.getId(), handover);
      }
    }
    if (resignation.getApproverId() != null) {
      User approver = userRepository.findById(resignation.getApproverId());
      if (approver != null) {
        userMap.put(approver.getId(), approver);
      }
    }

    return toDTO(resignation, employeeMap, userMap);
  }

  /**
   * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）.
   *
   * @param resignation 离职申请实体
   * @param employeeMap 员工映射表
   * @param userMap 用户映射表
   * @return 离职申请DTO
   */
  private ResignationDTO toDTO(
      final Resignation resignation,
      final Map<Long, Employee> employeeMap,
      final Map<Long, User> userMap) {
    ResignationDTO dto = new ResignationDTO();
    dto.setId(resignation.getId());
    dto.setEmployeeId(resignation.getEmployeeId());
    dto.setApplicationNo(resignation.getApplicationNo());
    dto.setResignationType(resignation.getResignationType());
    dto.setResignationDate(resignation.getResignationDate());
    dto.setLastWorkDate(resignation.getLastWorkDate());
    dto.setReason(resignation.getReason());
    dto.setHandoverPersonId(resignation.getHandoverPersonId());
    dto.setHandoverStatus(resignation.getHandoverStatus());
    dto.setHandoverNote(resignation.getHandoverNote());
    dto.setStatus(resignation.getStatus());
    dto.setApproverId(resignation.getApproverId());
    dto.setApprovedDate(resignation.getApprovedDate());
    dto.setComment(resignation.getComment());
    dto.setCreatedAt(resignation.getCreatedAt());
    dto.setUpdatedAt(resignation.getUpdatedAt());

    // 从Map获取员工和用户信息（避免N+1）
    if (resignation.getEmployeeId() != null) {
      Employee employee = employeeMap.get(resignation.getEmployeeId());
      if (employee != null && employee.getUserId() != null) {
        dto.setUserId(employee.getUserId());
        User user = userMap.get(employee.getUserId());
        if (user != null) {
          dto.setEmployeeName(user.getRealName());
        }
      }
    }

    // 从Map获取交接人信息
    if (resignation.getHandoverPersonId() != null) {
      User handoverPerson = userMap.get(resignation.getHandoverPersonId());
      if (handoverPerson != null) {
        dto.setHandoverPersonName(handoverPerson.getRealName());
      }
    }

    // 从Map获取审批人信息
    if (resignation.getApproverId() != null) {
      User approver = userMap.get(resignation.getApproverId());
      if (approver != null) {
        dto.setApproverName(approver.getRealName());
      }
    }

    // 设置离职类型名称
    if (dto.getResignationType() != null) {
      dto.setResignationTypeName(ResignationStatus.getTypeName(dto.getResignationType()));
    }

    // 设置交接状态名称
    if (dto.getHandoverStatus() != null) {
      dto.setHandoverStatusName(ResignationStatus.getHandoverStatusName(dto.getHandoverStatus()));
    }

    // 设置状态名称
    if (dto.getStatus() != null) {
      dto.setStatusName(ResignationStatus.getStatusName(dto.getStatus()));
    }

    return dto;
  }

  /** 编号生成序列号（线程安全） */
  private static final AtomicLong SEQUENCE = new AtomicLong(0);

  /**
   * 生成申请编号 ✅ 修复：使用完整时间戳+序列号，避免并发重复。
   *
   * @return 申请编号
   */
  private String generateApplicationNo() {
    String prefix = "RES";
    String timestamp = String.valueOf(System.currentTimeMillis());
    long seq = SEQUENCE.incrementAndGet() % 1000;
    return String.format("%s%s%03d", prefix, timestamp, seq);
  }
}
