package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Service for processing contract template variables. */
@Service
public class ContractTemplateVariableService {

  /** 日期格式化器 */
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Process template variables.
   *
   * @param templateContent template content
   * @param contract contract DTO
   * @param command create contract command
   * @return processed content
   */
  public String processTemplateVariables(
      final String templateContent,
      final ContractDTO contract,
      final CreateContractCommand command) {
    if (templateContent == null || templateContent.isEmpty()) {
      return "";
    }

    Map<String, String> variables = new HashMap<>();

    if (contract != null) {
      populateContractVariables(variables, contract);
    }

    if (command != null) {
      populateCommandVariables(variables, command);
    }

    return replaceVariables(templateContent, variables);
  }

  private void populateContractVariables(
      final Map<String, String> variables, final ContractDTO contract) {
    variables.put("contractNo", contract.getContractNo());
    variables.put("contractName", contract.getName());
    variables.put("clientName", contract.getClientName());
    variables.put("matterName", contract.getMatterName());
    variables.put("totalAmount", formatMoney(contract.getTotalAmount()));
    variables.put("currency", contract.getCurrency());
    variables.put("signDate", formatDate(contract.getSignDate()));
    variables.put("effectiveDate", formatDate(contract.getEffectiveDate()));
    variables.put("expiryDate", formatDate(contract.getExpiryDate()));
    variables.put("signerName", contract.getSignerName());
    variables.put("departmentName", contract.getDepartmentName());
    variables.put("feeTypeName", getFeeTypeName(contract.getFeeType()));
    variables.put("paymentTerms", contract.getPaymentTerms());
    variables.put("remark", contract.getRemark());
    variables.put("jurisdictionCourt", contract.getJurisdictionCourt());
    variables.put("claimAmount", formatMoney(contract.getClaimAmount()));
    variables.put("caseTypeName", contract.getCaseTypeName());
    variables.put("causeOfActionName", contract.getCauseOfActionName());
    variables.put("trialStageName", contract.getTrialStageName());
  }

  private void populateCommandVariables(
      final Map<String, String> variables, final CreateContractCommand command) {
    // Override or add variables from command if present (usually for preview before creation)
    if (command.getName() != null) {
      variables.put("contractName", command.getName());
    }
    if (command.getTotalAmount() != null) {
      variables.put("totalAmount", formatMoney(command.getTotalAmount()));
    }
    if (command.getCurrency() != null) {
      variables.put("currency", command.getCurrency());
    }
    if (command.getSignDate() != null) {
      variables.put("signDate", formatDate(command.getSignDate()));
    }
    if (command.getEffectiveDate() != null) {
      variables.put("effectiveDate", formatDate(command.getEffectiveDate()));
    }
    if (command.getExpiryDate() != null) {
      variables.put("expiryDate", formatDate(command.getExpiryDate()));
    }
    if (command.getPaymentTerms() != null) {
      variables.put("paymentTerms", command.getPaymentTerms());
    }
    if (command.getRemark() != null) {
      variables.put("remark", command.getRemark());
    }
    if (command.getJurisdictionCourt() != null) {
      variables.put("jurisdictionCourt", command.getJurisdictionCourt());
    }
    if (command.getClaimAmount() != null) {
      variables.put("claimAmount", formatMoney(command.getClaimAmount()));
    }
    if (command.getOpposingParty() != null) {
      variables.put("opposingParty", command.getOpposingParty());
    }
    if (command.getFeeType() != null) {
      variables.put("feeTypeName", getFeeTypeName(command.getFeeType()));
    }
  }

  private String replaceVariables(final String content, final Map<String, String> variables) {
    String result = content;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";
      result = result.replace("{" + key + "}", value);
    }
    return result;
  }

  private String formatDate(final LocalDate date) {
    return date != null ? date.format(DATE_FORMATTER) : "";
  }

  private String formatMoney(final BigDecimal amount) {
    return amount != null ? amount.toString() : "0.00";
  }

  /**
   * Get fee type name.
   *
   * @param feeType fee type code
   * @return fee type name
   */
  public String getFeeTypeName(final String feeType) {
    if (feeType == null) {
      return "";
    }
    // Simple mapping for now, ideally should use dictionary or enum
    switch (feeType) {
      case "FIXED":
        return "固定收费";
      case "HOURLY":
        return "计时收费";
      case "CONTINGENCY":
        return "风险代理";
      case "MIXED":
        return "混合收费";
      default:
        return feeType;
    }
  }
}
