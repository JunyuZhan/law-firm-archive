package com.lawfirm.application.contract.service;

import com.lawfirm.application.contract.command.CreateContractTemplateCommand;
import com.lawfirm.application.contract.dto.ContractTemplateDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 合同模板应用服务 ✅ 修复问题581-586: 添加权限验证 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateAppService {

  /** 合同模板仓储 */
  private final ContractTemplateRepository contractTemplateRepository;

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 日期时间格式化器 */
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** 管理角色常量 */
  private static final Set<String> TEMPLATE_MANAGE_ROLES =
      Set.of("ADMIN", "TEAM_LEADER", "DIRECTOR");

  /**
   * 获取所有启用的模板
   *
   * @return 模板列表
   */
  public List<ContractTemplateDTO> getActiveTemplates() {
    return contractTemplateRepository.findActiveTemplates().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取所有模板
   *
   * @return 模板列表
   */
  public List<ContractTemplateDTO> getAllTemplates() {
    return contractTemplateRepository.findAllTemplates().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 按合同类型获取模板
   *
   * @param contractType 合同类型
   * @return 模板列表
   */
  public List<ContractTemplateDTO> getTemplatesByType(final String contractType) {
    // ✅ 参数验证
    if (contractType == null || contractType.trim().isEmpty()) {
      throw new BusinessException("合同类型不能为空");
    }

    return contractTemplateRepository.findByContractType(contractType).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取模板详情
   *
   * @param id 模板ID
   * @return 模板DTO
   */
  public ContractTemplateDTO getTemplate(final Long id) {
    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");
    return toDTO(template);
  }

  /**
   * 创建模板
   *
   * @param command 创建命令
   * @return 模板DTO
   */
  @Transactional
  public ContractTemplateDTO createTemplate(final CreateContractTemplateCommand command) {
    // ✅ 权限验证：只有管理员或合伙人才能创建模板
    requireTemplateManagePermission();

    ContractTemplate template =
        ContractTemplate.builder()
            .templateNo(contractTemplateRepository.generateTemplateNo())
            .name(command.getName())
            .templateType(command.getContractType())
            .feeType(command.getFeeType())
            .content(command.getContent())
            .clauses(command.getClauses())
            .description(command.getDescription())
            .status("ACTIVE")
            .sortOrder(0)
            .build();

    contractTemplateRepository.save(template);
    log.info("合同模板创建成功: {}, 创建人: {}", template.getName(), SecurityUtils.getUserId());
    return toDTO(template);
  }

  /**
   * 更新模板
   *
   * @param id 模板ID
   * @param command 更新命令
   * @return 模板DTO
   */
  @Transactional
  public ContractTemplateDTO updateTemplate(
      final Long id, final CreateContractTemplateCommand command) {
    // ✅ 权限验证
    requireTemplateManagePermission();

    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");

    // ✅ 检查模板是否正在使用
    long usageCount = contractRepository.countByTemplateId(id);
    if (usageCount > 0) {
      log.warn("模板正在被{}个合同使用，修改可能影响已有合同", usageCount);
    }

    template.setName(command.getName());
    template.setTemplateType(command.getContractType());
    template.setFeeType(command.getFeeType());
    template.setContent(command.getContent());
    template.setClauses(command.getClauses());
    template.setDescription(command.getDescription());

    contractTemplateRepository.updateById(template);
    log.info("合同模板更新成功: {}, 操作人: {}", template.getName(), SecurityUtils.getUserId());
    return toDTO(template);
  }

  /**
   * 切换模板状态
   *
   * @param id 模板ID
   */
  @Transactional
  public void toggleStatus(final Long id) {
    // ✅ 权限验证
    requireTemplateManagePermission();

    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");

    // ✅ 禁用前检查是否正在使用
    if ("ACTIVE".equals(template.getStatus())) {
      long usageCount = contractRepository.countByTemplateId(id);
      if (usageCount > 0) {
        log.warn("模板正在被{}个合同使用，禁用后新合同将无法使用此模板", usageCount);
      }
    }

    template.setStatus("ACTIVE".equals(template.getStatus()) ? "INACTIVE" : "ACTIVE");
    contractTemplateRepository.updateById(template);
    log.info(
        "合同模板状态切换: {} -> {}, 操作人: {}",
        template.getName(),
        template.getStatus(),
        SecurityUtils.getUserId());
  }

  /**
   * 删除模板
   *
   * @param id 模板ID
   */
  @Transactional
  public void deleteTemplate(final Long id) {
    // ✅ 权限验证
    requireTemplateManagePermission();

    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(id, "模板不存在");

    // ✅ 检查模板是否正在使用
    long usageCount = contractRepository.countByTemplateId(id);
    if (usageCount > 0) {
      throw new BusinessException("模板正在被" + usageCount + "个合同使用，无法删除");
    }

    template.setDeleted(true);
    contractTemplateRepository.updateById(template);
    log.info("合同模板删除成功: {}, 操作人: {}", template.getName(), SecurityUtils.getUserId());
  }

  /**
   * 权限验证：只有管理员或合伙人才能管理模板.
   *
   * @throws BusinessException 如果用户没有权限
   */
  private void requireTemplateManagePermission() {
    Set<String> roles = SecurityUtils.getRoles();
    boolean hasPermission = roles.stream().anyMatch(TEMPLATE_MANAGE_ROLES::contains);
    if (!hasPermission) {
      throw new BusinessException("权限不足：只有管理员或合伙人才能管理合同模板");
    }
  }

  /**
   * 转换为DTO
   *
   * @param entity 实体
   * @return DTO
   */
  private ContractTemplateDTO toDTO(final ContractTemplate entity) {
    ContractTemplateDTO dto = new ContractTemplateDTO();
    BeanUtils.copyProperties(entity, dto);
    if (entity.getCreatedAt() != null) {
      dto.setCreatedAt(entity.getCreatedAt().format(FORMATTER));
    }
    if (entity.getUpdatedAt() != null) {
      dto.setUpdatedAt(entity.getUpdatedAt().format(FORMATTER));
    }
    return dto;
  }
}
