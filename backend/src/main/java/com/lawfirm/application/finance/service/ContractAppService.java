package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.service.FileAccessService;
import com.lawfirm.application.finance.command.ContractChangeCommand;
import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractParticipantDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.constant.ContractStatus;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.event.ContractAmendedEvent;
import com.lawfirm.domain.finance.event.ContractApprovedEvent;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 合同应用服务 */
@Slf4j
@Service("financeContractAppService")
@RequiredArgsConstructor
public class ContractAppService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 合同Mapper */
  private final ContractMapper contractMapper;

  /** 客户仓储 */
  private final ClientRepository clientRepository;

  /** 案件仓储 */
  private final MatterRepository matterRepository;

  /** 审批服务 */
  private final ApprovalService approvalService;

  /** 审批应用服务 */
  private final ApprovalAppService approvalAppService;

  /** 审批人服务 */
  private final ApproverService approverService;

  /** 合同参与人仓储 */
  private final ContractParticipantRepository participantRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /** 用户Mapper */
  private final UserMapper userMapper;

  /** 应用事件发布器 */
  private final ApplicationEventPublisher eventPublisher;

  /** 合同编号生成器 */
  private final ContractNumberGenerator contractNumberGenerator;

  /** 案由服务 */
  private final CauseOfActionService causeOfActionService;

  /** 审批Mapper */
  private final ApprovalMapper approvalMapper;

  /** 文件访问服务 */
  private final FileAccessService fileAccessService;

  /** 合同模板变量服务 */
  private final ContractTemplateVariableService contractTemplateVariableService;

  /** 合同名称服务（延迟加载以打破循环依赖） */
  @Lazy @Autowired private ContractNameService contractNameService;

  /**
   * 分页查询合同
   *
   * @param query 查询条件对象，包含合同编号、名称、客户ID等筛选条件
   * @return 分页结果对象，包含合同DTO列表和分页信息
   */
  public PageResult<ContractDTO> listContracts(final ContractQueryDTO query) {
    LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();

    if (StringUtils.hasText(query.getContractNo())) {
      wrapper.like(Contract::getContractNo, query.getContractNo());
    }
    if (StringUtils.hasText(query.getName())) {
      wrapper.like(Contract::getName, query.getName());
    }
    if (query.getClientId() != null) {
      wrapper.eq(Contract::getClientId, query.getClientId());
    }
    if (query.getMatterId() != null) {
      wrapper.eq(Contract::getMatterId, query.getMatterId());
    }
    if (StringUtils.hasText(query.getContractType())) {
      wrapper.eq(Contract::getContractType, query.getContractType());
    }
    if (StringUtils.hasText(query.getStatus())) {
      wrapper.eq(Contract::getStatus, query.getStatus());
    }
    if (query.getSignDateFrom() != null) {
      wrapper.ge(Contract::getSignDate, query.getSignDateFrom());
    }
    if (query.getSignDateTo() != null) {
      wrapper.le(Contract::getSignDate, query.getSignDateTo());
    }
    if (query.getSignerId() != null) {
      wrapper.eq(Contract::getSignerId, query.getSignerId());
    }
    // 新增筛选条件
    if (StringUtils.hasText(query.getFeeType())) {
      wrapper.eq(Contract::getFeeType, query.getFeeType());
    }
    if (query.getDepartmentId() != null) {
      wrapper.eq(Contract::getDepartmentId, query.getDepartmentId());
    }
    if (query.getEffectiveDateFrom() != null) {
      wrapper.ge(Contract::getEffectiveDate, query.getEffectiveDateFrom());
    }
    if (query.getEffectiveDateTo() != null) {
      wrapper.le(Contract::getEffectiveDate, query.getEffectiveDateTo());
    }
    if (query.getExpiryDateFrom() != null) {
      wrapper.ge(Contract::getExpiryDate, query.getExpiryDateFrom());
    }
    if (query.getExpiryDateTo() != null) {
      wrapper.le(Contract::getExpiryDate, query.getExpiryDateTo());
    }
    if (query.getAmountMin() != null) {
      wrapper.ge(Contract::getTotalAmount, query.getAmountMin());
    }
    if (query.getAmountMax() != null) {
      wrapper.le(Contract::getTotalAmount, query.getAmountMax());
    }
    if (query.getClaimAmountMin() != null) {
      wrapper.ge(Contract::getClaimAmount, query.getClaimAmountMin());
    }
    if (query.getClaimAmountMax() != null) {
      wrapper.le(Contract::getClaimAmount, query.getClaimAmountMax());
    }
    if (StringUtils.hasText(query.getTrialStage())) {
      wrapper.eq(Contract::getTrialStage, query.getTrialStage());
    }
    if (StringUtils.hasText(query.getConflictCheckStatus())) {
      wrapper.eq(Contract::getConflictCheckStatus, query.getConflictCheckStatus());
    }
    if (StringUtils.hasText(query.getArchiveStatus())) {
      wrapper.eq(Contract::getArchiveStatus, query.getArchiveStatus());
    }
    // 创建时间筛选
    if (query.getCreatedAtFrom() != null) {
      wrapper.ge(Contract::getCreatedAt, query.getCreatedAtFrom());
    }
    if (query.getCreatedAtTo() != null) {
      wrapper.le(Contract::getCreatedAt, query.getCreatedAtTo());
    }

    // 数据范围过滤
    applyDataScopeFilter(wrapper);

    wrapper.orderByDesc(Contract::getCreatedAt);

    IPage<Contract> page =
        contractRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<Contract> contracts = page.getRecords();
    if (contracts.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 批量加载客户信息，避免N+1查询
    Set<Long> clientIds =
        contracts.stream()
            .map(Contract::getClientId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Client> clientMap =
        clientIds.isEmpty()
            ? Collections.emptyMap()
            : clientRepository.listByIds(clientIds).stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

    // 批量加载项目信息，避免N+1查询
    Set<Long> matterIds =
        contracts.stream()
            .map(Contract::getMatterId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, com.lawfirm.domain.matter.entity.Matter> matterMap =
        matterIds.isEmpty()
            ? Collections.emptyMap()
            : matterRepository.listByIds(matterIds).stream()
                .collect(Collectors.toMap(com.lawfirm.domain.matter.entity.Matter::getId, m -> m));

    // 使用批量加载的数据转换DTO
    List<ContractDTO> records =
        contracts.stream().map(c -> toDTO(c, clientMap, matterMap)).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取我的合同（仅自己创建或签约的合同，不受数据范围限制）
   *
   * @param query 查询条件对象
   * @return 分页结果对象
   */
  public PageResult<ContractDTO> getMyContracts(final ContractQueryDTO query) {
    Long currentUserId = SecurityUtils.getCurrentUserId();

    LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();

    // 只查询自己创建或签约的合同
    List<Long> participatingContractIds = getParticipatingContractIds(currentUserId);
    if (participatingContractIds.isEmpty()) {
      wrapper.and(
          w ->
              w.eq(Contract::getSignerId, currentUserId)
                  .or()
                  .eq(Contract::getCreatedBy, currentUserId));
    } else {
      wrapper.and(
          w ->
              w.eq(Contract::getSignerId, currentUserId)
                  .or()
                  .eq(Contract::getCreatedBy, currentUserId)
                  .or()
                  .in(Contract::getId, participatingContractIds));
    }

    // 其他筛选条件
    if (StringUtils.hasText(query.getContractNo())) {
      wrapper.like(Contract::getContractNo, query.getContractNo());
    }
    if (StringUtils.hasText(query.getName())) {
      wrapper.like(Contract::getName, query.getName());
    }
    if (StringUtils.hasText(query.getStatus())) {
      wrapper.eq(Contract::getStatus, query.getStatus());
    }
    if (query.getSignDateFrom() != null) {
      wrapper.ge(Contract::getSignDate, query.getSignDateFrom());
    }
    if (query.getSignDateTo() != null) {
      wrapper.le(Contract::getSignDate, query.getSignDateTo());
    }
    // 创建时间筛选
    if (query.getCreatedAtFrom() != null) {
      wrapper.ge(Contract::getCreatedAt, query.getCreatedAtFrom());
    }
    if (query.getCreatedAtTo() != null) {
      wrapper.le(Contract::getCreatedAt, query.getCreatedAtTo());
    }

    wrapper.orderByDesc(Contract::getCreatedAt);

    IPage<Contract> page =
        contractRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<Contract> contracts = page.getRecords();
    if (contracts.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // 批量加载客户信息
    Set<Long> clientIds =
        contracts.stream()
            .map(Contract::getClientId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Client> clientMap =
        clientIds.isEmpty()
            ? Collections.emptyMap()
            : clientRepository.listByIds(clientIds).stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

    // 批量加载项目信息
    Set<Long> matterIds =
        contracts.stream()
            .map(Contract::getMatterId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, com.lawfirm.domain.matter.entity.Matter> matterMap =
        matterIds.isEmpty()
            ? Collections.emptyMap()
            : matterRepository.listByIds(matterIds).stream()
                .collect(Collectors.toMap(com.lawfirm.domain.matter.entity.Matter::getId, m -> m));

    List<ContractDTO> records =
        contracts.stream().map(c -> toDTO(c, clientMap, matterMap)).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建合同
   *
   * @param command 创建合同命令对象
   * @return 创建成功的合同DTO
   * @throws BusinessException 当客户不存在或状态无效时抛出异常
   */
  @Transactional
  public ContractDTO createContract(final CreateContractCommand command) {
    // 1. 验证客户存在且状态为正式客户
    Client client = clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
    if (!"ACTIVE".equals(client.getStatus())) {
      throw new BusinessException("只能为已转正的正式客户创建合同，当前客户状态为：" + client.getStatus());
    }

    // 2. 验证案件存在（如果指定）
    if (command.getMatterId() != null) {
      matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
    }

    // 3. 验证风险代理比例范围
    if (command.getRiskRatio() != null) {
      if (command.getRiskRatio().compareTo(BigDecimal.ZERO) < 0
          || command.getRiskRatio().compareTo(new BigDecimal("100")) > 0) {
        throw new BusinessException("风险代理比例必须在0-100之间");
      }
    }

    // 4. 草稿状态不生成合同编号，编号在审批通过时生成
    // 这样可以避免未通过审批的合同占用编号，只有审批通过的合同才会获得正式编号
    String contractNo = null;

    // 5. 合同名称（如果为空则自动生成）
    String contractName = command.getName();
    if (contractName == null || contractName.isBlank()) {
      contractName = contractNameService.generateContractName(command, client);
    }

    // 6. 创建合同实体
    Contract contract =
        Contract.builder()
            .contractNo(contractNo)
            .name(contractName)
            .contractType(command.getContractType())
            .clientId(command.getClientId())
            .matterId(command.getMatterId())
            .feeType(command.getFeeType())
            .totalAmount(command.getTotalAmount())
            .paidAmount(BigDecimal.ZERO)
            .currency(command.getCurrency() != null ? command.getCurrency() : "CNY")
            .signDate(command.getSignDate() != null ? command.getSignDate() : LocalDate.now())
            .effectiveDate(command.getEffectiveDate())
            .expiryDate(command.getExpiryDate())
            .status(ContractStatus.DRAFT)
            .signerId(
                command.getSignerId() != null ? command.getSignerId() : SecurityUtils.getUserId())
            .departmentId(
                command.getDepartmentId() != null
                    ? command.getDepartmentId()
                    : SecurityUtils.getDepartmentId())
            .paymentTerms(command.getPaymentTerms())
            .fileUrl(command.getFileUrl())
            .remark(command.getRemark())
            // 扩展字段
            .trialStage(command.getTrialStage())
            .claimAmount(command.getClaimAmount())
            .jurisdictionCourt(command.getJurisdictionCourt())
            .opposingParty(command.getOpposingParty())
            .conflictCheckStatus(
                command.getConflictCheckStatus() != null
                    ? command.getConflictCheckStatus()
                    : "NOT_REQUIRED")
            .archiveStatus("NOT_ARCHIVED")
            .caseType(command.getCaseType())
            .causeOfAction(command.getCauseOfAction())
            .advanceTravelFee(command.getAdvanceTravelFee())
            .riskRatio(command.getRiskRatio())
            // 提成分配方案
            .commissionRuleId(command.getCommissionRuleId())
            .firmRate(command.getFirmRate())
            .leadLawyerRate(command.getLeadLawyerRate())
            .assistLawyerRate(command.getAssistLawyerRate())
            .supportStaffRate(command.getSupportStaffRate())
            .originatorRate(command.getOriginatorRate())
            .caseSummary(command.getCaseSummary())
            .build();

    // 处理文件URL：如果提供了fileUrl，尝试解析并设置新字段（向后兼容）
    if (command.getFileUrl() != null && !command.getFileUrl().isEmpty()) {
      setFileStorageInfo(contract, command.getFileUrl());
    }

    // 调试日志：打印保存前的提成方案数据
    log.info("=== 保存合同前的提成方案数据 ===");
    log.info("commissionRuleId: {}", contract.getCommissionRuleId());
    log.info("firmRate: {}", contract.getFirmRate());
    log.info("leadLawyerRate: {}", contract.getLeadLawyerRate());
    log.info("assistLawyerRate: {}", contract.getAssistLawyerRate());
    log.info("supportStaffRate: {}", contract.getSupportStaffRate());
    log.info("originatorRate: {}", contract.getOriginatorRate());
    log.info("=================================");

    // 6. 保存合同
    contractRepository.save(contract);

    // 调试日志：打印保存后的合同ID
    log.info("合同保存成功，ID: {}", contract.getId());

    // 7. 自动添加参与人（确保"我的收款"能显示数据）
    // 优先使用签约人，如果没有则使用当前登录用户
    Long participantUserId =
        contract.getSignerId() != null ? contract.getSignerId() : SecurityUtils.getUserId();
    if (participantUserId != null) {
      try {
        // 检查是否已存在参与人
        if (!participantRepository.existsByContractIdAndUserId(
            contract.getId(), participantUserId)) {
          // 根据提成方案设置角色和提成比例
          String role = "LEAD"; // 默认为主办律师
          BigDecimal commissionRate = command.getLeadLawyerRate(); // 使用提成方案中的主办律师比例

          ContractParticipant participant =
              ContractParticipant.builder()
                  .contractId(contract.getId())
                  .userId(participantUserId)
                  .role(role)
                  .commissionRate(commissionRate)
                  .remark("合同创建时自动添加")
                  .build();

          participantRepository.save(participant);
          log.info(
              "合同创建时自动添加参与人: contractId={}, userId={}, role={}",
              contract.getId(),
              participantUserId,
              role);
        }
      } catch (Exception e) {
        // 参与人添加失败不影响合同创建，只记录日志
        log.warn(
            "合同创建时自动添加参与人失败: contractId={}, userId={}, error={}",
            contract.getId(),
            participantUserId,
            e.getMessage());
      }
    }

    log.info("合同创建成功: {} ({})", contract.getName(), contract.getContractNo());
    return toDTO(contract);
  }

  /**
   * 更新合同 只有未审批通过的合同（DRAFT、REJECTED）可以直接修改 已审批通过的合同（ACTIVE）需要通过变更审批流程
   *
   * @param command 更新合同命令对象
   * @return 更新后的合同DTO
   * @throws BusinessException 当合同不存在或状态不符合修改条件时抛出异常
   */
  @Transactional
  public ContractDTO updateContract(final UpdateContractCommand command) {
    Contract contract = contractRepository.getByIdOrThrow(command.getId(), "合同不存在");

    // 验证用户是否是合同创建者、签约人或参与人（只有所有者才能编辑）
    validateContractOwnership(contract);

    // 只有草稿和被拒绝状态可以修改，待审批状态不允许修改（需要先取消审批）
    if (!ContractStatus.DRAFT.equals(contract.getStatus())
        && !ContractStatus.REJECTED.equals(contract.getStatus())) {
      throw new BusinessException("只有草稿状态或被拒绝状态的合同可以直接修改。已审批通过的合同如需修改，请使用变更申请功能");
    }

    // 验证风险代理比例范围
    if (command.getRiskRatio() != null) {
      if (command.getRiskRatio().compareTo(BigDecimal.ZERO) < 0
          || command.getRiskRatio().compareTo(new BigDecimal("100")) > 0) {
        throw new BusinessException("风险代理比例必须在0-100之间");
      }
    }

    // 更新字段
    if (StringUtils.hasText(command.getName())) {
      contract.setName(command.getName());
    }
    if (StringUtils.hasText(command.getContractType())) {
      contract.setContractType(command.getContractType());
    }
    if (command.getClientId() != null) {
      clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
      contract.setClientId(command.getClientId());
    }
    if (command.getMatterId() != null) {
      matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
      contract.setMatterId(command.getMatterId());
    }
    if (StringUtils.hasText(command.getFeeType())) {
      contract.setFeeType(command.getFeeType());
    }
    if (command.getTotalAmount() != null) {
      contract.setTotalAmount(command.getTotalAmount());
    }
    if (command.getCurrency() != null) {
      contract.setCurrency(command.getCurrency());
    }
    if (command.getSignDate() != null) {
      contract.setSignDate(command.getSignDate());
    }
    if (command.getEffectiveDate() != null) {
      contract.setEffectiveDate(command.getEffectiveDate());
    }
    if (command.getExpiryDate() != null) {
      contract.setExpiryDate(command.getExpiryDate());
    }
    if (command.getSignerId() != null) {
      contract.setSignerId(command.getSignerId());
    }
    if (command.getDepartmentId() != null) {
      contract.setDepartmentId(command.getDepartmentId());
    }
    if (command.getPaymentTerms() != null) {
      contract.setPaymentTerms(command.getPaymentTerms());
    }
    if (command.getFileUrl() != null) {
      setFileStorageInfo(contract, command.getFileUrl());
    }
    if (command.getRemark() != null) {
      contract.setRemark(command.getRemark());
    }
    // 扩展字段
    if (command.getCaseType() != null) {
      contract.setCaseType(command.getCaseType());
    }
    if (command.getCauseOfAction() != null) {
      contract.setCauseOfAction(command.getCauseOfAction());
    }
    if (command.getTrialStage() != null) {
      contract.setTrialStage(command.getTrialStage());
    }
    if (command.getClaimAmount() != null) {
      contract.setClaimAmount(command.getClaimAmount());
    }
    if (command.getJurisdictionCourt() != null) {
      contract.setJurisdictionCourt(command.getJurisdictionCourt());
    }
    if (command.getOpposingParty() != null) {
      contract.setOpposingParty(command.getOpposingParty());
    }
    if (command.getConflictCheckStatus() != null) {
      contract.setConflictCheckStatus(command.getConflictCheckStatus());
    }
    if (command.getArchiveStatus() != null) {
      contract.setArchiveStatus(command.getArchiveStatus());
    }
    if (command.getAdvanceTravelFee() != null) {
      contract.setAdvanceTravelFee(command.getAdvanceTravelFee());
    }
    if (command.getRiskRatio() != null) {
      contract.setRiskRatio(command.getRiskRatio());
    }
    if (command.getSealRecord() != null) {
      contract.setSealRecord(command.getSealRecord());
    }

    // 提成分配方案字段
    if (command.getCommissionRuleId() != null) {
      contract.setCommissionRuleId(command.getCommissionRuleId());
    }
    if (command.getFirmRate() != null) {
      contract.setFirmRate(command.getFirmRate());
    }
    if (command.getLeadLawyerRate() != null) {
      contract.setLeadLawyerRate(command.getLeadLawyerRate());
    }
    if (command.getAssistLawyerRate() != null) {
      contract.setAssistLawyerRate(command.getAssistLawyerRate());
    }
    if (command.getSupportStaffRate() != null) {
      contract.setSupportStaffRate(command.getSupportStaffRate());
    }
    if (command.getOriginatorRate() != null) {
      contract.setOriginatorRate(command.getOriginatorRate());
    }

    contractRepository.updateById(contract);
    log.info("合同更新成功: {}", contract.getName());
    return toDTO(contract);
  }

  /**
   * 删除合同 只有草稿状态和已拒绝状态的合同可以删除 已审批通过的合同（ACTIVE）不能删除（需要终止或完成）
   *
   * <p>注意：已拒绝状态的合同如果已生成编号，删除后编号不会释放（编号作废不再使用） 只有真正生效的合同（ACTIVE状态）才占用编号位置
   *
   * @param id 合同ID
   * @throws BusinessException 当合同不存在或状态不符合删除条件时抛出异常
   */
  @Transactional
  public void deleteContract(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    // 验证用户是否是合同创建者、签约人或参与人（只有所有者才能删除）
    validateContractOwnership(contract);

    // 只有草稿状态和已拒绝状态可以删除（已审批通过的合同不能删除）
    if (!ContractStatus.DRAFT.equals(contract.getStatus())
        && !ContractStatus.REJECTED.equals(contract.getStatus())) {
      throw new BusinessException("只有草稿状态或已拒绝状态的合同可以删除，已审批通过的合同不能删除");
    }

    // 如果已拒绝状态的合同有编号，记录日志（编号不会释放，作废不再使用）
    if (ContractStatus.REJECTED.equals(contract.getStatus()) && contract.getContractNo() != null) {
      log.info(
          "删除已拒绝状态的合同，编号作废: contractNo={}, contractId={}",
          contract.getContractNo(),
          contract.getId());
    }

    contractMapper.deleteById(id);
    log.info("合同删除成功: {}, 状态: {}", contract.getName(), contract.getStatus());
  }

  /**
   * 获取合同详情
   *
   * @param id 合同ID
   * @return 合同DTO
   */
  public ContractDTO getContractById(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
    ContractDTO dto = toDTO(contract);

    // 查询关联的审批单
    List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals("CONTRACT", id);
    dto.setApprovals(approvals);

    // 设置当前待审批的审批单
    ApprovalDTO currentApproval =
        approvals.stream()
            .filter(a -> ApprovalStatus.PENDING.equals(a.getStatus()))
            .findFirst()
            .orElse(null);
    dto.setCurrentApproval(currentApproval);

    return dto;
  }

  /**
   * 提交审批（支持手动选择审批人）
   *
   * @param id 合同ID
   * @param approverId 审批人ID（可选，为空时自动查找）
   */
  @Transactional
  public void submitForApproval(final Long id, final Long approverId) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    // 允许草稿状态和被拒绝状态的合同提交审批
    if (!ContractStatus.canSubmit(contract.getStatus())) {
      throw new BusinessException("只有草稿状态或被拒绝状态的合同可以提交审批");
    }

    // 注意：合同编号在审批通过时生成，而不是提交审批时
    // 这样可以避免审批不通过的合同占用编号

    contract.setStatus(ContractStatus.PENDING);
    contractRepository.updateById(contract);

    // 创建审批记录
    try {
      // 使用局部变量存储审批人ID，因为参数是final的
      Long finalApproverId = approverId;

      // 如果没有指定审批人，自动查找
      if (finalApproverId == null) {
        finalApproverId = approverService.findContractApprover(contract.getTotalAmount());
        if (finalApproverId == null) {
          log.warn("未找到合同审批人，使用默认审批人");
          finalApproverId = approverService.findDefaultApprover();
        }
      }

      if (finalApproverId == null) {
        throw new BusinessException("无法找到审批人，请选择审批人或配置系统审批人");
      }

      // 验证审批人存在
      User approver = userRepository.getById(finalApproverId);
      if (approver == null) {
        throw new BusinessException("选择的审批人不存在");
      }

      String priority =
          contract.getTotalAmount() != null
                  && contract.getTotalAmount().compareTo(new BigDecimal("100000")) >= 0
              ? "HIGH"
              : "MEDIUM";

      // 注意：此时合同编号为空（编号在审批通过时生成），使用合同ID作为业务编号
      String businessNo =
          contract.getContractNo() != null
              ? contract.getContractNo()
              : "DRAFT-" + contract.getId();
      approvalService.createApproval(
          "CONTRACT",
          contract.getId(),
          businessNo,
          contract.getName(),
          finalApproverId,
          priority,
          "NORMAL",
          null // businessSnapshot
          );

      log.info("合同提交审批成功: {} (审批人: {})", contract.getName(), approver.getRealName());
    } catch (BusinessException e) {
      // 回滚合同状态
      contract.setStatus(ContractStatus.DRAFT);
      contractRepository.updateById(contract);
      log.error("合同提交审批失败: {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      // 回滚合同状态
      contract.setStatus(ContractStatus.DRAFT);
      contractRepository.updateById(contract);
      log.error("合同提交审批异常: {}", e.getMessage(), e);
      throw new BusinessException("提交审批失败: " + e.getMessage());
    }
  }

  /**
   * 获取可选审批人列表（当前用户架构垂直线上的领导） 包括：当前部门负责人、上级部门负责人、主任、团队负责人
   *
   * @return 审批人列表，每个审批人包含id、realName、departmentName、position等信息
   */
  public List<Map<String, Object>> getAvailableApprovers() {
    Long currentUserId = SecurityUtils.getUserId();
    Long currentDeptId = SecurityUtils.getDepartmentId();

    List<Map<String, Object>> approvers = new ArrayList<>();
    Set<Long> addedUserIds = new HashSet<>();

    // 1. 沿部门层级向上查找所有部门负责人
    Long deptId = currentDeptId;
    while (deptId != null) {
      Department dept = departmentRepository.getById(deptId);
      if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(currentUserId)) {
        if (!addedUserIds.contains(dept.getLeaderId())) {
          User leader = userRepository.getById(dept.getLeaderId());
          if (leader != null && "ACTIVE".equals(leader.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", leader.getId());
            approver.put("realName", leader.getRealName());
            approver.put("departmentName", dept.getName());
            approver.put("position", "部门负责人");
            approvers.add(approver);
            addedUserIds.add(leader.getId());
          }
        }
        deptId = dept.getParentId();
      } else if (dept != null) {
        deptId = dept.getParentId();
      } else {
        deptId = null;
      }
    }

    // 2. 添加所有团队负责人（TEAM_LEADER角色）
    List<Long> partnerIds = userMapper.selectUserIdsByRoleCode("TEAM_LEADER");
    if (partnerIds != null) {
      for (Long partnerId : partnerIds) {
        if (!partnerId.equals(currentUserId) && !addedUserIds.contains(partnerId)) {
          User partner = userRepository.getById(partnerId);
          if (partner != null && "ACTIVE".equals(partner.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", partner.getId());
            approver.put("realName", partner.getRealName());
            approver.put("departmentName", getDepartmentName(partner.getDepartmentId()));
            approver.put("position", "团队负责人");
            approvers.add(approver);
            addedUserIds.add(partner.getId());
          }
        }
      }
    }

    // 3. 添加主任（DIRECTOR角色）
    List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
    if (directorIds != null) {
      for (Long directorId : directorIds) {
        if (!directorId.equals(currentUserId) && !addedUserIds.contains(directorId)) {
          User director = userRepository.getById(directorId);
          if (director != null && "ACTIVE".equals(director.getStatus())) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("id", director.getId());
            approver.put("realName", director.getRealName());
            approver.put("departmentName", getDepartmentName(director.getDepartmentId()));
            approver.put("position", "主任");
            approvers.add(approver);
            addedUserIds.add(director.getId());
          }
        }
      }
    }

    return approvers;
  }

  /**
   * 获取部门名称.
   *
   * @param deptId 部门ID
   * @return 部门名称，如果部门不存在或ID为null则返回空字符串
   */
  private String getDepartmentName(final Long deptId) {
    if (deptId == null) {
      return "";
    }
    Department dept = departmentRepository.getById(deptId);
    return dept != null ? dept.getName() : "";
  }

  /**
   * 审批通过
   *
   * <p>Requirements: 1.1, 1.2 - 合同审批通过后发布事件，触发数据同步 如果是变更审批，发布 ContractAmendedEvent 通知财务模块
   *
   * <p>问题248修复：事件发布失败时抛出异常触发事务回滚 问题250修复：改进审批类型判断逻辑
   *
   * @param id 合同ID
   */
  @Transactional
  public void approve(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    if (!ContractStatus.PENDING.equals(contract.getStatus())) {
      throw new BusinessException("只有待审批状态可以审批");
    }

    // 检查是否是变更审批（通过审批记录的子类型判断）
    boolean isAmendment = isAmendmentApproval(contract.getId());

    // 如果是变更审批，保存变更前的数据快照
    String beforeSnapshot = null;
    if (isAmendment) {
      beforeSnapshot = buildContractSnapshot(contract);
    }

    // 审批通过时生成合同编号（如果还没有编号）
    // 这样只有审批通过的合同才会真正占用编号，被拒绝的不会占用
    if (!isAmendment
        && (contract.getContractNo() == null || contract.getContractNo().isBlank())) {
      String contractNo =
          contractNumberGenerator.generate(
              contract.getCaseType(), contract.getFeeType(), contract.getContractType());
      contract.setContractNo(contractNo);
      log.info(
          "审批通过时生成合同编号: contractId={}, contractNo={}",
          contract.getId(),
          contract.getContractNo());
    }

    contract.setStatus(ContractStatus.ACTIVE);
    if (contract.getEffectiveDate() == null) {
      contract.setEffectiveDate(LocalDate.now());
    }
    contractRepository.updateById(contract);

    // 发布事件（失败时抛出异常触发事务回滚）
    try {
      if (isAmendment) {
        // 变更审批通过，发布变更事件通知财务模块
        String afterSnapshot = buildContractSnapshot(contract);
        eventPublisher.publishEvent(
            ContractAmendedEvent.builder()
                .contractId(contract.getId())
                .amendmentType(detectAmendmentType(beforeSnapshot, afterSnapshot))
                .amendedBy(SecurityUtils.getUserId())
                .amendedAt(java.time.LocalDateTime.now())
                .amendmentReason("合同变更审批通过")
                .beforeSnapshot(beforeSnapshot)
                .afterSnapshot(afterSnapshot)
                .build());
        log.info("合同变更审批通过: {}", contract.getName());
      } else {
        // 新建审批通过，发布审批通过事件
        eventPublisher.publishEvent(
            new ContractApprovedEvent(this, contract.getId(), SecurityUtils.getUserId()));
        log.info("合同审批通过: {}", contract.getName());
      }
    } catch (Exception e) {
      log.error("合同审批事件发布失败，触发事务回滚: contractId={}", id, e);
      throw new BusinessException("合同审批事件发布失败: " + e.getMessage());
    }
  }

  /**
   * 判断是否是变更审批
   *
   * <p>问题250修复：使用多个关键词匹配标题，更健壮 注：未来可添加 approvalSubType 字段到 ApprovalDTO 进一步改进
   *
   * @param contractId 合同ID
   * @return 是否是变更审批
   */
  private boolean isAmendmentApproval(final Long contractId) {
    List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals("CONTRACT", contractId);
    if (approvals == null || approvals.isEmpty()) {
      return false;
    }
    // 查找待审批的记录，检查标题是否包含变更相关关键词
    return approvals.stream()
        .filter(a -> ApprovalStatus.PENDING.equals(a.getStatus()))
        .anyMatch(
            a -> {
              if (a.getBusinessTitle() != null) {
                String title = a.getBusinessTitle();
                // 匹配多个变更相关关键词，比原来只匹配"变更申请"更健壮
                return title.contains("变更") || title.contains("修改") || title.contains("调整");
              }
              return false;
            });
  }

  /**
   * 构建合同数据快照
   *
   * @param contract 合同实体
   * @return 合同快照JSON字符串
   */
  private String buildContractSnapshot(final Contract contract) {
    try {
      Map<String, Object> snapshot = new HashMap<>();
      snapshot.put("totalAmount", contract.getTotalAmount());
      snapshot.put("name", contract.getName());
      snapshot.put("feeType", contract.getFeeType());
      snapshot.put("opposingParty", contract.getOpposingParty());
      snapshot.put("caseType", contract.getCaseType());
      snapshot.put("causeOfAction", contract.getCauseOfAction());

      // 获取参与人信息
      List<ContractParticipant> participants =
          participantRepository.findByContractId(contract.getId());
      if (participants != null) {
        List<Map<String, Object>> participantList =
            participants.stream()
                .map(
                    p -> {
                      Map<String, Object> pMap = new HashMap<>();
                      pMap.put("userId", p.getUserId());
                      pMap.put("role", p.getRole());
                      pMap.put("commissionRate", p.getCommissionRate());
                      return pMap;
                    })
                .collect(Collectors.toList());
        snapshot.put("participants", participantList);
      }

      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(snapshot);
    } catch (Exception e) {
      log.warn("构建合同快照失败", e);
      return "{}";
    }
  }

  /**
   * 检测变更类型
   *
   * <p>问题249优化：扩展变更类型检测，支持更多业务场景 返回最重要的变更类型（按优先级：AMOUNT > TERM > PARTICIPANT > OTHER）
   *
   * @param beforeSnapshot 变更前快照
   * @param afterSnapshot 变更后快照
   * @return 变更类型
   */
  private String detectAmendmentType(final String beforeSnapshot, final String afterSnapshot) {
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      @SuppressWarnings("unchecked")
      Map<String, Object> before = mapper.readValue(beforeSnapshot, Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> after = mapper.readValue(afterSnapshot, Map.class);

      // 1. 检查金额变更（最高优先级）
      if (!java.util.Objects.equals(before.get("totalAmount"), after.get("totalAmount"))) {
        return "AMOUNT";
      }

      // 2. 检查期限变更（到期日、生效日）
      if (!java.util.Objects.equals(before.get("expiryDate"), after.get("expiryDate"))
          || !java.util.Objects.equals(before.get("effectiveDate"), after.get("effectiveDate"))) {
        return "TERM";
      }

      // 3. 检查参与人变更
      if (!java.util.Objects.equals(before.get("participants"), after.get("participants"))) {
        return "PARTICIPANT";
      }

      // 4. 检查其他关键字段变更
      if (!java.util.Objects.equals(before.get("paymentTerms"), after.get("paymentTerms"))
          || !java.util.Objects.equals(before.get("feeType"), after.get("feeType"))) {
        return "PAYMENT";
      }

      return "OTHER";
    } catch (Exception e) {
      log.warn("解析合同快照失败", e);
      return "OTHER";
    }
  }

  /**
   * 审批拒绝
   *
   * @param id 合同ID
   * @param reason 拒绝原因
   */
  @Transactional
  public void reject(final Long id, final String reason) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    if (!ContractStatus.PENDING.equals(contract.getStatus())) {
      throw new BusinessException("只有待审批状态可以拒绝");
    }

    contract.setStatus(ContractStatus.REJECTED);
    contract.setRemark(reason);
    contractRepository.updateById(contract);
    log.info("合同审批拒绝: {}", contract.getName());
  }

  /**
   * 撤回审批
   *
   * <p>只有合同创建者或签约人才能撤回，且只能撤回待审批状态的合同
   *
   * @param id 合同ID
   */
  @Transactional
  public void withdrawApproval(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    // 只有待审批状态可以撤回
    if (!ContractStatus.PENDING.equals(contract.getStatus())) {
      throw new BusinessException("只有待审批状态的合同可以撤回");
    }

    // 验证用户是否是合同创建者或签约人
    Long currentUserId = SecurityUtils.getUserId();
    if (!currentUserId.equals(contract.getCreatedBy())
        && !currentUserId.equals(contract.getSignerId())) {
      throw new BusinessException("只有合同创建者或签约人才能撤回审批");
    }

    // 查找并取消关联的审批单
    try {
      LambdaQueryWrapper<Approval> wrapper = new LambdaQueryWrapper<>();
      wrapper
          .eq(Approval::getBusinessType, "CONTRACT")
          .eq(Approval::getBusinessId, id)
          .eq(Approval::getStatus, ApprovalStatus.PENDING);
      List<Approval> approvals = approvalMapper.selectList(wrapper);

      for (Approval approval : approvals) {
        approval.setStatus(ApprovalStatus.WITHDRAWN);
        approvalMapper.updateById(approval);
        log.info("审批单已撤回: approvalId={}", approval.getId());
      }
    } catch (Exception e) {
      log.warn("取消审批单失败: {}", e.getMessage());
      // 即使取消审批单失败，也继续撤回合同状态
    }

    // 将合同状态改回草稿
    contract.setStatus(ContractStatus.DRAFT);
    contractRepository.updateById(contract);
    log.info("合同审批已撤回: {}, 操作人: {}", contract.getName(), currentUserId);
  }

  /**
   * 终止合同
   *
   * @param id 合同ID
   * @param reason 终止原因
   */
  @Transactional
  public void terminate(final Long id, final String reason) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    if (!ContractStatus.canTerminate(contract.getStatus())) {
      throw new BusinessException("只有生效中的合同可以终止");
    }

    contract.setStatus(ContractStatus.TERMINATED);
    contract.setRemark(reason);
    contractRepository.updateById(contract);
    log.info("合同已终止: {}", contract.getName());
  }

  /**
   * 完成合同
   *
   * @param id 合同ID
   */
  @Transactional
  public void complete(final Long id) {
    Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");

    if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
      throw new BusinessException("只有生效中的合同可以完成");
    }

    contract.setStatus(ContractStatus.COMPLETED);
    contractRepository.updateById(contract);
    log.info("合同已完成: {}", contract.getName());
  }

  /**
   * 获取已审批的合同列表（用于创建项目时选择）
   *
   * <p>包含参与人信息，便于创建项目时自动填充 注意：一个合同可以创建多个项目，所以不再检查 matterId
   *
   * @return 已审批的合同列表
   */
  public List<ContractDTO> getApprovedContracts() {
    LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Contract::getStatus, ContractStatus.ACTIVE);
    // 一个合同可以创建多个项目（如常年法顾合同下有多个具体项目）
    // 所以不再过滤 matterId 为空的合同
    wrapper.orderByDesc(Contract::getCreatedAt);

    List<Contract> contracts = contractRepository.list(wrapper);
    return contracts.stream()
        .map(
            contract -> {
              ContractDTO dto = toDTO(contract);
              // 加载参与人信息
              List<ContractParticipant> participants =
                  participantRepository.findByContractId(contract.getId());
              if (participants != null && !participants.isEmpty()) {
                dto.setParticipants(
                    participants.stream().map(this::toParticipantDTO).collect(Collectors.toList()));
              }
              return dto;
            })
        .collect(Collectors.toList());
  }

  /**
   * 申请合同变更
   *
   * <p>用于已审批通过的合同申请变更，需要重新审批
   *
   * @param command 合同变更命令对象
   */
  @Transactional
  public void applyContractChange(final ContractChangeCommand command) {
    Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");

    // 验证用户是否是合同创建者、签约人或参与人（只有所有者才能申请变更）
    validateContractOwnership(contract);

    // 只有已审批通过的合同可以申请变更
    if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
      throw new BusinessException("只有已审批通过的合同可以申请变更");
    }

    // 构建变更内容说明（记录所有变更的字段）
    StringBuilder changeDesc = new StringBuilder();
    changeDesc.append("变更原因：").append(command.getChangeReason()).append("\n");
    changeDesc.append("变更内容：\n");

    if (command.getName() != null && !command.getName().equals(contract.getName())) {
      changeDesc
          .append("- 合同名称：")
          .append(contract.getName())
          .append(" → ")
          .append(command.getName())
          .append("\n");
    }
    if (command.getContractType() != null
        && !command.getContractType().equals(contract.getContractType())) {
      changeDesc
          .append("- 合同类型：")
          .append(contract.getContractType())
          .append(" → ")
          .append(command.getContractType())
          .append("\n");
    }
    if (command.getClientId() != null && !command.getClientId().equals(contract.getClientId())) {
      changeDesc.append("- 客户变更\n");
    }
    if (command.getFeeType() != null && !command.getFeeType().equals(contract.getFeeType())) {
      changeDesc
          .append("- 收费方式：")
          .append(contract.getFeeType())
          .append(" → ")
          .append(command.getFeeType())
          .append("\n");
    }
    if (command.getTotalAmount() != null
        && command.getTotalAmount().compareTo(contract.getTotalAmount()) != 0) {
      changeDesc
          .append("- 合同金额：")
          .append(contract.getTotalAmount())
          .append(" → ")
          .append(command.getTotalAmount())
          .append("\n");
    }
    if (command.getSignDate() != null && !command.getSignDate().equals(contract.getSignDate())) {
      changeDesc.append("- 签约日期变更\n");
    }
    if (command.getEffectiveDate() != null
        && !command.getEffectiveDate().equals(contract.getEffectiveDate())) {
      changeDesc.append("- 生效日期变更\n");
    }
    if (command.getExpiryDate() != null
        && !command.getExpiryDate().equals(contract.getExpiryDate())) {
      changeDesc.append("- 到期日期变更\n");
    }
    if (command.getPaymentTerms() != null
        && !command.getPaymentTerms().equals(contract.getPaymentTerms())) {
      changeDesc.append("- 付款条款变更\n");
    }

    if (command.getChangeDescription() != null
        && !command.getChangeDescription().trim().isEmpty()) {
      changeDesc.append("\n详细说明：").append(command.getChangeDescription());
    }

    // 将变更内容保存到备注中（作为变更记录）
    String changeRecord =
        String.format(
            "[变更申请] %s\n%s",
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            changeDesc.toString());
    String newRemark =
        contract.getRemark() != null ? contract.getRemark() + "\n\n" + changeRecord : changeRecord;

    // 更新合同为待审批状态，并保存变更内容到备注
    contract.setStatus(ContractStatus.PENDING);
    contract.setRemark(newRemark);

    // 应用变更内容（但状态为待审批，需要审批通过后才生效）
    if (command.getName() != null) {
      contract.setName(command.getName());
    }
    if (command.getContractType() != null) {
      contract.setContractType(command.getContractType());
    }
    if (command.getClientId() != null) {
      clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
      contract.setClientId(command.getClientId());
    }
    if (command.getMatterId() != null) {
      matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
      contract.setMatterId(command.getMatterId());
    }
    if (command.getFeeType() != null) {
      contract.setFeeType(command.getFeeType());
    }
    if (command.getTotalAmount() != null) {
      contract.setTotalAmount(command.getTotalAmount());
    }
    if (command.getCurrency() != null) {
      contract.setCurrency(command.getCurrency());
    }
    if (command.getSignDate() != null) {
      contract.setSignDate(command.getSignDate());
    }
    if (command.getEffectiveDate() != null) {
      contract.setEffectiveDate(command.getEffectiveDate());
    }
    if (command.getExpiryDate() != null) {
      contract.setExpiryDate(command.getExpiryDate());
    }
    if (command.getSignerId() != null) {
      contract.setSignerId(command.getSignerId());
    }
    if (command.getDepartmentId() != null) {
      contract.setDepartmentId(command.getDepartmentId());
    }
    if (command.getPaymentTerms() != null) {
      contract.setPaymentTerms(command.getPaymentTerms());
    }
    if (command.getFileUrl() != null) {
      setFileStorageInfo(contract, command.getFileUrl());
    }

    contractRepository.updateById(contract);

    // 创建变更审批记录
    try {
      Long approverId = approverService.findContractApprover(contract.getTotalAmount());
      if (approverId == null) {
        log.warn("未找到合同审批人，使用默认审批人");
        approverId = approverService.findDefaultApprover();
      }

      if (approverId == null) {
        throw new BusinessException("无法找到审批人，请先配置系统审批人");
      }

      String priority =
          contract.getTotalAmount() != null
                  && contract.getTotalAmount().compareTo(new BigDecimal("100000")) >= 0
              ? "HIGH"
              : "MEDIUM";

      // 创建变更审批，业务标题包含"变更"标识
      approvalService.createApproval(
          "CONTRACT",
          contract.getId(),
          contract.getContractNo(),
          contract.getName() + " [变更申请]",
          approverId,
          priority,
          "NORMAL",
          changeDesc.toString() // 将变更内容作为业务快照
          );

      log.info("合同变更申请提交成功: {} (审批人: {})", contract.getName(), approverId);
    } catch (BusinessException e) {
      // 回滚合同状态
      contract.setStatus(ContractStatus.ACTIVE);
      contractRepository.updateById(contract);
      log.error("合同变更申请提交失败: {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      // 回滚合同状态
      contract.setStatus(ContractStatus.ACTIVE);
      contractRepository.updateById(contract);
      log.error("合同变更申请提交异常: {}", e.getMessage(), e);
      throw new BusinessException("提交变更申请失败: " + e.getMessage());
    }
  }

  /**
   * 获取合同类型名称（模板类型名称）
   *
   * @param type 合同类型代码
   * @return 合同类型名称
   */
  public String getContractTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "CIVIL_PROXY" -> "民事代理";
      case "ADMINISTRATIVE_PROXY" -> "行政代理";
      case "CRIMINAL_DEFENSE" -> "刑事辩护";
      case "LEGAL_COUNSEL" -> "法律顾问";
      case "NON_LITIGATION" -> "非诉案件";
      case "CUSTOM" -> "自定义模板";
      default -> type;
    };
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  public String getStatusName(final String status) {
    return ContractStatus.getStatusName(status);
  }

  /**
   * 应用数据范围过滤
   *
   * <p>ALL: 可看全部 DEPT_AND_CHILD: 可看本部门及下级部门 DEPT: 可看本部门 SELF: 只能看自己的合同
   *
   * @param wrapper 查询包装器
   */
  private void applyDataScopeFilter(final LambdaQueryWrapper<Contract> wrapper) {
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getCurrentUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    switch (dataScope) {
      case "ALL":
        // 可看全部，不加过滤条件
        break;
      case "DEPT_AND_CHILD":
        // 可看本部门及下级部门
        if (deptId != null) {
          List<Long> deptIds = getAllChildDepartmentIds(deptId);
          deptIds.add(deptId);
          wrapper.and(
              w ->
                  w.in(Contract::getDepartmentId, deptIds)
                      .or()
                      .eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId));
        } else {
          // 没有部门，只能看自己的
          wrapper.and(
              w ->
                  w.eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId));
        }
        break;
      case "DEPT":
        // 仅本部门
        if (deptId != null) {
          wrapper.and(
              w ->
                  w.eq(Contract::getDepartmentId, deptId)
                      .or()
                      .eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId));
        } else {
          wrapper.and(
              w ->
                  w.eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId));
        }
        break;
      default: // SELF
        // 只能看自己签约或创建的合同，以及自己作为参与人的合同
        List<Long> participatingContractIds = getParticipatingContractIds(currentUserId);
        if (participatingContractIds.isEmpty()) {
          wrapper.and(
              w ->
                  w.eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId));
        } else {
          wrapper.and(
              w ->
                  w.eq(Contract::getSignerId, currentUserId)
                      .or()
                      .eq(Contract::getCreatedBy, currentUserId)
                      .or()
                      .in(Contract::getId, participatingContractIds));
        }
        break;
    }
  }

  /**
   * 获取所有下级部门ID（带深度限制和循环检测）
   *
   * @param parentId 父部门ID
   * @return 所有下级部门ID列表
   */
  private List<Long> getAllChildDepartmentIds(final Long parentId) {
    List<Long> result = new ArrayList<>();
    Set<Long> visited = new HashSet<>(); // 防止循环引用
    getAllChildDepartmentIdsRecursive(parentId, result, visited, 0, 10); // 最大10层
    return result;
  }

  /**
   * 递归获取子部门ID（内部方法）
   *
   * @param parentId 父部门ID
   * @param result 结果列表
   * @param visited 已访问部门集合（防止循环引用）
   * @param depth 当前递归深度
   * @param maxDepth 最大递归深度
   */
  private void getAllChildDepartmentIdsRecursive(
      final Long parentId,
      final List<Long> result,
      final Set<Long> visited,
      final int depth,
      final int maxDepth) {
    if (depth >= maxDepth) {
      log.warn("部门层级超过最大深度{}，停止递归，parentId: {}", maxDepth, parentId);
      return;
    }

    if (visited.contains(parentId)) {
      log.warn("检测到部门循环引用，跳过: parentId={}", parentId);
      return;
    }
    visited.add(parentId);

    List<Department> children = departmentRepository.findByParentId(parentId);
    for (Department child : children) {
      if (!visited.contains(child.getId())) {
        result.add(child.getId());
        getAllChildDepartmentIdsRecursive(child.getId(), result, visited, depth + 1, maxDepth);
      }
    }
  }

  /**
   * 获取用户作为参与人的合同ID列表
   *
   * @param userId 用户ID
   * @return 参与人的合同ID列表
   */
  private List<Long> getParticipatingContractIds(final Long userId) {
    return participantRepository.findContractIdsByUserId(userId);
  }

  /**
   * 验证用户是否是合同的创建者、签约人或参与人（用于编辑/删除/变更操作） 只有合同的创建者、签约人或参与人才能编辑合同，管理员除外
   *
   * @param contract 合同
   * @throws BusinessException 如果用户不是合同所有者
   */
  private void validateContractOwnership(final Contract contract) {
    // 管理员可以操作所有合同
    if (SecurityUtils.isAdmin()) {
      return;
    }

    Long currentUserId = SecurityUtils.getCurrentUserId();

    // 检查是否是合同创建者
    if (currentUserId.equals(contract.getCreatedBy())) {
      return;
    }

    // 检查是否是合同签约人
    if (currentUserId.equals(contract.getSignerId())) {
      return;
    }

    // 检查是否是合同参与人
    List<Long> participatingContractIds = getParticipatingContractIds(currentUserId);
    if (participatingContractIds.contains(contract.getId())) {
      return;
    }

    throw new BusinessException("只有合同的创建者、签约人或参与人才能执行此操作");
  }

  /**
   * Entity 转 DTO
   *
   * @param contract 合同实体
   * @return 合同DTO
   */
  private ContractDTO toDTO(final Contract contract) {
    ContractDTO dto = new ContractDTO();
    dto.setId(contract.getId());
    dto.setContractNo(contract.getContractNo());
    dto.setName(contract.getName());
    dto.setTemplateId(contract.getTemplateId());
    dto.setContent(contract.getContent());
    dto.setContractType(contract.getContractType());
    dto.setContractTypeName(getContractTypeName(contract.getContractType()));
    dto.setClientId(contract.getClientId());
    // 填充客户名称
    if (contract.getClientId() != null) {
      try {
        var client = clientRepository.getById(contract.getClientId());
        if (client != null) {
          dto.setClientName(client.getName());
        }
      } catch (Exception e) {
        log.warn("获取客户名称失败，clientId: {}", contract.getClientId(), e);
      }
    }
    dto.setMatterId(contract.getMatterId());
    // 填充项目名称
    if (contract.getMatterId() != null) {
      try {
        var matter = matterRepository.getById(contract.getMatterId());
        if (matter != null) {
          dto.setMatterName(matter.getName());
        }
      } catch (Exception e) {
        log.warn("获取项目名称失败，matterId: {}", contract.getMatterId(), e);
      }
    }
    dto.setFeeType(contract.getFeeType());
    dto.setFeeTypeName(contractTemplateVariableService.getFeeTypeName(contract.getFeeType()));
    dto.setTotalAmount(contract.getTotalAmount());
    dto.setPaidAmount(contract.getPaidAmount());
    dto.setUnpaidAmount(
        contract
            .getTotalAmount()
            .subtract(
                contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO));
    dto.setCurrency(contract.getCurrency());
    dto.setSignDate(contract.getSignDate());
    dto.setEffectiveDate(contract.getEffectiveDate());
    dto.setExpiryDate(contract.getExpiryDate());
    dto.setStatus(contract.getStatus());
    dto.setStatusName(getStatusName(contract.getStatus()));
    dto.setSignerId(contract.getSignerId());
    dto.setDepartmentId(contract.getDepartmentId());
    dto.setCreatedBy(contract.getCreatedBy()); // 创建人ID
    dto.setPaymentTerms(contract.getPaymentTerms());
    dto.setFileUrl(contract.getFileUrl());
    dto.setRemark(contract.getRemark());
    dto.setCreatedAt(contract.getCreatedAt());
    dto.setUpdatedAt(contract.getUpdatedAt());
    // 扩展字段
    dto.setCaseType(contract.getCaseType());
    dto.setCaseTypeName(MatterConstants.getCaseTypeName(contract.getCaseType()));
    dto.setCauseOfAction(contract.getCauseOfAction());
    // 设置案由名称（用于前端显示，特别是审批表）
    dto.setCauseOfActionName(
        getCauseOfActionName(contract.getCauseOfAction(), contract.getCaseType()));
    dto.setTrialStage(contract.getTrialStage());
    dto.setTrialStageName(getTrialStageName(contract.getTrialStage()));
    dto.setClaimAmount(contract.getClaimAmount());
    dto.setJurisdictionCourt(contract.getJurisdictionCourt());
    dto.setOpposingParty(contract.getOpposingParty());
    dto.setConflictCheckStatus(contract.getConflictCheckStatus());
    dto.setConflictCheckStatusName(getConflictCheckStatusName(contract.getConflictCheckStatus()));
    dto.setArchiveStatus(contract.getArchiveStatus());
    dto.setArchiveStatusName(getArchiveStatusName(contract.getArchiveStatus()));
    dto.setAdvanceTravelFee(contract.getAdvanceTravelFee());
    dto.setRiskRatio(contract.getRiskRatio());
    dto.setSealRecord(contract.getSealRecord());
    // 提成分配方案
    dto.setCommissionRuleId(contract.getCommissionRuleId());
    dto.setFirmRate(contract.getFirmRate());
    dto.setLeadLawyerRate(contract.getLeadLawyerRate());
    dto.setAssistLawyerRate(contract.getAssistLawyerRate());
    dto.setSupportStaffRate(contract.getSupportStaffRate());
    dto.setOriginatorRate(contract.getOriginatorRate());
    dto.setCaseSummary(contract.getCaseSummary());
    return dto;
  }

  /**
   * Entity 转 DTO（使用预加载的关联数据，避免N+1查询）
   *
   * @param contract 合同实体
   * @param clientMap 预加载的客户Map
   * @param matterMap 预加载的项目Map
   * @return 合同DTO
   */
  private ContractDTO toDTO(
      final Contract contract,
      final Map<Long, Client> clientMap,
      final Map<Long, com.lawfirm.domain.matter.entity.Matter> matterMap) {
    ContractDTO dto = new ContractDTO();
    dto.setId(contract.getId());
    dto.setContractNo(contract.getContractNo());
    dto.setName(contract.getName());
    dto.setTemplateId(contract.getTemplateId());
    dto.setContent(contract.getContent());
    dto.setContractType(contract.getContractType());
    dto.setContractTypeName(getContractTypeName(contract.getContractType()));
    dto.setClientId(contract.getClientId());
    // 从预加载的Map获取客户名称
    if (contract.getClientId() != null && clientMap != null) {
      Client client = clientMap.get(contract.getClientId());
      if (client != null) {
        dto.setClientName(client.getName());
      }
    }
    dto.setMatterId(contract.getMatterId());
    // 从预加载的Map获取项目名称
    if (contract.getMatterId() != null && matterMap != null) {
      com.lawfirm.domain.matter.entity.Matter matter = matterMap.get(contract.getMatterId());
      if (matter != null) {
        dto.setMatterName(matter.getName());
      }
    }
    dto.setFeeType(contract.getFeeType());
    dto.setFeeTypeName(contractTemplateVariableService.getFeeTypeName(contract.getFeeType()));
    dto.setTotalAmount(contract.getTotalAmount());
    dto.setPaidAmount(contract.getPaidAmount());
    dto.setUnpaidAmount(
        contract
            .getTotalAmount()
            .subtract(
                contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO));
    dto.setCurrency(contract.getCurrency());
    dto.setSignDate(contract.getSignDate());
    dto.setEffectiveDate(contract.getEffectiveDate());
    dto.setExpiryDate(contract.getExpiryDate());
    dto.setStatus(contract.getStatus());
    dto.setStatusName(getStatusName(contract.getStatus()));
    dto.setSignerId(contract.getSignerId());
    dto.setDepartmentId(contract.getDepartmentId());
    dto.setCreatedBy(contract.getCreatedBy());
    dto.setPaymentTerms(contract.getPaymentTerms());
    dto.setFileUrl(contract.getFileUrl());
    dto.setRemark(contract.getRemark());
    dto.setCreatedAt(contract.getCreatedAt());
    dto.setUpdatedAt(contract.getUpdatedAt());
    // 扩展字段
    dto.setCaseType(contract.getCaseType());
    dto.setCaseTypeName(MatterConstants.getCaseTypeName(contract.getCaseType()));
    dto.setCauseOfAction(contract.getCauseOfAction());
    // 设置案由名称（用于前端显示，特别是审批表）
    dto.setCauseOfActionName(
        getCauseOfActionName(contract.getCauseOfAction(), contract.getCaseType()));
    dto.setTrialStage(contract.getTrialStage());
    dto.setTrialStageName(getTrialStageName(contract.getTrialStage()));
    dto.setClaimAmount(contract.getClaimAmount());
    dto.setJurisdictionCourt(contract.getJurisdictionCourt());
    dto.setOpposingParty(contract.getOpposingParty());
    dto.setConflictCheckStatus(contract.getConflictCheckStatus());
    dto.setConflictCheckStatusName(getConflictCheckStatusName(contract.getConflictCheckStatus()));
    dto.setArchiveStatus(contract.getArchiveStatus());
    dto.setArchiveStatusName(getArchiveStatusName(contract.getArchiveStatus()));
    dto.setAdvanceTravelFee(contract.getAdvanceTravelFee());
    dto.setRiskRatio(contract.getRiskRatio());
    dto.setSealRecord(contract.getSealRecord());
    dto.setCommissionRuleId(contract.getCommissionRuleId());
    dto.setFirmRate(contract.getFirmRate());
    dto.setLeadLawyerRate(contract.getLeadLawyerRate());
    dto.setAssistLawyerRate(contract.getAssistLawyerRate());
    dto.setSupportStaffRate(contract.getSupportStaffRate());
    dto.setOriginatorRate(contract.getOriginatorRate());
    dto.setCaseSummary(contract.getCaseSummary());
    return dto;
  }

  /**
   * 获取审理阶段名称（支持多选，逗号分隔）
   *
   * @param stage 审理阶段代码
   * @return 审理阶段名称
   */
  public String getTrialStageName(final String stage) {
    if (stage == null || stage.isEmpty()) {
      return null;
    }
    // 支持多选：逗号分隔的多个值
    return java.util.Arrays.stream(stage.split(","))
        .map(
            s ->
                switch (s.trim()) {
                    // 通用阶段
                  case "FIRST_INSTANCE" -> "一审";
                  case "SECOND_INSTANCE" -> "二审";
                  case "RETRIAL" -> "再审";
                  case "EXECUTION" -> "执行";
                  case "NON_LITIGATION" -> "非诉服务";
                  case "ARBITRATION" -> "仲裁阶段";
                    // 刑事案件阶段
                  case "INVESTIGATION" -> "侦查阶段";
                  case "PROSECUTION_REVIEW" -> "审查起诉";
                  case "DEATH_PENALTY_REVIEW" -> "死刑复核";
                    // 行政案件阶段
                  case "ADMINISTRATIVE_RECONSIDERATION" -> "行政复议";
                    // 执行案件阶段
                  case "EXECUTION_OBJECTION" -> "执行异议";
                  case "EXECUTION_REVIEW" -> "执行复议";
                  default -> s;
                })
        .collect(java.util.stream.Collectors.joining("、"));
  }

  /**
   * 获取利冲审查状态名称
   *
   * @param status 利冲审查状态代码
   * @return 利冲审查状态名称
   */
  public String getConflictCheckStatusName(final String status) {
    if (status == null) {
      return "未检查";
    }
    return switch (status) {
      case "PENDING" -> "待审查";
      case "PASSED" -> "已通过";
      case "FAILED" -> "未通过";
      case "NOT_REQUIRED" -> "无需审查";
      case "NO_CONFLICT" -> "无冲突";
      case "CONFLICT" -> "存在冲突";
      case "WAIVED" -> "已豁免";
      default -> status;
    };
  }

  /**
   * 获取归档状态名称
   *
   * @param status 归档状态代码
   * @return 归档状态名称
   */
  private String getArchiveStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "NOT_ARCHIVED" -> "未归档";
      case "ARCHIVED" -> "已归档";
      case "DESTROYED" -> "已销毁";
      default -> status;
    };
  }

  /**
   * 获取付款计划状态名称
   *
   * @param status 付款计划状态代码
   * @return 付款计划状态名称
   */
  public String getPaymentScheduleStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "PENDING" -> "待收";
      case "PARTIAL" -> "部分收款";
      case "PAID" -> "已收清";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  /**
   * 获取参与人角色名称
   *
   * @param role 参与人角色代码
   * @return 参与人角色名称
   */
  public String getParticipantRoleName(final String role) {
    if (role == null) {
      return null;
    }
    return switch (role) {
      case "LEAD" -> "承办律师";
      case "CO_COUNSEL" -> "协办律师";
      case "ORIGINATOR" -> "案源人";
      case "PARALEGAL" -> "律师助理";
      default -> role;
    };
  }

  // ========== 付款计划管理 ==========
  // 付款计划相关方法已迁移到 ContractPaymentScheduleService

  // ========== 参与人管理 ==========
  // 参与人相关方法已迁移到 ContractParticipantService

  // ========== 合同模板功能 ==========
  // 模板相关方法已迁移到 ContractTemplateService

  /**
   * 处理模板变量替换 支持所有合同模板变量
   *
   * @param templateContent 模板内容
   * @param contract 合同DTO
   * @param command 创建合同命令
   * @return 处理后的模板内容
   */
  public String processTemplateVariables(
      final String templateContent,
      final ContractDTO contract,
      final CreateContractCommand command) {
    return contractTemplateVariableService.processTemplateVariables(
        templateContent, contract, command);
  }

  // ========== 合同打印功能 ==========
  // 打印相关方法已迁移到 ContractPrintService

  /**
   * 获取案由名称
   *
   * @param causeOfAction 案由代码
   * @param caseType 案件类型
   * @return 案由名称
   */
  public String getCauseOfActionName(final String causeOfAction, final String caseType) {
    if (causeOfAction == null || causeOfAction.isEmpty()) {
      return "";
    }

    String causeType =
        switch (caseType != null ? caseType : "") {
          case "CRIMINAL" -> CauseOfActionService.TYPE_CRIMINAL;
          case "ADMINISTRATIVE" -> CauseOfActionService.TYPE_ADMIN;
          default -> CauseOfActionService.TYPE_CIVIL;
        };

    return causeOfActionService.getCauseName(causeOfAction, causeType);
  }

  // ========== 合同名称生成 ==========
  // 名称生成相关方法已迁移到 ContractNameService

  // ========== 合同文件管理 ==========
  // 文件相关方法已迁移到 ContractFileService

  /**
   * 设置文件存储信息（新字段） 如果fileUrl是MinIO URL，尝试解析并设置新字段；否则只设置fileUrl（向后兼容）
   *
   * @param contract 合同实体
   * @param fileUrl 文件URL
   */
  private void setFileStorageInfo(final Contract contract, final String fileUrl) {
    contract.setFileUrl(fileUrl);

    // 尝试从URL解析存储信息
    Map<String, String> storageInfo = fileAccessService.parseStorageInfoFromUrl(fileUrl);
    if (storageInfo != null) {
      contract.setBucketName(storageInfo.get("bucketName"));
      contract.setStoragePath(storageInfo.get("storagePath"));
      contract.setPhysicalName(storageInfo.get("physicalName"));
      // fileHash无法从URL解析，保持为null
      log.debug(
          "从URL解析存储信息成功: contractId={}, storagePath={}",
          contract.getId(),
          storageInfo.get("storagePath"));
    } else {
      // 不是MinIO URL或无法解析，只设置fileUrl（向后兼容）
      log.debug("无法从URL解析存储信息，仅设置fileUrl: fileUrl={}", fileUrl);
    }
  }

  /**
   * 参与人 Entity 转 DTO
   *
   * @param participant 参与人实体
   * @return 参与人DTO
   */
  private ContractParticipantDTO toParticipantDTO(final ContractParticipant participant) {
    ContractParticipantDTO dto = new ContractParticipantDTO();
    dto.setId(participant.getId());
    dto.setContractId(participant.getContractId());
    dto.setUserId(participant.getUserId());
    // 填充用户名称
    if (participant.getUserId() != null) {
      try {
        var user = userRepository.getById(participant.getUserId());
        if (user != null) {
          dto.setUserName(user.getRealName());
        }
      } catch (Exception e) {
        log.warn("获取用户名称失败，userId: {}", participant.getUserId(), e);
      }
    }
    dto.setRole(participant.getRole());
    dto.setRoleName(getParticipantRoleName(participant.getRole()));
    dto.setCommissionRate(participant.getCommissionRate());
    dto.setRemark(participant.getRemark());
    dto.setCreatedAt(participant.getCreatedAt());
    dto.setUpdatedAt(participant.getUpdatedAt());
    return dto;
  }
}
