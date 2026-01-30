package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractPrintDTO;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 合同打印服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPrintService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 客户仓储 */
  private final ClientRepository clientRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 参与人仓储 */
  private final ContractParticipantRepository participantRepository;

  /** 模板仓储 */
  private final ContractTemplateRepository contractTemplateRepository;

  /** 审批应用服务 */
  private final ApprovalAppService approvalAppService;

  /** 合同应用服务 */
  private final ContractAppService contractAppService;

  /** 系统配置服务 */
  private final com.lawfirm.application.system.service.SysConfigAppService sysConfigAppService;

  /** 合同模板变量服务 */
  private final ContractTemplateVariableService contractTemplateVariableService;

  /**
   * 获取合同打印数据
   *
   * <p>用于打印合同和收案审批表
   *
   * @param contractId 合同ID
   * @return 合同打印数据DTO
   */
  public ContractPrintDTO getContractPrintData(final Long contractId) {
    Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

    ContractPrintDTO printDTO = new ContractPrintDTO();

    // 基本信息
    populateBasicInfo(printDTO, contract);

    // 委托人信息
    populateClientInfo(printDTO, contract);

    // 律所信息
    populateFirmInfo(printDTO);

    // 案件信息
    populateMatterInfo(printDTO, contract);

    // 费用信息
    populateFeeInfo(printDTO, contract);

    // 时间信息
    populateTimeInfo(printDTO, contract);

    // 人员信息
    populatePersonnelInfo(printDTO, contract, contractId);

    // 利冲信息
    populateConflictCheckInfo(printDTO, contract);

    // 审批信息
    populateApprovalInfo(printDTO, contractId);

    // 获取合同内容
    populateContractContent(printDTO, contract, contractId);

    return printDTO;
  }

  /**
   * 填充基本信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateBasicInfo(final ContractPrintDTO printDTO, final Contract contract) {
    printDTO.setId(contract.getId());
    printDTO.setContractNo(contract.getContractNo());
    printDTO.setName(contract.getName());
    printDTO.setContractType(contract.getContractType());
    printDTO.setContractTypeName(
        contractAppService.getContractTypeName(contract.getContractType()));
    printDTO.setStatus(contract.getStatus());
    printDTO.setStatusName(contractAppService.getStatusName(contract.getStatus()));
  }

  /**
   * 填充委托人信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateClientInfo(final ContractPrintDTO printDTO, final Contract contract) {
    if (contract.getClientId() != null) {
      Client client = clientRepository.findById(contract.getClientId());
      if (client != null) {
        printDTO.setClientId(client.getId());
        printDTO.setClientName(client.getName());
        printDTO.setClientType(client.getClientType());
        printDTO.setClientTypeName("INDIVIDUAL".equals(client.getClientType()) ? "个人" : "企业");
        printDTO.setClientAddress(client.getRegisteredAddress());
        printDTO.setClientPhone(client.getContactPhone());
        // 根据客户类型使用不同的证件号码字段
        if ("INDIVIDUAL".equals(client.getClientType())) {
          printDTO.setClientIdNumber(client.getIdCard());
        } else {
          printDTO.setClientIdNumber(client.getCreditCode());
        }
      }
    }
  }

  /**
   * 填充律所信息
   *
   * @param printDTO 打印DTO
   */
  private void populateFirmInfo(final ContractPrintDTO printDTO) {
    try {
      printDTO.setFirmName(sysConfigAppService.getConfigValue("firm.name"));
      printDTO.setFirmAddress(sysConfigAppService.getConfigValue("firm.address"));
      printDTO.setFirmPhone(sysConfigAppService.getConfigValue("firm.phone"));
      printDTO.setFirmLegalRep(sysConfigAppService.getConfigValue("firm.legal.rep"));
    } catch (Exception e) {
      log.warn("获取律所信息失败", e);
      printDTO.setFirmName("");
    }
  }

  /**
   * 填充案件信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateMatterInfo(final ContractPrintDTO printDTO, final Contract contract) {
    printDTO.setCaseType(contract.getCaseType());
    printDTO.setCaseTypeName(MatterConstants.getCaseTypeName(contract.getCaseType()));
    printDTO.setCauseOfAction(contract.getCauseOfAction());
    // 案由名称转换
    printDTO.setCauseOfActionName(
        contractAppService.getCauseOfActionName(
            contract.getCauseOfAction(), contract.getCaseType()));
    printDTO.setTrialStage(contract.getTrialStage());
    printDTO.setTrialStageName(contractAppService.getTrialStageName(contract.getTrialStage()));
    printDTO.setOpposingParty(contract.getOpposingParty());
    printDTO.setJurisdictionCourt(contract.getJurisdictionCourt());
    printDTO.setClaimAmount(contract.getClaimAmount());
    printDTO.setDescription(contract.getCaseSummary()); // 案情摘要
  }

  /**
   * 填充费用信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateFeeInfo(final ContractPrintDTO printDTO, final Contract contract) {
    printDTO.setFeeType(contract.getFeeType());
    printDTO.setFeeTypeName(contractTemplateVariableService.getFeeTypeName(contract.getFeeType()));
    printDTO.setTotalAmount(contract.getTotalAmount());
  }

  /**
   * 填充时间信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateTimeInfo(final ContractPrintDTO printDTO, final Contract contract) {
    printDTO.setSignDate(contract.getSignDate());
    printDTO.setEffectiveDate(contract.getEffectiveDate());
    printDTO.setExpiryDate(contract.getExpiryDate());
  }

  /**
   * 填充人员信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   * @param contractId 合同ID
   */
  private void populatePersonnelInfo(
      final ContractPrintDTO printDTO, final Contract contract, final Long contractId) {
    // 签约人信息
    if (contract.getSignerId() != null) {
      User signer = userRepository.findById(contract.getSignerId());
      if (signer != null) {
        printDTO.setSignerId(signer.getId());
        printDTO.setSignerName(signer.getRealName());
      }
    }

    // 获取参与人（问题244修复：使用批量加载避免N+1查询）
    List<ContractParticipant> participants = participantRepository.findByContractId(contractId);
    String leadLawyerName = null;
    StringBuilder assistLawyerNames = new StringBuilder();
    String originatorName = null;

    if (!participants.isEmpty()) {
      // 批量加载参与人的用户信息
      Set<Long> participantUserIds =
          participants.stream()
              .map(ContractParticipant::getUserId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      Map<Long, User> participantUserMap =
          participantUserIds.isEmpty()
              ? Collections.emptyMap()
              : userRepository.listByIds(participantUserIds).stream()
                  .collect(Collectors.toMap(User::getId, u -> u));

      for (ContractParticipant p : participants) {
        User user = participantUserMap.get(p.getUserId());
        if (user != null) {
          if ("LEAD".equals(p.getRole())) {
            leadLawyerName = user.getRealName();
          } else if ("CO_COUNSEL".equals(p.getRole())) {
            if (assistLawyerNames.length() > 0) {
              assistLawyerNames.append("、");
            }
            assistLawyerNames.append(user.getRealName());
          } else if ("ORIGINATOR".equals(p.getRole())) {
            originatorName = user.getRealName();
          }
        }
      }
    }
    printDTO.setLeadLawyerName(leadLawyerName);
    printDTO.setAssistLawyerNames(assistLawyerNames.toString());
    printDTO.setOriginatorName(originatorName);
  }

  /**
   * 填充利冲信息
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   */
  private void populateConflictCheckInfo(final ContractPrintDTO printDTO, final Contract contract) {
    printDTO.setConflictCheckStatus(contract.getConflictCheckStatus());
    printDTO.setConflictCheckStatusName(
        contractAppService.getConflictCheckStatusName(contract.getConflictCheckStatus()));
    // 根据合同利冲检查状态判断结果显示
    String conflictCheckStatus = contract.getConflictCheckStatus();
    if (conflictCheckStatus == null || "NOT_REQUIRED".equals(conflictCheckStatus)) {
      // 无需审查：创建客户时已通过利冲检查，合同无需再次审查
      printDTO.setConflictCheckResult("无需审查");
    } else if ("PASSED".equals(conflictCheckStatus)) {
      // 已通过：利冲审查已通过，无冲突
      printDTO.setConflictCheckResult("无");
    } else if ("FAILED".equals(conflictCheckStatus)) {
      // 未通过：利冲审查未通过，存在冲突
      printDTO.setConflictCheckResult("有");
    } else if ("PENDING".equals(conflictCheckStatus)) {
      // 待审查：需要利冲审查
      printDTO.setConflictCheckResult("待审查");
    } else {
      // 其他状态使用状态名称
      printDTO.setConflictCheckResult(
          contractAppService.getConflictCheckStatusName(conflictCheckStatus));
    }
  }

  /**
   * 填充审批信息
   *
   * @param printDTO 打印DTO
   * @param contractId 合同ID
   */
  private void populateApprovalInfo(final ContractPrintDTO printDTO, final Long contractId) {
    List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals("CONTRACT", contractId);
    if (approvals != null && !approvals.isEmpty()) {
      printDTO.setApprovals(
          approvals.stream()
              .map(
                  a -> {
                    ContractPrintDTO.ApprovalInfo info = new ContractPrintDTO.ApprovalInfo();
                    info.setApproverName(a.getApproverName());
                    // 根据审批人角色判断（简化处理：第一个审批人为接待律师，后续为律所领导）
                    info.setApproverRole("律所领导");
                    info.setStatus(a.getStatus());
                    info.setStatusName(a.getStatusName());
                    info.setComment(a.getComment());
                    info.setApprovedAt(a.getApprovedAt());
                    return info;
                  })
              .collect(Collectors.toList()));
    }
  }

  /**
   * 填充合同内容
   *
   * @param printDTO 打印DTO
   * @param contract 合同实体
   * @param contractId 合同ID
   */
  private void populateContractContent(
      final ContractPrintDTO printDTO, final Contract contract, final Long contractId) {
    // 优先使用已经替换好变量的内容（存储在remark字段中）
    if (StringUtils.hasText(contract.getRemark())) {
      printDTO.setContractContent(contract.getRemark());
    } else {
      // 如果remark为空，则尝试从模板重新生成（兼容旧数据）
      try {
        Long templateId = 1L; // 默认模板
        ContractTemplate template = contractTemplateRepository.findById(templateId);
        if (template != null && StringUtils.hasText(template.getContent())) {
          ContractDTO tempDto = contractAppService.getContractById(contractId);
          CreateContractCommand tempCommand = new CreateContractCommand();
          tempCommand.setClientId(contract.getClientId());
          String content =
              contractAppService.processTemplateVariables(
                  template.getContent(), tempDto, tempCommand);
          printDTO.setContractContent(content);
        }
      } catch (Exception e) {
        log.warn("获取模板内容失败", e);
      }
    }
  }
}
