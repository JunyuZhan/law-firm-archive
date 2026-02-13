package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateContractCommand;
import com.lawfirm.application.hr.command.UpdateContractCommand;
import com.lawfirm.application.hr.dto.ContractDTO;
import com.lawfirm.application.hr.dto.ContractQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.Contract;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.domain.hr.repository.ContractRepository;
import com.lawfirm.domain.hr.repository.EmployeeRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 劳动合同应用服务 */
@Slf4j
@Service("laborContractAppService")
@RequiredArgsConstructor
public class ContractAppService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 合同Mapper */
  private final ContractMapper contractMapper;

  /** 员工仓储 */
  private final EmployeeRepository employeeRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 合同编号生成序列号，避免并发冲突 */
  private final AtomicLong contractSequence = new AtomicLong(0);

  /**
   * 分页查询劳动合同 问题340修复：使用批量加载避免N+1查询 数据权限：管理角色看全部，普通员工只能看自己的.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ContractDTO> listContracts(final ContractQueryDTO query) {
    // 数据权限控制：普通员工只能看自己的合同
    Long filterEmployeeId = query.getEmployeeId();
    if (!SecurityUtils.hasAllDataScope()) {
      // 非全局权限用户，只能查看自己的合同
      Long currentUserId = SecurityUtils.getUserId();
      Optional<Employee> currentEmployeeOpt = employeeRepository.findByUserId(currentUserId);
      if (currentEmployeeOpt.isPresent()) {
        filterEmployeeId = currentEmployeeOpt.get().getId();
      } else {
        // 用户没有对应的员工档案，返回空结果
        return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
      }
    }

    IPage<Contract> page =
        contractMapper.selectContractPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            filterEmployeeId,
            query.getContractNo(),
            query.getStatus());
    List<Contract> contracts = page.getRecords();

    if (contracts.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 批量加载员工信息
    Set<Long> employeeIds =
        contracts.stream()
            .map(Contract::getEmployeeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Employee> employeeMap =
        employeeIds.isEmpty()
            ? Collections.emptyMap()
            : employeeRepository.listByIds(new ArrayList<>(employeeIds)).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

    // 批量加载用户信息
    Set<Long> userIds =
        employeeMap.values().stream()
            .map(Employee::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO（从Map获取，避免N+1）
    List<ContractDTO> dtos =
        contracts.stream().map(c -> toDTO(c, employeeMap, userMap)).collect(Collectors.toList());

    return PageResult.of(dtos, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 根据ID查询劳动合同 数据权限：管理角色可查看所有，普通员工只能查看自己的.
   *
   * @param id 合同ID
   * @return 合同DTO
   */
  public ContractDTO getContractById(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");

    // 数据权限检查：普通员工只能查看自己的合同
    if (!SecurityUtils.hasAllDataScope()) {
      Long currentUserId = SecurityUtils.getUserId();
      Optional<Employee> currentEmployeeOpt = employeeRepository.findByUserId(currentUserId);
      if (currentEmployeeOpt.isEmpty()
          || !currentEmployeeOpt.get().getId().equals(contract.getEmployeeId())) {
        throw new BusinessException("无权查看该合同");
      }
    }

    return toDTO(contract);
  }

  /**
   * 根据员工ID查询所有合同.
   *
   * @param employeeId 员工ID
   * @return 合同列表
   */
  public List<ContractDTO> getContractsByEmployeeId(final Long employeeId) {
    List<Contract> contracts = contractRepository.findByEmployeeId(employeeId);
    return contracts.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 创建劳动合同.
   *
   * @param command 创建命令
   * @return 合同DTO
   */
  @Transactional
  public ContractDTO createContract(final CreateContractCommand command) {
    // 验证员工存在
    Employee employee = employeeRepository.getByIdOrThrow(command.getEmployeeId(), "员工不存在");

    // 验证合同日期有效性：开始日期不能晚于结束日期
    if (command.getEndDate() != null
        && command.getStartDate() != null
        && command.getStartDate().isAfter(command.getEndDate())) {
      throw new BusinessException("合同开始日期不能晚于结束日期");
    }

    // 生成合同编号
    String contractNo = command.getContractNo();
    if (contractNo == null || contractNo.isEmpty()) {
      contractNo = generateContractNo();
    } else {
      // 检查合同编号是否已存在
      if (contractRepository.findByContractNo(contractNo).isPresent()) {
        throw new BusinessException("合同编号已存在");
      }
    }

    // 计算试用期结束日期
    LocalDate probationEndDate = command.getProbationEndDate();
    if (probationEndDate == null
        && command.getProbationMonths() != null
        && command.getProbationMonths() > 0) {
      probationEndDate = command.getStartDate().plusMonths(command.getProbationMonths());
    }

    // 计算到期日期（如果是固定期限合同）
    LocalDate expireDate = command.getEndDate();
    if (expireDate == null
        && "FIXED".equals(command.getContractType())
        && command.getStartDate() != null) {
      // 默认3年
      expireDate = command.getStartDate().plusYears(3);
    }

    // 创建合同
    Contract contract =
        Contract.builder()
            .employeeId(command.getEmployeeId())
            .contractNo(contractNo)
            .contractType(command.getContractType())
            .startDate(command.getStartDate())
            .endDate(command.getEndDate())
            .probationMonths(
                command.getProbationMonths() != null ? command.getProbationMonths() : 0)
            .probationEndDate(probationEndDate)
            .baseSalary(command.getBaseSalary())
            .performanceBonus(command.getPerformanceBonus())
            .otherAllowance(command.getOtherAllowance())
            .status("ACTIVE")
            .signDate(command.getSignDate() != null ? command.getSignDate() : LocalDate.now())
            .expireDate(expireDate)
            .renewCount(0)
            .contractFileUrl(command.getContractFileUrl())
            .remark(command.getRemark())
            .build();

    contractRepository.save(contract);
    log.info("创建劳动合同成功: {} ({})", contractNo, employee.getEmployeeNo());
    return toDTO(contract);
  }

  /**
   * 更新劳动合同.
   *
   * @param id 合同ID
   * @param command 更新命令
   * @return 合同DTO
   */
  @Transactional
  public ContractDTO updateContract(final Long id, final UpdateContractCommand command) {
    Contract contract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");

    // 如果更新合同编号，检查是否重复
    if (command.getContractNo() != null
        && !command.getContractNo().equals(contract.getContractNo())) {
      if (contractRepository.findByContractNo(command.getContractNo()).isPresent()) {
        throw new BusinessException("合同编号已存在");
      }
      contract.setContractNo(command.getContractNo());
    }

    // 更新字段
    if (command.getContractType() != null) {
      contract.setContractType(command.getContractType());
    }
    if (command.getStartDate() != null) {
      contract.setStartDate(command.getStartDate());
    }
    if (command.getEndDate() != null) {
      contract.setEndDate(command.getEndDate());
    }
    if (command.getProbationMonths() != null) {
      contract.setProbationMonths(command.getProbationMonths());
    }
    if (command.getProbationEndDate() != null) {
      contract.setProbationEndDate(command.getProbationEndDate());
    }
    if (command.getBaseSalary() != null) {
      contract.setBaseSalary(command.getBaseSalary());
    }
    if (command.getPerformanceBonus() != null) {
      contract.setPerformanceBonus(command.getPerformanceBonus());
    }
    if (command.getOtherAllowance() != null) {
      contract.setOtherAllowance(command.getOtherAllowance());
    }
    if (command.getStatus() != null) {
      contract.setStatus(command.getStatus());
    }
    if (command.getSignDate() != null) {
      contract.setSignDate(command.getSignDate());
    }
    if (command.getExpireDate() != null) {
      contract.setExpireDate(command.getExpireDate());
    }
    if (command.getContractFileUrl() != null) {
      contract.setContractFileUrl(command.getContractFileUrl());
    }
    if (command.getRemark() != null) {
      contract.setRemark(command.getRemark());
    }

    contractRepository.updateById(contract);
    log.info("更新劳动合同成功: {}", id);
    return toDTO(contract);
  }

  /**
   * 删除劳动合同（软删除）.
   *
   * @param id 合同ID
   */
  @Transactional
  public void deleteContract(final Long id) {
    contractRepository.getByIdOrThrow(id, "劳动合同不存在");
    contractRepository.softDelete(id);
    log.info("删除劳动合同: {}", id);
  }

  /**
   * 续签合同.
   *
   * @param id 原合同ID
   * @param newStartDate 新开始日期
   * @param newEndDate 新结束日期
   * @return 新合同DTO
   */
  @Transactional
  public ContractDTO renewContract(
      final Long id, final LocalDate newStartDate, final LocalDate newEndDate) {
    Contract oldContract = contractRepository.getByIdOrThrow(id, "劳动合同不存在");

    if (!"ACTIVE".equals(oldContract.getStatus())) {
      throw new BusinessException("只有生效中的合同可以续签");
    }

    // 将旧合同状态改为已到期
    oldContract.setStatus("EXPIRED");
    oldContract.setExpireDate(newStartDate.minusDays(1));
    contractRepository.updateById(oldContract);

    // 创建新合同
    Contract newContract =
        Contract.builder()
            .employeeId(oldContract.getEmployeeId())
            .contractNo(generateContractNo())
            .contractType(oldContract.getContractType())
            .startDate(newStartDate)
            .endDate(newEndDate)
            .probationMonths(0) // 续签不再有试用期
            .baseSalary(oldContract.getBaseSalary())
            .performanceBonus(oldContract.getPerformanceBonus())
            .otherAllowance(oldContract.getOtherAllowance())
            .status("ACTIVE")
            .signDate(LocalDate.now())
            .expireDate(newEndDate)
            .renewCount(oldContract.getRenewCount() + 1)
            .remark("续签自合同：" + oldContract.getContractNo())
            .build();

    contractRepository.save(newContract);
    log.info("续签劳动合同成功: {} -> {}", oldContract.getContractNo(), newContract.getContractNo());
    return toDTO(newContract);
  }

  /**
   * 转换为DTO（单个合同，会查询员工和用户）.
   *
   * @param contract 合同实体
   * @return DTO
   */
  private ContractDTO toDTO(final Contract contract) {
    return toDTO(contract, null, null);
  }

  /**
   * 转换为DTO（批量优化版本，从Map获取员工和用户信息） 问题340修复：支持批量加载.
   *
   * @param contract 合同实体
   * @param employeeMap 员工Map
   * @param userMap 用户Map
   * @return DTO
   */
  private ContractDTO toDTO(
      final Contract contract,
      final Map<Long, Employee> employeeMap,
      final Map<Long, User> userMap) {
    ContractDTO dto = new ContractDTO();
    dto.setId(contract.getId());
    dto.setEmployeeId(contract.getEmployeeId());
    dto.setContractNo(contract.getContractNo());
    dto.setContractType(contract.getContractType());
    dto.setStartDate(contract.getStartDate());
    dto.setEndDate(contract.getEndDate());
    dto.setProbationMonths(contract.getProbationMonths());
    dto.setProbationEndDate(contract.getProbationEndDate());
    dto.setBaseSalary(contract.getBaseSalary());
    dto.setPerformanceBonus(contract.getPerformanceBonus());
    dto.setOtherAllowance(contract.getOtherAllowance());
    dto.setStatus(contract.getStatus());
    dto.setSignDate(contract.getSignDate());
    dto.setExpireDate(contract.getExpireDate());
    dto.setRenewCount(contract.getRenewCount());
    dto.setContractFileUrl(contract.getContractFileUrl());
    dto.setRemark(contract.getRemark());
    dto.setCreatedAt(contract.getCreatedAt());
    dto.setUpdatedAt(contract.getUpdatedAt());

    // 加载员工和用户信息（从Map获取或单独查询）
    if (contract.getEmployeeId() != null) {
      Employee employee =
          (employeeMap != null)
              ? employeeMap.get(contract.getEmployeeId())
              : employeeRepository.findById(contract.getEmployeeId());
      if (employee != null && employee.getUserId() != null) {
        dto.setUserId(employee.getUserId());
        User user =
            (userMap != null)
                ? userMap.get(employee.getUserId())
                : userRepository.findById(employee.getUserId());
        if (user != null) {
          dto.setEmployeeName(user.getRealName());
        }
      }
    }

    // 设置类型名称
    if (dto.getContractType() != null) {
      switch (dto.getContractType()) {
        case "FIXED" -> dto.setContractTypeName("固定期限");
        case "UNFIXED" -> dto.setContractTypeName("无固定期限");
        case "PROJECT" -> dto.setContractTypeName("项目合同");
        case "INTERN" -> dto.setContractTypeName("实习");
        default -> dto.setContractTypeName(dto.getContractType());
      }
    }

    // 设置状态名称
    if (dto.getStatus() != null) {
      switch (dto.getStatus()) {
        case "ACTIVE" -> dto.setStatusName("生效中");
        case "EXPIRED" -> dto.setStatusName("已到期");
        case "TERMINATED" -> dto.setStatusName("已终止");
        case "RENEWED" -> dto.setStatusName("已续签");
        default -> dto.setStatusName(dto.getStatus());
      }
    }

    return dto;
  }

  /**
   * 生成合同编号 问题343修复：使用日期+序列号+随机数避免并发冲突
   *
   * @return 生成的合同编号
   */
  private String generateContractNo() {
    String prefix = "HC";
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = contractSequence.incrementAndGet() % 1000;
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return String.format("%s%s%03d%s", prefix, date, seq, random);
  }
}
