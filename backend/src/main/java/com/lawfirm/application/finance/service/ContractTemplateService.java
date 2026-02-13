package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.common.constant.ContractStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 合同模板服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 模板仓储 */
  private final ContractTemplateRepository contractTemplateRepository;

  /** 合同应用服务 */
  private final ContractAppService contractAppService;

  /** 合同模板变量服务 */
  private final ContractTemplateVariableService contractTemplateVariableService;

  /**
   * 基于模板创建合同
   *
   * <p>选择模板时自动填充 contract_type, fee_type, content 字段
   *
   * @param templateId 模板ID
   * @param command 创建合同命令对象
   * @return 创建的合同DTO
   */
  @Transactional
  public ContractDTO createFromTemplate(
      final Long templateId, final CreateContractCommand command) {
    // 获取模板
    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(templateId, "模板不存在");

    if (!ContractStatus.ACTIVE.equals(template.getStatus())) {
      throw new BusinessException("模板已停用，无法使用");
    }

    // 从模板填充字段（如果命令中未指定）
    populateCommandFromTemplate(command, template);

    // 创建合同
    ContractDTO contract = contractAppService.createContract(command);

    // 获取合同实体并设置模板ID
    Contract entity = contractRepository.getById(contract.getId());
    entity.setTemplateId(templateId);

    // 处理模板内容的变量替换，存储到 content 字段
    if (StringUtils.hasText(template.getContent())) {
      String processedContent =
          contractTemplateVariableService.processTemplateVariables(
              template.getContent(), contract, command);
      entity.setContent(processedContent);
      contract.setContent(processedContent);
    }

    contractRepository.updateById(entity);
    contract.setTemplateId(templateId);

    log.info("基于模板创建合同成功: templateId={}, contractId={}", templateId, contract.getId());
    return contract;
  }

  /**
   * 从模板填充命令字段
   *
   * @param command 创建合同命令
   * @param template 合同模板
   */
  private void populateCommandFromTemplate(
      final CreateContractCommand command, final ContractTemplate template) {
    // 合同类型等于模板类型
    if (!StringUtils.hasText(command.getContractType())
        && StringUtils.hasText(template.getTemplateType())) {
      command.setContractType(template.getTemplateType());
    }
    if (!StringUtils.hasText(command.getFeeType())) {
      command.setFeeType(template.getFeeType());
    }
    // 如果未指定案件类型，根据模板类型推断
    if (!StringUtils.hasText(command.getCaseType())
        && StringUtils.hasText(template.getTemplateType())) {
      String inferredCaseType =
          switch (template.getTemplateType()) {
            case "CRIMINAL_DEFENSE" -> "CRIMINAL";
            case "CIVIL_PROXY" -> "CIVIL";
            case "ADMINISTRATIVE_PROXY" -> "ADMINISTRATIVE";
            case "LEGAL_COUNSEL" -> "LEGAL_COUNSEL";
            case "NON_LITIGATION" -> null; // 非诉项目不需要 caseType
            default -> null;
          };
      if (inferredCaseType != null) {
        command.setCaseType(inferredCaseType);
      }
    }
  }

  /**
   * 预览模板变量替换结果
   *
   * <p>用于前端预览合同内容
   *
   * @param templateId 模板ID
   * @param command 创建合同命令对象
   * @return 替换变量后的模板内容
   */
  public String previewTemplateContent(final Long templateId, final CreateContractCommand command) {
    ContractTemplate template = contractTemplateRepository.getByIdOrThrow(templateId, "模板不存在");

    if (!StringUtils.hasText(template.getContent())) {
      return "";
    }

    // 构建临时 DTO 用于变量替换
    ContractDTO tempDto = buildTempContractDTO(command);

    return contractTemplateVariableService.processTemplateVariables(
        template.getContent(), tempDto, command);
  }

  /**
   * 构建临时合同DTO用于预览
   *
   * @param command 创建合同命令
   * @return 临时合同DTO
   */
  private ContractDTO buildTempContractDTO(final CreateContractCommand command) {
    ContractDTO tempDto = new ContractDTO();
    tempDto.setClientId(command.getClientId());
    tempDto.setTotalAmount(command.getTotalAmount());
    tempDto.setSignDate(command.getSignDate());
    tempDto.setEffectiveDate(command.getEffectiveDate());
    tempDto.setExpiryDate(command.getExpiryDate());
    tempDto.setSignerId(command.getSignerId());
    tempDto.setClaimAmount(command.getClaimAmount());
    tempDto.setJurisdictionCourt(command.getJurisdictionCourt());
    tempDto.setOpposingParty(command.getOpposingParty());
    tempDto.setCauseOfAction(command.getCauseOfAction());
    tempDto.setTrialStage(command.getTrialStage());
    tempDto.setMatterId(command.getMatterId());
    tempDto.setContractNo("[待生成]");
    return tempDto;
  }
}
