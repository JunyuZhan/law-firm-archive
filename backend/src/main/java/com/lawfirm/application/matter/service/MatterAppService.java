package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.document.service.DossierAutoArchiveService;
import com.lawfirm.application.matter.command.CloseMatterCommand;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.command.UpdateMatterCommand;
import com.lawfirm.application.matter.dto.MatterClientDTO;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterParticipantDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.ContractStatus;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.util.SqlUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import com.lawfirm.domain.matter.event.MatterUpdatedEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 案件应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterAppService {

  /** 案件仓储. */
  private final MatterRepository matterRepository;

  /** 案件-客户关联仓储. */
  private final MatterClientRepository matterClientRepository;

  /** 案件Mapper. */
  private final MatterMapper matterMapper;

  /** 案件参与者Mapper. */
  private final MatterParticipantMapper participantMapper;

  /** 案件参与者仓储. */
  private final MatterParticipantRepository matterParticipantRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** 合同仓储. */
  private final ContractRepository contractRepository;

  /** 合同参与者仓储. */
  private final ContractParticipantRepository contractParticipantRepository;

  /** 部门仓储. */
  private final DepartmentRepository departmentRepository;

  /** 部门Mapper. */
  private final DepartmentMapper departmentMapper;

  /** 审批服务. */
  private final ApprovalService approvalService;

  /** 审批人服务. */
  private final ApproverService approverService;

  /** 归档应用服务. */
  private final ArchiveAppService archiveAppService;

  /** 期限应用服务. */
  private final DeadlineAppService deadlineAppService;

  /** 通知应用服务. */
  private final NotificationAppService notificationAppService;

  /** 对象映射器. */
  private final ObjectMapper objectMapper;

  /** 卷宗自动归档服务. */
  private final DossierAutoArchiveService dossierAutoArchiveService;

  /** 事件发布器. */
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 分页查询案件 优化：使用批量加载替代N+1查询，显著提升列表查询性能
   *
   * @param query 查询条件对象，包含案件名称、状态、时间范围等筛选条件
   * @return 分页结果对象，包含案件DTO列表和分页信息
   */
  public PageResult<MatterDTO> listMatters(final MatterQueryDTO query) {
    IPage<Matter> page;

    // 转义 LIKE 查询中的通配符
    String escapedName = SqlUtils.escapeLike(query.getName());
    String escapedMatterNo = SqlUtils.escapeLike(query.getMatterNo());

    if (Boolean.TRUE.equals(query.getMyMatters())) {
      // 查询我参与的案件（已经按用户过滤，不需要额外权限过滤）
      Long userId = SecurityUtils.getUserId();
      page =
          matterMapper.selectByParticipantUserId(
              new Page<>(query.getPageNum(), query.getPageSize()),
              userId,
              escapedName,
              query.getStatus(),
              query.getCreatedAtFrom(),
              query.getCreatedAtTo());
    } else {
      // 根据用户权限过滤数据
      String dataScope = SecurityUtils.getDataScope();
      Long currentUserId = SecurityUtils.getUserId();
      Long deptId = SecurityUtils.getDepartmentId();

      log.debug("查询项目列表 - 用户ID: {}, 数据权限: {}, 部门ID: {}", currentUserId, dataScope, deptId);

      // 获取可访问的项目ID列表
      List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

      log.debug("可访问的项目ID列表: {}", accessibleMatterIds);

      // 如果返回空列表，表示没有权限，返回空结果
      if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
        log.debug("用户{}没有可访问的项目，返回空结果", currentUserId);
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
      }

      // 设置权限过滤的案件ID列表
      query.setMatterIds(accessibleMatterIds); // null表示可以访问所有项目（ALL权限）
      // 设置转义后的 LIKE 查询参数
      query.setName(escapedName);
      query.setMatterNo(escapedMatterNo);
      page =
          matterMapper.selectMatterPage(new Page<>(query.getPageNum(), query.getPageSize()), query);

      log.debug("查询结果 - 总数: {}, 当前页记录数: {}", page.getTotal(), page.getRecords().size());
    }

    List<Matter> matters = page.getRecords();
    if (matters.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    // 批量转换DTO（避免N+1查询）
    List<MatterDTO> records = batchConvertToDTO(matters);

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 批量转换项目列表为DTO（性能优化：避免N+1查询） 使用批量加载替代循环中的单次查询
   *
   * @param matters 案件列表
   * @return 案件DTO列表
   */
  private List<MatterDTO> batchConvertToDTO(final List<Matter> matters) {
    if (matters == null || matters.isEmpty()) {
      return Collections.emptyList();
    }

    // 1. 收集所有需要查询的ID
    Set<Long> clientIds = new HashSet<>();
    Set<Long> contractIds = new HashSet<>();
    Set<Long> userIds = new HashSet<>();
    Set<Long> deptIds = new HashSet<>();
    Set<Long> matterIds = new HashSet<>();

    for (Matter m : matters) {
      matterIds.add(m.getId());
      if (m.getClientId() != null) {
        clientIds.add(m.getClientId());
      }
      if (m.getContractId() != null) {
        contractIds.add(m.getContractId());
      }
      if (m.getOriginatorId() != null) {
        userIds.add(m.getOriginatorId());
      }
      if (m.getLeadLawyerId() != null) {
        userIds.add(m.getLeadLawyerId());
      }
      if (m.getDepartmentId() != null) {
        deptIds.add(m.getDepartmentId());
      }
    }

    // 2. 批量加载关联数据
    Map<Long, Client> clientMap =
        clientIds.isEmpty()
            ? Collections.emptyMap()
            : clientRepository.listByIds(new ArrayList<>(clientIds)).stream()
                .collect(Collectors.toMap(Client::getId, c -> c, (a, b) -> a));

    Map<Long, Contract> contractMap =
        contractIds.isEmpty()
            ? Collections.emptyMap()
            : contractRepository.listByIds(new ArrayList<>(contractIds)).stream()
                .collect(Collectors.toMap(Contract::getId, c -> c, (a, b) -> a));

    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

    Map<Long, Department> deptMap =
        deptIds.isEmpty()
            ? Collections.emptyMap()
            : departmentRepository.listByIds(new ArrayList<>(deptIds)).stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));

    // 3. 批量加载多客户列表
    Map<Long, List<MatterClient>> matterClientsMap = loadMatterClientsMap(matterIds);

    // 4. 使用预加载的数据转换DTO
    return matters.stream()
        .map(m -> toDTOWithMaps(m, clientMap, contractMap, userMap, deptMap, matterClientsMap))
        .collect(Collectors.toList());
  }

  /**
   * 批量加载项目客户关联
   *
   * @param matterIds 案件ID集合
   * @return 案件ID到客户关联列表的映射
   */
  private Map<Long, List<MatterClient>> loadMatterClientsMap(final Set<Long> matterIds) {
    if (matterIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<MatterClient> allClients =
        matterClientRepository
            .lambdaQuery()
            .in(MatterClient::getMatterId, matterIds)
            .eq(MatterClient::getDeleted, false)
            .list();

    return allClients.stream().collect(Collectors.groupingBy(MatterClient::getMatterId));
  }

  /**
   * 使用预加载的Map转换单个项目DTO（避免N+1查询）
   *
   * @param matter 案件实体
   * @param clientMap 客户Map
   * @param contractMap 合同Map
   * @param userMap 用户Map
   * @param deptMap 部门Map
   * @param matterClientsMap 案件客户关联Map
   * @return 案件DTO
   */
  private MatterDTO toDTOWithMaps(
      final Matter matter,
      final Map<Long, Client> clientMap,
      final Map<Long, Contract> contractMap,
      final Map<Long, User> userMap,
      final Map<Long, Department> deptMap,
      final Map<Long, List<MatterClient>> matterClientsMap) {
    MatterDTO dto = new MatterDTO();
    dto.setId(matter.getId());
    dto.setMatterNo(matter.getMatterNo());
    dto.setName(matter.getName());
    dto.setMatterType(matter.getMatterType());
    dto.setMatterTypeName(MatterConstants.getMatterTypeName(matter.getMatterType()));
    dto.setCaseType(matter.getCaseType());
    dto.setCaseTypeName(MatterConstants.getCaseTypeName(matter.getCaseType()));
    dto.setLitigationStage(matter.getLitigationStage());
    dto.setLitigationStageName(MatterConstants.getLitigationStageName(matter.getLitigationStage()));
    dto.setCauseOfAction(matter.getCauseOfAction());
    dto.setBusinessType(matter.getBusinessType());
    dto.setClientId(matter.getClientId());
    dto.setOpposingParty(matter.getOpposingParty());
    dto.setOpposingLawyerName(matter.getOpposingLawyerName());
    dto.setOpposingLawyerLicenseNo(matter.getOpposingLawyerLicenseNo());
    dto.setOpposingLawyerFirm(matter.getOpposingLawyerFirm());
    dto.setOpposingLawyerPhone(matter.getOpposingLawyerPhone());
    dto.setOpposingLawyerEmail(matter.getOpposingLawyerEmail());
    dto.setDescription(matter.getDescription());
    dto.setStatus(matter.getStatus());
    dto.setStatusName(MatterConstants.getMatterStatusName(matter.getStatus()));
    dto.setOriginatorId(matter.getOriginatorId());
    dto.setLeadLawyerId(matter.getLeadLawyerId());
    dto.setDepartmentId(matter.getDepartmentId());
    dto.setFeeType(matter.getFeeType());
    dto.setFeeTypeName(getFeeTypeName(matter.getFeeType()));
    dto.setEstimatedFee(matter.getEstimatedFee());
    dto.setActualFee(matter.getActualFee());
    dto.setFilingDate(matter.getFilingDate());
    dto.setExpectedClosingDate(matter.getExpectedClosingDate());
    dto.setActualClosingDate(matter.getActualClosingDate());
    dto.setClaimAmount(matter.getClaimAmount());
    dto.setOutcome(matter.getOutcome());
    dto.setContractId(matter.getContractId());
    dto.setRemark(matter.getRemark());
    dto.setConflictStatus(matter.getConflictStatus());
    dto.setCreatedAt(matter.getCreatedAt());
    dto.setUpdatedAt(matter.getUpdatedAt());

    // 从预加载的Map获取关联数据（无额外数据库查询）
    if (matter.getClientId() != null) {
      Client client = clientMap.get(matter.getClientId());
      if (client != null) {
        dto.setClientName(client.getName());
      }
    }

    if (matter.getContractId() != null) {
      Contract contract = contractMap.get(matter.getContractId());
      if (contract != null) {
        dto.setContractNo(contract.getContractNo());
        dto.setContractAmount(contract.getTotalAmount());
      }
    }

    // 加载多客户列表
    List<MatterClient> matterClients = matterClientsMap.get(matter.getId());
    if (matterClients != null && !matterClients.isEmpty()) {
      dto.setClients(
          matterClients.stream().map(this::toMatterClientDTO).collect(Collectors.toList()));
    }

    if (matter.getOriginatorId() != null) {
      User user = userMap.get(matter.getOriginatorId());
      if (user != null) {
        dto.setOriginatorName(user.getRealName());
      }
    }

    if (matter.getLeadLawyerId() != null) {
      User user = userMap.get(matter.getLeadLawyerId());
      if (user != null) {
        dto.setLeadLawyerName(user.getRealName());
      }
    }

    if (matter.getDepartmentId() != null) {
      Department dept = deptMap.get(matter.getDepartmentId());
      if (dept != null) {
        dto.setDepartmentName(dept.getName());
      }
    }

    return dto;
  }

  /**
   * 创建案件
   *
   * @param command 创建案件命令对象
   * @return 创建成功的案件DTO
   * @throws BusinessException 当客户不存在或合同无效时抛出异常
   */
  @Transactional
  public MatterDTO createMatter(final CreateMatterCommand command) {
    // 1. 验证客户存在
    clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

    // 2. 验证合同（项目必须关联已审批通过的合同）
    if (command.getContractId() == null) {
      throw new BusinessException("创建项目必须关联合同，请先创建并审批合同");
    }
    Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
    if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
      throw new BusinessException("只能基于已审批通过的合同创建项目，当前合同状态：" + contract.getStatus());
    }
    // 注意：一个合同可以创建多个项目（比如常年法顾合同下有多个具体项目）
    // 所以不再检查 contract.getMatterId() != null
    // 验证客户一致性（主要客户必须与合同客户一致）
    if (!contract.getClientId().equals(command.getClientId())) {
      throw new BusinessException("项目主要客户必须与合同客户一致");
    }

    // 3. 生成案件编号
    Long originatorId =
        command.getOriginatorId() != null ? command.getOriginatorId() : SecurityUtils.getUserId();
    String matterNo = generateMatterNo(originatorId, command.getCaseType());

    // 3. 创建案件实体
    // 由于项目是基于已审批通过的合同创建的，直接设为进行中状态
    Matter matter =
        Matter.builder()
            .matterNo(matterNo)
            .name(command.getName())
            .matterType(command.getMatterType())
            .caseType(command.getCaseType())
            .litigationStage(command.getLitigationStage())
            .causeOfAction(command.getCauseOfAction())
            .businessType(command.getBusinessType())
            .clientId(command.getClientId())
            .opposingParty(command.getOpposingParty())
            .opposingLawyerName(command.getOpposingLawyerName())
            .opposingLawyerLicenseNo(command.getOpposingLawyerLicenseNo())
            .opposingLawyerFirm(command.getOpposingLawyerFirm())
            .opposingLawyerPhone(command.getOpposingLawyerPhone())
            .opposingLawyerEmail(command.getOpposingLawyerEmail())
            .description(command.getDescription())
            .status(MatterConstants.STATUS_ACTIVE) // 基于已审批合同创建，直接进行中
            .originatorId(
                command.getOriginatorId() != null
                    ? command.getOriginatorId()
                    : SecurityUtils.getUserId())
            .leadLawyerId(command.getLeadLawyerId())
            .departmentId(
                command.getDepartmentId() != null
                    ? command.getDepartmentId()
                    : SecurityUtils.getDepartmentId())
            .feeType(command.getFeeType())
            .estimatedFee(command.getEstimatedFee())
            .filingDate(command.getFilingDate())
            .expectedClosingDate(command.getExpectedClosingDate())
            .claimAmount(command.getClaimAmount())
            .contractId(command.getContractId())
            .remark(command.getRemark())
            .conflictStatus(MatterConstants.STATUS_PENDING)
            .build();

    // 4. 保存案件
    matterRepository.save(matter);

    // 4.1 关联合同到项目
    contract.setMatterId(matter.getId());
    contractRepository.updateById(contract);

    // 4.2 保存客户关联（多客户支持）
    saveMatterClients(matter.getId(), command);

    // 4.3 从合同复制参与人到项目（如果合同有参与人）
    copyContractParticipantsToMatter(contract.getId(), matter.getId());

    // 5. 添加团队成员
    if (command.getParticipants() != null && !command.getParticipants().isEmpty()) {
      for (CreateMatterCommand.ParticipantCommand pc : command.getParticipants()) {
        // 检查是否已从合同复制过来
        if (participantMapper.countByMatterIdAndUserId(matter.getId(), pc.getUserId()) == 0) {
          addParticipant(
              matter.getId(),
              pc.getUserId(),
              pc.getRole(),
              pc.getCommissionRate(),
              pc.getIsOriginator());
        }
      }
    }

    // 6. 如果指定了主办律师，自动添加为团队成员
    if (command.getLeadLawyerId() != null) {
      if (participantMapper.countByMatterIdAndUserId(matter.getId(), command.getLeadLawyerId())
          == 0) {
        addParticipant(matter.getId(), command.getLeadLawyerId(), "LEAD", null, false);
      }
    }

    // 7. 自动将创建者添加为团队成员（如果还没有添加）
    Long creatorId = SecurityUtils.getUserId();
    if (creatorId != null
        && participantMapper.countByMatterIdAndUserId(matter.getId(), creatorId) == 0) {
      // 如果创建者就是主办律师，不再重复添加
      if (!creatorId.equals(command.getLeadLawyerId())) {
        addParticipant(matter.getId(), creatorId, "CO_COUNSEL", null, false);
      }
    }

    // 8. 如果是诉讼类项目且有立案日期，自动创建期限提醒
    if ("LITIGATION".equals(matter.getMatterType()) && matter.getFilingDate() != null) {
      try {
        deadlineAppService.autoCreateDeadlines(matter.getId());
      } catch (Exception e) {
        log.warn("自动创建期限提醒失败: matterId={}", matter.getId(), e);
      }
    }

    // 9. 自动归档卷宗材料（异步执行，不影响主流程）
    // 注意：使用异步方法并传递当前用户ID，因为异步线程无法获取SecurityContext
    try {
      Long currentUserId = SecurityUtils.getUserIdOrDefault(1L);
      dossierAutoArchiveService.archiveMatterDocumentsAsync(
          matter.getId(), command.getContractId(), currentUserId);
    } catch (Exception e) {
      log.warn("自动归档卷宗材料失败: matterId={}", matter.getId(), e);
    }

    log.info("案件创建成功: {} ({})", matter.getName(), matter.getMatterNo());
    return toDTO(matter);
  }

  /**
   * 基于合同创建项目 从已审批的合同自动填充项目信息
   *
   * @param contractId 合同ID
   * @param command 创建命令
   * @return 案件DTO
   */
  @Transactional
  public MatterDTO createMatterFromContract(
      final Long contractId, final CreateMatterCommand command) {
    // 验证合同存在
    Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

    // 从合同自动填充项目信息
    command.setContractId(contractId);
    if (command.getClientId() == null) {
      command.setClientId(contract.getClientId());
    }
    if (command.getFeeType() == null) {
      command.setFeeType(contract.getFeeType());
    }
    if (command.getEstimatedFee() == null && contract.getTotalAmount() != null) {
      command.setEstimatedFee(contract.getTotalAmount());
    }
    if (command.getDepartmentId() == null) {
      command.setDepartmentId(contract.getDepartmentId());
    }

    // 创建项目（createMatter会验证合同状态并自动关联）
    return createMatter(command);
  }

  /**
   * 更新案件信息
   *
   * @param command 更新案件命令对象
   * @return 更新后的案件DTO
   * @throws BusinessException 当案件不存在或状态不符合更新条件时抛出异常
   */
  @Transactional
  public MatterDTO updateMatter(final UpdateMatterCommand command) {
    Matter matter = matterRepository.getByIdOrThrow(command.getId(), "案件不存在");

    // 验证用户是否有权限访问该项目
    validateMatterAccess(command.getId());

    // 验证用户是否是项目负责人或参与者（只有项目成员才能编辑）
    validateMatterOwnership(command.getId());

    // 归档的项目不能编辑
    if (MatterConstants.STATUS_ARCHIVED.equals(matter.getStatus())) {
      throw new BusinessException("已归档的项目不能编辑");
    }

    // 更新字段
    if (StringUtils.hasText(command.getName())) {
      matter.setName(command.getName());
    }
    if (StringUtils.hasText(command.getMatterType())) {
      matter.setMatterType(command.getMatterType());
    }
    if (command.getCaseType() != null) {
      matter.setCaseType(command.getCaseType());
    }
    if (command.getLitigationStage() != null) {
      matter.setLitigationStage(command.getLitigationStage());
    }
    if (command.getCauseOfAction() != null) {
      matter.setCauseOfAction(command.getCauseOfAction());
    }
    if (command.getBusinessType() != null) {
      matter.setBusinessType(command.getBusinessType());
    }
    if (command.getClientId() != null) {
      clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
      matter.setClientId(command.getClientId());
    }
    if (command.getOpposingParty() != null) {
      matter.setOpposingParty(command.getOpposingParty());
    }
    if (command.getOpposingLawyerName() != null) {
      matter.setOpposingLawyerName(command.getOpposingLawyerName());
    }
    if (command.getOpposingLawyerLicenseNo() != null) {
      matter.setOpposingLawyerLicenseNo(command.getOpposingLawyerLicenseNo());
    }
    if (command.getOpposingLawyerFirm() != null) {
      matter.setOpposingLawyerFirm(command.getOpposingLawyerFirm());
    }
    if (command.getOpposingLawyerPhone() != null) {
      matter.setOpposingLawyerPhone(command.getOpposingLawyerPhone());
    }
    if (command.getOpposingLawyerEmail() != null) {
      matter.setOpposingLawyerEmail(command.getOpposingLawyerEmail());
    }
    if (command.getDescription() != null) {
      matter.setDescription(command.getDescription());
    }
    if (command.getOriginatorId() != null) {
      matter.setOriginatorId(command.getOriginatorId());
    }
    if (command.getLeadLawyerId() != null) {
      matter.setLeadLawyerId(command.getLeadLawyerId());
    }
    if (command.getDepartmentId() != null) {
      matter.setDepartmentId(command.getDepartmentId());
    }
    if (command.getFeeType() != null) {
      matter.setFeeType(command.getFeeType());
    }
    if (command.getEstimatedFee() != null) {
      matter.setEstimatedFee(command.getEstimatedFee());
    }
    if (command.getActualFee() != null) {
      matter.setActualFee(command.getActualFee());
    }
    if (command.getFilingDate() != null) {
      matter.setFilingDate(command.getFilingDate());
    }
    if (command.getExpectedClosingDate() != null) {
      matter.setExpectedClosingDate(command.getExpectedClosingDate());
    }
    if (command.getActualClosingDate() != null) {
      matter.setActualClosingDate(command.getActualClosingDate());
    }
    if (command.getClaimAmount() != null) {
      matter.setClaimAmount(command.getClaimAmount());
    }
    if (command.getOutcome() != null) {
      matter.setOutcome(command.getOutcome());
    }
    if (command.getContractId() != null) {
      matter.setContractId(command.getContractId());
    }
    if (command.getRemark() != null) {
      matter.setRemark(command.getRemark());
    }

    matterRepository.updateById(matter);
    log.info("案件更新成功: {}", matter.getName());

    // 发布项目更新事件，触发自动推送等后续操作
    try {
      Long operatorId = SecurityUtils.getCurrentUserId();
      eventPublisher.publishEvent(
          new MatterUpdatedEvent(this, matter.getId(), matter.getClientId(), operatorId));
    } catch (Exception e) {
      // 事件发布失败不影响主流程
      log.warn("发布项目更新事件失败: matterId={}, error={}", matter.getId(), e.getMessage());
    }

    return toDTO(matter);
  }

  /**
   * 删除案件
   *
   * @param id 案件ID
   * @throws BusinessException 当案件不存在或状态不符合删除条件时抛出异常
   */
  @Transactional
  public void deleteMatter(final Long id) {
    Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");

    // 验证用户是否有权限访问该项目
    validateMatterAccess(id);

    if (!MatterConstants.STATUS_DRAFT.equals(matter.getStatus())) {
      throw new BusinessException("只有草稿状态的案件可以删除");
    }

    // 删除团队成员
    participantMapper.deleteByMatterId(id);
    // 删除案件
    matterMapper.deleteById(id);
    log.info("案件删除成功: {}", matter.getName());
  }

  /**
   * 获取案件详情
   *
   * @param id 案件ID
   * @return 案件DTO详情
   * @throws BusinessException 当案件不存在或无权访问时抛出异常
   */
  public MatterDTO getMatterById(final Long id) {
    Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");

    // 验证用户是否有权限访问该项目
    validateMatterAccess(id);

    MatterDTO dto = toDTO(matter);

    // 加载团队成员
    List<MatterParticipant> participants = participantMapper.selectByMatterId(id);
    dto.setParticipants(
        participants.stream().map(this::toParticipantDTO).collect(Collectors.toList()));

    return dto;
  }

  /**
   * 修改案件状态
   *
   * @param id 案件ID
   * @param status 新状态
   * @throws BusinessException 当状态流转不合法时抛出异常
   */
  @Transactional
  public void changeStatus(final Long id, final String status) {
    Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");

    // 验证用户是否有权限访问该项目
    validateMatterAccess(id);

    // 验证用户是否是项目负责人或参与者（只有项目成员才能修改状态）
    validateMatterOwnership(id);

    String oldStatus = matter.getStatus();

    // 状态流转验证
    validateStatusTransition(oldStatus, status);

    matter.setStatus(status);
    if (MatterConstants.STATUS_CLOSED.equals(status) && matter.getActualClosingDate() == null) {
      matter.setActualClosingDate(LocalDate.now());
    }

    matterRepository.updateById(matter);
    log.info("案件状态修改成功: {} -> {}", matter.getName(), status);

    // 发送状态变更通知给项目参与人
    sendMatterStatusNotification(matter, oldStatus, status);

    // 如果状态改为 ARCHIVED，自动创建档案记录
    if (MatterConstants.STATUS_ARCHIVED.equals(status)) {
      try {
        // 检查是否已存在档案
        if (archiveAppService.getArchiveByMatterId(id) == null) {
          com.lawfirm.application.archive.command.CreateArchiveCommand archiveCmd =
              new com.lawfirm.application.archive.command.CreateArchiveCommand();
          archiveCmd.setMatterId(id);
          archiveCmd.setArchiveName(matter.getName() + " - 档案");
          // 根据项目类型设置档案类型
          if ("LITIGATION".equals(matter.getMatterType())) {
            archiveCmd.setArchiveType("LITIGATION");
          } else {
            archiveCmd.setArchiveType("NON_LITIGATION");
          }
          archiveCmd.setRetentionPeriod("10_YEARS"); // 默认10年
          archiveAppService.createArchiveFromMatter(archiveCmd);
          log.info("项目归档后自动创建档案: matterId={}", id);
        }
      } catch (Exception e) {
        log.error("项目归档后自动创建档案失败", e);
        // 不抛出异常，避免影响状态变更
      }
    }
  }

  /**
   * 发送项目状态变更通知给所有参与人
   *
   * @param matter 案件实体
   * @param oldStatus 旧状态
   * @param newStatus 新状态
   */
  private void sendMatterStatusNotification(
      final Matter matter, final String oldStatus, final String newStatus) {
    try {
      Long currentUserId = SecurityUtils.getUserId();
      String currentUserName = SecurityUtils.getRealName();

      // 获取项目参与人
      List<MatterParticipant> participants = participantMapper.selectByMatterId(matter.getId());

      String statusName = MatterConstants.getMatterStatusName(newStatus);
      String title = String.format("项目【%s】状态变更", matter.getName());
      String content =
          String.format("%s 将项目【%s】状态修改为：%s", currentUserName, matter.getName(), statusName);

      // 给所有参与人发送通知（排除操作人自己）
      for (MatterParticipant p : participants) {
        if (!p.getUserId().equals(currentUserId)) {
          notificationAppService.sendSystemNotification(
              p.getUserId(), title, content, "MATTER", matter.getId());
        }
      }

      log.info(
          "项目状态变更通知已发送: matterId={}, status={}, 通知人数={}",
          matter.getId(),
          newStatus,
          participants.size());
    } catch (Exception e) {
      log.warn("发送项目状态变更通知失败: matterId={}", matter.getId(), e);
    }
  }

  /**
   * 添加团队成员
   *
   * @param matterId 案件ID
   * @param userId 用户ID
   * @param role 角色代码
   * @param commissionRate 提成比例
   * @param isOriginator 是否案源人
   * @throws BusinessException 当成员已存在时抛出异常
   */
  @Transactional
  public void addParticipant(
      final Long matterId,
      final Long userId,
      final String role,
      final java.math.BigDecimal commissionRate,
      final Boolean isOriginator) {
    // 验证用户是否有权限访问该项目
    validateMatterAccess(matterId);

    // 验证用户是否是项目负责人或参与者（只有项目成员才能管理团队）
    validateMatterOwnership(matterId);

    // 检查是否已在团队中
    if (participantMapper.countByMatterIdAndUserId(matterId, userId) > 0) {
      throw new BusinessException("该成员已在案件团队中");
    }

    MatterParticipant participant =
        MatterParticipant.builder()
            .matterId(matterId)
            .userId(userId)
            .role(role)
            .commissionRate(commissionRate)
            .isOriginator(isOriginator != null ? isOriginator : false)
            .joinDate(LocalDate.now())
            .status(MatterConstants.STATUS_ACTIVE)
            .build();

    participantMapper.insert(participant);
    log.info("添加案件团队成员: matterId={}, userId={}, role={}", matterId, userId, role);

    // 发送通知给被添加的成员
    try {
      Matter matter = matterRepository.findById(matterId);
      if (matter != null) {
        String currentUserName = SecurityUtils.getRealName();
        String roleName = getRoleName(role);
        String title = "您已被添加到项目";
        String content =
            String.format("%s 将您添加到项目【%s】，您的角色是：%s", currentUserName, matter.getName(), roleName);
        notificationAppService.sendSystemNotification(userId, title, content, "MATTER", matterId);
      }
    } catch (Exception e) {
      log.warn("发送团队成员添加通知失败: matterId={}, userId={}", matterId, userId, e);
    }
  }

  /**
   * 获取角色名称
   *
   * @param role 角色代码
   * @return 角色名称
   */
  private String getRoleName(final String role) {
    if (role == null) {
      return "成员";
    }
    return switch (role) {
      case "LEAD_LAWYER", "LEAD" -> "主办律师";
      case "ASSISTANT", "CO_COUNSEL" -> "协办律师";
      case "PARALEGAL" -> "律师助理";
      case "TRAINEE" -> "实习律师";
      default -> role;
    };
  }

  /**
   * 移除团队成员
   *
   * @param matterId 案件ID
   * @param userId 用户ID
   */
  @Transactional
  public void removeParticipant(final Long matterId, final Long userId) {
    // 验证用户是否有权限访问该项目
    validateMatterAccess(matterId);

    // 验证用户是否是项目负责人或参与者（只有项目成员才能管理团队）
    validateMatterOwnership(matterId);

    participantMapper.delete(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MatterParticipant>()
            .eq(MatterParticipant::getMatterId, matterId)
            .eq(MatterParticipant::getUserId, userId));
    log.info("移除案件团队成员: matterId={}, userId={}", matterId, userId);
  }

  /**
   * 生成案件编号 格式：年份 + 创建人标识 + 类型码 + 顺序号 例如：2026张三MS-0001, 2026ZS001XS-0001
   *
   * @param originatorId 创建人ID
   * @param caseType 案件类型（CIVIL, CRIMINAL, ADMINISTRATIVE等）
   * @return 案件编号
   */
  private String generateMatterNo(final Long originatorId, final String caseType) {
    // 1. 获取年份（4位）
    String year = String.valueOf(LocalDate.now().getYear());

    // 2. 获取创建人标识（优先使用工号，否则使用姓名）
    User originator = userRepository.getByIdOrThrow(originatorId, "创建人不存在");
    String creatorIdentifier;
    if (StringUtils.hasText(originator.getEmployeeNo())) {
      creatorIdentifier = originator.getEmployeeNo(); // 工号，如：ZS001
    } else {
      creatorIdentifier = originator.getRealName(); // 姓名，如：张三
    }

    // 3. 根据案件类型生成类型码
    String typeCode = getCaseTypeCode(caseType);

    // 4. 构建前缀：年份 + 创建人标识 + 类型码 + "-"
    String prefix = year + creatorIdentifier + typeCode + "-";

    // 5. 查询该前缀下的最大序号
    Integer maxSeq =
        matterRepository.getBaseMapper().selectMaxSequenceByPrefix(prefix, prefix.length());
    int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

    // 6. 生成完整编号（序号4位，不足补0）
    return prefix + String.format("%04d", nextSeq);
  }

  /**
   * 根据案件类型获取类型码
   *
   * @param caseType 案件类型
   * @return 类型简码（如 MS, XS）
   */
  private String getCaseTypeCode(final String caseType) {
    if (caseType == null) {
      return "QT"; // 其他
    }
    return switch (caseType) {
      case "CIVIL" -> "MS"; // 民事
      case "CRIMINAL" -> "XS"; // 刑事
      case "ADMINISTRATIVE" -> "XZ"; // 行政
      case "ARBITRATION" -> "ZC"; // 仲裁
      case "BANKRUPTCY" -> "PC"; // 破产
      case "IP" -> "ZS"; // 知识产权
      case "ENFORCEMENT" -> "ZX"; // 执行
      case "LEGAL_COUNSEL" -> "GW"; // 法律顾问
      case "SPECIAL_SERVICE" -> "ZX"; // 专项服务
      default -> "QT"; // 其他
    };
  }

  /**
   * 验证状态流转（严格状态机） 状态流转规则： - DRAFT (草稿) -> PENDING (待审批) - PENDING (待审批) -> ACTIVE (进行中) / DRAFT
   * (退回草稿) - ACTIVE (进行中) -> SUSPENDED (暂停) / PENDING_CLOSE (待结案) - SUSPENDED (暂停) -> ACTIVE (恢复) /
   * PENDING_CLOSE (待结案) - PENDING_CLOSE (待结案) -> CLOSED (已结案) / ACTIVE (退回进行中) - CLOSED (已结案) ->
   * ARCHIVED (已归档) - ARCHIVED (已归档) -> 终态，不允许再变更
   *
   * @param from 当前状态
   * @param to 目标状态
   */
  private void validateStatusTransition(final String from, final String to) {
    if (from == null || to == null) {
      throw new BusinessException("状态不能为空");
    }

    // 相同状态无需验证
    if (from.equals(to)) {
      return;
    }

    // 定义允许的状态流转关系
    java.util.Map<String, List<String>> allowedTransitions =
        java.util.Map.of(
            MatterConstants.STATUS_DRAFT, List.of(MatterConstants.STATUS_PENDING), // 草稿 -> 待审批
            MatterConstants.STATUS_PENDING,
                List.of(
                    MatterConstants.STATUS_ACTIVE, MatterConstants.STATUS_DRAFT), // 待审批 -> 进行中/草稿
            MatterConstants.STATUS_ACTIVE,
                List.of(
                    MatterConstants.STATUS_SUSPENDED,
                    MatterConstants.STATUS_PENDING_CLOSE), // 进行中 -> 暂停/待结案
            MatterConstants.STATUS_SUSPENDED,
                List.of(
                    MatterConstants.STATUS_ACTIVE,
                    MatterConstants.STATUS_PENDING_CLOSE), // 暂停 -> 进行中/待结案
            MatterConstants.STATUS_PENDING_CLOSE,
                List.of(
                    MatterConstants.STATUS_CLOSED, MatterConstants.STATUS_ACTIVE), // 待结案 -> 已结案/进行中
            MatterConstants.STATUS_CLOSED, List.of(MatterConstants.STATUS_ARCHIVED) // 已结案 -> 已归档
            // ARCHIVED 是终态，不允许再变更
            );

    List<String> allowed = allowedTransitions.get(from);
    if (allowed == null) {
      // 终态或未知状态
      throw new BusinessException(String.format("状态 [%s] 不允许变更", getStatusName(from)));
    }

    if (!allowed.contains(to)) {
      String allowedNames =
          allowed.stream()
              .map(this::getStatusName)
              .collect(java.util.stream.Collectors.joining("、"));
      throw new BusinessException(
          String.format(
              "不允许的状态流转: %s -> %s。当前状态只能变更为: %s",
              getStatusName(from), getStatusName(to), allowedNames));
    }

    log.debug("状态流转验证通过: {} -> {}", from, to);
  }

  /**
   * 获取状态中文名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    return MatterConstants.getMatterStatusName(status);
  }

  /**
   * 获取收费方式名称
   *
   * @param type 收费方式代码
   * @return 收费方式名称
   */
  private String getFeeTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "FIXED" -> "固定收费";
      case "HOURLY" -> "计时收费";
      case "CONTINGENCY" -> "风险代理";
      case "MIXED" -> "混合收费";
      default -> type;
    };
  }

  /**
   * 验证用户是否有权限访问指定的项目（用于查看）
   *
   * @param matterId 项目ID
   * @throws BusinessException 如果用户无权访问该项目
   */
  private void validateMatterAccess(final Long matterId) {
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();
    List<Long> accessibleMatterIds = getAccessibleMatterIds(dataScope, currentUserId, deptId);

    // 如果返回null，表示可以访问所有项目（ALL权限）
    // 否则检查项目ID是否在可访问列表中
    if (accessibleMatterIds != null && !accessibleMatterIds.contains(matterId)) {
      throw new BusinessException("无权访问该项目");
    }
  }

  /**
   * 验证用户是否是项目的负责人或参与者（用于编辑/归档等操作） 只有项目负责人和参与者才能编辑项目，管理员除外
   *
   * @param matterId 项目ID
   * @throws BusinessException 如果用户不是项目成员
   */
  public void validateMatterOwnership(final Long matterId) {
    // 管理员可以操作所有项目
    if (SecurityUtils.isAdmin()) {
      return;
    }

    Long currentUserId = SecurityUtils.getUserId();
    Matter matter = matterRepository.findById(matterId);
    if (matter == null) {
      throw new BusinessException("项目不存在");
    }

    // 检查是否是项目负责人
    if (currentUserId.equals(matter.getLeadLawyerId())) {
      return;
    }

    // 检查是否是项目参与者
    boolean isParticipant =
        matterParticipantRepository
            .lambdaQuery()
            .eq(MatterParticipant::getMatterId, matterId)
            .eq(MatterParticipant::getUserId, currentUserId)
            .eq(MatterParticipant::getStatus, MatterConstants.STATUS_ACTIVE)
            .eq(MatterParticipant::getDeleted, false)
            .exists();

    if (!isParticipant) {
      throw new BusinessException("只有项目负责人和参与者才能执行此操作");
    }
  }

  /**
   * 获取部门及所有下级部门ID列表 使用递归CTE一次性查询
   *
   * @param deptId 部门ID
   * @return 部门ID列表
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
   * 获取可访问的项目ID列表（根据数据权限）
   *
   * @param dataScope 数据权限
   * @param currentUserId 当前用户ID
   * @param deptId 部门ID
   * @return null表示可以访问所有项目，否则返回可访问的项目ID列表
   */
  public List<Long> getAccessibleMatterIds(
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

      log.debug("SELF权限 - 用户{}负责的项目ID列表: {}", currentUserId, leadMatterIds);

      // 查询自己参与的项目（只查询状态为ACTIVE的参与关系）
      var participantList =
          matterParticipantRepository
              .lambdaQuery()
              .select(MatterParticipant::getMatterId)
              .eq(MatterParticipant::getUserId, currentUserId)
              .eq(MatterParticipant::getStatus, MatterConstants.STATUS_ACTIVE)
              .eq(MatterParticipant::getDeleted, false)
              .list();

      List<Long> participantMatterIds =
          participantList.stream()
              .map(MatterParticipant::getMatterId)
              .distinct()
              .collect(Collectors.toList());

      log.debug("SELF权限 - 用户{}参与的项目ID列表（ACTIVE）: {}", currentUserId, participantMatterIds);

      // 合并去重
      matterIds.addAll(leadMatterIds);
      matterIds.addAll(participantMatterIds);
      matterIds = matterIds.stream().distinct().collect(Collectors.toList());

      log.debug("SELF权限 - 用户{}最终可访问的项目ID列表: {}", currentUserId, matterIds);
    }

    return matterIds.isEmpty() ? Collections.emptyList() : matterIds;
  }

  /**
   * Matter Entity 转 DTO
   *
   * @param matter 案件实体
   * @return 案件DTO
   */
  private MatterDTO toDTO(final Matter matter) {
    MatterDTO dto = new MatterDTO();
    dto.setId(matter.getId());
    dto.setMatterNo(matter.getMatterNo());
    dto.setName(matter.getName());
    dto.setMatterType(matter.getMatterType());
    dto.setMatterTypeName(MatterConstants.getMatterTypeName(matter.getMatterType()));
    dto.setCaseType(matter.getCaseType());
    dto.setCaseTypeName(MatterConstants.getCaseTypeName(matter.getCaseType()));
    dto.setLitigationStage(matter.getLitigationStage());
    dto.setLitigationStageName(MatterConstants.getLitigationStageName(matter.getLitigationStage()));
    dto.setCauseOfAction(matter.getCauseOfAction());
    // 案由名称由前端根据code查找，后端只存储code
    dto.setBusinessType(matter.getBusinessType());
    dto.setClientId(matter.getClientId());
    dto.setOpposingParty(matter.getOpposingParty());
    dto.setOpposingLawyerName(matter.getOpposingLawyerName());
    dto.setOpposingLawyerLicenseNo(matter.getOpposingLawyerLicenseNo());
    dto.setOpposingLawyerFirm(matter.getOpposingLawyerFirm());
    dto.setOpposingLawyerPhone(matter.getOpposingLawyerPhone());
    dto.setOpposingLawyerEmail(matter.getOpposingLawyerEmail());
    dto.setDescription(matter.getDescription());
    dto.setStatus(matter.getStatus());
    dto.setStatusName(MatterConstants.getMatterStatusName(matter.getStatus()));
    dto.setOriginatorId(matter.getOriginatorId());
    dto.setLeadLawyerId(matter.getLeadLawyerId());
    dto.setDepartmentId(matter.getDepartmentId());
    dto.setFeeType(matter.getFeeType());
    dto.setFeeTypeName(getFeeTypeName(matter.getFeeType()));
    dto.setEstimatedFee(matter.getEstimatedFee());
    dto.setActualFee(matter.getActualFee());
    dto.setFilingDate(matter.getFilingDate());
    dto.setExpectedClosingDate(matter.getExpectedClosingDate());
    dto.setActualClosingDate(matter.getActualClosingDate());
    dto.setClaimAmount(matter.getClaimAmount());
    dto.setOutcome(matter.getOutcome());
    dto.setContractId(matter.getContractId());
    dto.setRemark(matter.getRemark());
    dto.setConflictStatus(matter.getConflictStatus());
    dto.setCreatedAt(matter.getCreatedAt());
    dto.setUpdatedAt(matter.getUpdatedAt());

    // 查询关联数据
    if (matter.getClientId() != null) {
      var client = clientRepository.findById(matter.getClientId());
      if (client != null) {
        dto.setClientName(client.getName());
      }
    }

    // 查询合同编号和合同金额
    if (matter.getContractId() != null) {
      try {
        var contract = contractRepository.findById(matter.getContractId());
        if (contract != null) {
          dto.setContractNo(contract.getContractNo());
          dto.setContractAmount(contract.getTotalAmount());
        }
      } catch (Exception e) {
        log.warn("获取合同信息失败: contractId={}", matter.getContractId(), e);
      }
    }

    // 加载多客户列表
    List<MatterClient> matterClients = matterClientRepository.findByMatterId(matter.getId());
    if (matterClients != null && !matterClients.isEmpty()) {
      dto.setClients(
          matterClients.stream().map(this::toMatterClientDTO).collect(Collectors.toList()));
    }

    if (matter.getOriginatorId() != null) {
      var user = userRepository.findById(matter.getOriginatorId());
      if (user != null) {
        dto.setOriginatorName(user.getRealName());
      }
    }
    if (matter.getLeadLawyerId() != null) {
      var user = userRepository.findById(matter.getLeadLawyerId());
      if (user != null) {
        dto.setLeadLawyerName(user.getRealName());
      }
    }
    if (matter.getDepartmentId() != null) {
      var dept = departmentRepository.findById(matter.getDepartmentId());
      if (dept != null) {
        dto.setDepartmentName(dept.getName());
      }
    }

    return dto;
  }

  /**
   * Participant Entity 转 DTO
   *
   * @param p 参与人实体
   * @return 参与人DTO
   */
  private MatterParticipantDTO toParticipantDTO(final MatterParticipant p) {
    MatterParticipantDTO dto = new MatterParticipantDTO();
    dto.setId(p.getId());
    dto.setMatterId(p.getMatterId());
    dto.setUserId(p.getUserId());
    dto.setRole(p.getRole());
    dto.setRoleName(getRoleName(p.getRole()));
    dto.setCommissionRate(p.getCommissionRate());
    dto.setIsOriginator(p.getIsOriginator());
    dto.setJoinDate(p.getJoinDate());
    dto.setExitDate(p.getExitDate());
    dto.setStatus(p.getStatus());
    dto.setRemark(p.getRemark());

    // 查询用户信息
    if (p.getUserId() != null) {
      User user = userRepository.findById(p.getUserId());
      if (user != null) {
        dto.setUserName(user.getRealName());
        dto.setUserPosition(user.getPosition());
      }
    }

    return dto;
  }

  /**
   * MatterClient Entity 转 DTO
   *
   * @param mc 案件客户关联实体
   * @return 案件客户关联DTO
   */
  private MatterClientDTO toMatterClientDTO(final MatterClient mc) {
    MatterClientDTO dto = new MatterClientDTO();
    dto.setId(mc.getId());
    dto.setMatterId(mc.getMatterId());
    dto.setClientId(mc.getClientId());
    dto.setClientRole(mc.getClientRole());
    dto.setClientRoleName(MatterClientDTO.getClientRoleName(mc.getClientRole()));
    dto.setIsPrimary(mc.getIsPrimary());

    // 查询客户信息
    if (mc.getClientId() != null) {
      var client = clientRepository.findById(mc.getClientId());
      if (client != null) {
        dto.setClientName(client.getName());
        dto.setClientType(client.getClientType());
      }
    }

    return dto;
  }

  /**
   * 保存项目的多客户关联
   *
   * @param matterId 案件ID
   * @param command 创建命令
   */
  private void saveMatterClients(final Long matterId, final CreateMatterCommand command) {
    // 如果有显式的客户列表，使用列表
    if (command.getClients() != null && !command.getClients().isEmpty()) {
      boolean hasPrimary = false;
      for (CreateMatterCommand.ClientCommand cc : command.getClients()) {
        MatterClient mc =
            MatterClient.builder()
                .matterId(matterId)
                .clientId(cc.getClientId())
                .clientRole(cc.getClientRole() != null ? cc.getClientRole() : "PLAINTIFF")
                .isPrimary(Boolean.TRUE.equals(cc.getIsPrimary()))
                .build();
        matterClientRepository.save(mc);
        if (Boolean.TRUE.equals(cc.getIsPrimary())) {
          hasPrimary = true;
        }
      }
      // 如果没有指定主要客户，将第一个设为主要客户
      if (!hasPrimary) {
        List<MatterClient> clients = matterClientRepository.findByMatterId(matterId);
        if (!clients.isEmpty()) {
          MatterClient first = clients.get(0);
          first.setIsPrimary(true);
          matterClientRepository.updateById(first);
        }
      }
    } else {
      // 向后兼容：如果只有单个clientId，创建一条记录
      if (command.getClientId() != null) {
        MatterClient mc =
            MatterClient.builder()
                .matterId(matterId)
                .clientId(command.getClientId())
                .clientRole("PLAINTIFF")
                .isPrimary(true)
                .build();
        matterClientRepository.save(mc);
      }
    }
  }

  /**
   * 申请项目结案
   *
   * @param command 结案申请命令
   * @return 更新后的案件DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public MatterDTO applyCloseMatter(final CloseMatterCommand command) {
    Matter matter = matterRepository.findById(command.getMatterId());
    if (matter == null) {
      throw new BusinessException("项目不存在");
    }

    // 验证用户是否有权限访问该项目
    validateMatterAccess(command.getMatterId());

    // 验证用户是否是项目负责人或参与者（只有项目成员才能申请结案）
    validateMatterOwnership(command.getMatterId());

    if (!MatterConstants.STATUS_ACTIVE.equals(matter.getStatus())
        && !MatterConstants.STATUS_SUSPENDED.equals(matter.getStatus())) {
      throw new BusinessException("只有进行中或暂停状态的项目才能申请结案");
    }

    // 更新项目信息
    matter.setActualClosingDate(command.getClosingDate());
    matter.setOutcome(command.getOutcome());
    if (command.getRemark() != null) {
      matter.setRemark(
          (matter.getRemark() != null ? matter.getRemark() + "\n" : "")
              + "结案申请: "
              + command.getClosingReason()
              + (command.getSummary() != null ? "\n结案总结: " + command.getSummary() : ""));
    }
    matter.setStatus(MatterConstants.STATUS_PENDING_CLOSE); // 待审批结案状态
    matter.setUpdatedAt(java.time.LocalDateTime.now());
    matter.setUpdatedBy(SecurityUtils.getUserId());
    matterRepository.getBaseMapper().updateById(matter);

    // 创建结案审批记录
    try {
      // 使用指定的审批人，如果没有指定则使用结案审批人（优先团队负责人）
      Long approverId =
          command.getApproverId() != null
              ? command.getApproverId()
              : approverService.findMatterCloseApprover();
      String businessSnapshot = objectMapper.writeValueAsString(matter);
      approvalService.createApproval(
          "MATTER_CLOSE",
          matter.getId(),
          matter.getMatterNo(),
          "项目结案申请：" + matter.getName(),
          approverId,
          "NORMAL",
          "NORMAL",
          businessSnapshot);
    } catch (Exception e) {
      log.error("创建结案审批记录失败", e);
      // 不阻断主流程
    }

    log.info("申请项目结案: matterId={}, matterNo={}", command.getMatterId(), matter.getMatterNo());

    return toDTO(matter);
  }

  /**
   * 审批项目结案
   *
   * @param matterId 案件ID
   * @param approved 是否批准
   * @param comment 审批意见
   * @return 更新后的案件DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public MatterDTO approveCloseMatter(
      final Long matterId, final Boolean approved, final String comment) {
    Matter matter = matterRepository.findById(matterId);
    if (matter == null) {
      throw new BusinessException("项目不存在");
    }

    // 验证用户是否有权限访问该项目
    validateMatterAccess(matterId);

    if (!MatterConstants.STATUS_PENDING_CLOSE.equals(matter.getStatus())) {
      throw new BusinessException("项目不在待审批结案状态");
    }

    if (Boolean.TRUE.equals(approved)) {
      // 批准结案
      matter.setStatus(MatterConstants.STATUS_CLOSED);
      matter.setUpdatedAt(java.time.LocalDateTime.now());
      matter.setUpdatedBy(SecurityUtils.getUserId());
      matterRepository.getBaseMapper().updateById(matter);

      // 发送通知给主办律师，提醒完善卷宗后提交档案入库
      // 注意：不再自动创建档案，由律师自行完善卷宗后手动创建档案并提交入库审批
      sendArchiveReminderNotification(matter);

      log.info("项目结案审批通过: matterId={}, matterNo={}", matterId, matter.getMatterNo());
    } else {
      // 驳回结案申请
      matter.setStatus(MatterConstants.STATUS_ACTIVE); // 恢复为进行中状态
      matter.setUpdatedAt(java.time.LocalDateTime.now());
      matter.setUpdatedBy(SecurityUtils.getUserId());
      if (comment != null) {
        matter.setRemark(
            (matter.getRemark() != null ? matter.getRemark() + "\n" : "") + "结案申请被驳回: " + comment);
      }
      matterRepository.getBaseMapper().updateById(matter);

      log.info(
          "项目结案审批驳回: matterId={}, matterNo={}, comment={}",
          matterId,
          matter.getMatterNo(),
          comment);
    }

    return toDTO(matter);
  }

  /**
   * 生成结案报告（简化版，返回项目基本信息）
   *
   * @param matterId 案件ID
   * @return 结案报告内容
   */
  public String generateCloseReport(final Long matterId) {
    Matter matter = matterRepository.findById(matterId);
    if (matter == null) {
      throw new BusinessException("项目不存在");
    }

    // 验证用户是否有权限访问该项目
    validateMatterAccess(matterId);

    if (!MatterConstants.STATUS_CLOSED.equals(matter.getStatus())) {
      throw new BusinessException("只有已结案的项目才能生成结案报告");
    }

    // 生成简单的结案报告文本
    StringBuilder report = new StringBuilder();
    report.append("项目结案报告\n");
    report.append("==================\n\n");
    report.append("项目编号: ").append(matter.getMatterNo()).append("\n");
    report.append("项目名称: ").append(matter.getName()).append("\n");
    report
        .append("项目类型: ")
        .append(MatterConstants.getMatterTypeName(matter.getMatterType()))
        .append("\n");
    report.append("立案日期: ").append(matter.getFilingDate()).append("\n");
    report.append("结案日期: ").append(matter.getActualClosingDate()).append("\n");
    report
        .append("判决/调解结果: ")
        .append(matter.getOutcome() != null ? matter.getOutcome() : "无")
        .append("\n");
    report
        .append("实际收费: ")
        .append(matter.getActualFee() != null ? matter.getActualFee() : "0")
        .append("元\n");
    report
        .append("\n备注: ")
        .append(matter.getRemark() != null ? matter.getRemark() : "无")
        .append("\n");

    return report.toString();
  }

  /**
   * 发送档案入库提醒通知
   *
   * <p>结案审批通过后，通知主办律师完善卷宗并提交档案入库。
   * 律师需要：1. 检查并完善卷宗材料 2. 到档案管理创建档案 3. 提交入库审批
   *
   * @param matter 案件实体
   */
  private void sendArchiveReminderNotification(final Matter matter) {
    String content =
        String.format(
            "项目【%s】（%s）已结案，请完善卷宗材料后到档案管理创建档案并提交入库审批。",
            matter.getName(), matter.getMatterNo());

    try {
      // 通知主办律师
      if (matter.getLeadLawyerId() != null) {
        notificationAppService.sendSystemNotification(
            matter.getLeadLawyerId(), "项目结案-请提交档案入库", content, "ARCHIVE", matter.getId());
      }

      // 通知案源人（如果不同于主办律师）
      if (matter.getOriginatorId() != null
          && !matter.getOriginatorId().equals(matter.getLeadLawyerId())) {
        notificationAppService.sendSystemNotification(
            matter.getOriginatorId(), "项目结案-请提交档案入库", content, "ARCHIVE", matter.getId());
      }

      log.info("已发送档案入库提醒: matterId={}, matterNo={}", matter.getId(), matter.getMatterNo());
    } catch (Exception e) {
      log.warn("发送档案入库提醒失败: matterId={}, error={}", matter.getId(), e.getMessage());
      // 不影响主流程
    }
  }

  /**
   * 从合同复制参与人到项目 当基于合同创建项目时，自动将合同参与人复制为项目参与人
   *
   * @param contractId 合同ID
   * @param matterId 案件ID
   */
  private void copyContractParticipantsToMatter(final Long contractId, final Long matterId) {
    List<ContractParticipant> contractParticipants =
        contractParticipantRepository.findByContractId(contractId);

    if (contractParticipants == null || contractParticipants.isEmpty()) {
      log.debug("合同无参与人，跳过复制: contractId={}", contractId);
      return;
    }

    for (ContractParticipant cp : contractParticipants) {
      // 检查是否已存在
      if (participantMapper.countByMatterIdAndUserId(matterId, cp.getUserId()) > 0) {
        log.debug("参与人已存在，跳过: matterId={}, userId={}", matterId, cp.getUserId());
        continue;
      }

      // 映射合同参与人角色到项目参与人角色
      String matterRole = mapContractRoleToMatterRole(cp.getRole());
      boolean isOriginator = "ORIGINATOR".equals(cp.getRole());

      MatterParticipant mp =
          MatterParticipant.builder()
              .matterId(matterId)
              .userId(cp.getUserId())
              .role(matterRole)
              .commissionRate(cp.getCommissionRate())
              .isOriginator(isOriginator)
              .joinDate(LocalDate.now())
              .status(MatterConstants.STATUS_ACTIVE)
              .remark("从合同自动复制")
              .build();

      participantMapper.insert(mp);
      log.debug(
          "复制合同参与人到项目: contractId={}, matterId={}, userId={}, role={}",
          contractId,
          matterId,
          cp.getUserId(),
          matterRole);
    }

    log.info(
        "从合同复制参与人到项目完成: contractId={}, matterId={}, count={}",
        contractId,
        matterId,
        contractParticipants.size());
  }

  /**
   * 映射合同参与人角色到项目参与人角色
   *
   * @param contractRole 合同角色代码
   * @return 项目角色代码
   */
  private String mapContractRoleToMatterRole(final String contractRole) {
    if (contractRole == null) {
      return "CO_COUNSEL";
    }
    return switch (contractRole) {
      case "LEAD" -> "LEAD"; // 承办律师 -> 主办律师
      case "CO_COUNSEL" -> "CO_COUNSEL"; // 协办律师 -> 协办律师
      case "ORIGINATOR" -> "CO_COUNSEL"; // 案源人 -> 协办律师（isOriginator标记为true）
      case "PARALEGAL" -> "PARALEGAL"; // 律师助理 -> 律师助理
      default -> "CO_COUNSEL";
    };
  }
}
