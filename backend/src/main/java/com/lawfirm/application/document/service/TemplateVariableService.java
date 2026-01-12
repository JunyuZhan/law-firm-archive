package com.lawfirm.application.document.service;

import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板变量服务
 * 用于收集和替换模板变量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateVariableService {

    private final MatterRepository matterRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final ApprovalMapper approvalMapper;
    private final SysConfigMapper sysConfigMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter DATE_SHORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 收集案件相关的所有变量值
     */
    public Map<String, Object> collectVariables(Long matterId) {
        Map<String, Object> variables = new HashMap<>();

        // 获取案件信息
        Matter matter = matterRepository.getByIdOrThrow(matterId, "案件不存在");
        collectMatterVariables(variables, matter);

        // 获取客户信息
        if (matter.getClientId() != null) {
            Client client = clientRepository.findById(matter.getClientId());
            if (client != null) {
                collectClientVariables(variables, client);
            }
        }

        // 获取主办律师信息
        if (matter.getLeadLawyerId() != null) {
            User lawyer = userRepository.findById(matter.getLeadLawyerId());
            if (lawyer != null) {
                collectLawyerVariables(variables, lawyer);
            }
        }

        // 获取合同信息
        if (matter.getContractId() != null) {
            Contract contract = contractRepository.getById(matter.getContractId());
            if (contract != null) {
                collectContractVariables(variables, contract);
                
                // 获取审批信息
                List<Approval> approvals = approvalMapper.selectByBusiness("CONTRACT", contract.getId());
                if (approvals != null && !approvals.isEmpty()) {
                    Approval approval = approvals.stream()
                        .filter(a -> "APPROVED".equals(a.getStatus()))
                        .findFirst()
                        .orElse(approvals.get(0));
                    collectApprovalVariables(variables, approval);
                }
            }
        }

        // 日期变量
        collectDateVariables(variables);

        // 律所信息
        collectFirmVariables(variables);

        // 对方律师信息
        if (matter.getOpposingLawyerName() != null) {
            variables.put("opposingLawyer.name", matter.getOpposingLawyerName());
            variables.put("opposingLawyer.licenseNo", nullToEmpty(matter.getOpposingLawyerLicenseNo()));
            variables.put("opposingLawyer.firm", nullToEmpty(matter.getOpposingLawyerFirm()));
            variables.put("opposingLawyer.phone", nullToEmpty(matter.getOpposingLawyerPhone()));
            variables.put("opposingLawyer.email", nullToEmpty(matter.getOpposingLawyerEmail()));
        }

        log.debug("收集模板变量完成: matterId={}, variables={}", matterId, variables.keySet());
        return variables;
    }

    /**
     * 收集带合同和审批信息的变量（用于自动归档）
     */
    public Map<String, Object> collectVariablesWithContractAndApproval(
            Long matterId, Long contractId, Approval approval) {
        Map<String, Object> variables = collectVariables(matterId);

        // 覆盖合同信息
        if (contractId != null) {
            Contract contract = contractRepository.getById(contractId);
            if (contract != null) {
                collectContractVariables(variables, contract);
            }
        }

        // 覆盖审批信息
        if (approval != null) {
            collectApprovalVariables(variables, approval);
        }

        return variables;
    }

    /**
     * 收集项目变量
     */
    private void collectMatterVariables(Map<String, Object> variables, Matter matter) {
        variables.put("matter.name", nullToEmpty(matter.getName()));
        variables.put("matter.no", nullToEmpty(matter.getMatterNo()));
        variables.put("matter.status", nullToEmpty(matter.getStatus()));
        variables.put("matter.businessType", nullToEmpty(matter.getBusinessType()));
        variables.put("matter.caseType", nullToEmpty(matter.getCaseType()));
        variables.put("matter.caseTypeName", getCaseTypeName(matter.getCaseType()));
        variables.put("matter.matterType", nullToEmpty(matter.getMatterType()));
        variables.put("matter.matterTypeName", getMatterTypeName(matter.getMatterType()));
        variables.put("matter.description", nullToEmpty(matter.getDescription()));
        variables.put("matter.opposingParty", nullToEmpty(matter.getOpposingParty()));
        variables.put("matter.causeOfAction", nullToEmpty(matter.getCauseOfAction()));
        variables.put("matter.litigationStage", nullToEmpty(matter.getLitigationStage()));
        variables.put("matter.litigationStageName", getLitigationStageName(matter.getLitigationStage()));
        
        if (matter.getFilingDate() != null) {
            variables.put("matter.filingDate", matter.getFilingDate().format(DATE_FORMATTER));
        } else {
            variables.put("matter.filingDate", "");
        }
        if (matter.getExpectedClosingDate() != null) {
            variables.put("matter.expectedClosingDate", matter.getExpectedClosingDate().format(DATE_FORMATTER));
        } else {
            variables.put("matter.expectedClosingDate", "");
        }
    }

    /**
     * 收集客户变量
     */
    private void collectClientVariables(Map<String, Object> variables, Client client) {
        variables.put("client.name", nullToEmpty(client.getName()));
        variables.put("client.type", nullToEmpty(client.getClientType()));
        variables.put("client.typeName", getClientTypeName(client.getClientType()));
        variables.put("client.legalPerson", nullToEmpty(client.getLegalRepresentative()));
        variables.put("client.address", nullToEmpty(client.getRegisteredAddress()));
        variables.put("client.phone", nullToEmpty(client.getContactPhone()));
        variables.put("client.email", nullToEmpty(client.getContactEmail()));
        variables.put("client.creditCode", nullToEmpty(client.getCreditCode()));
        
        // 根据客户类型设置身份标识
        if ("ENTERPRISE".equals(client.getClientType())) {
            variables.put("client.idLabel", "统一社会信用代码");
            variables.put("client.idNumber", nullToEmpty(client.getCreditCode()));
        } else {
            variables.put("client.idLabel", "身份证号");
            variables.put("client.idNumber", nullToEmpty(client.getIdCard()));
        }
    }

    /**
     * 收集律师变量
     */
    private void collectLawyerVariables(Map<String, Object> variables, User lawyer) {
        variables.put("lawyer.name", nullToEmpty(lawyer.getRealName()));
        variables.put("lawyer.phone", nullToEmpty(lawyer.getPhone()));
        variables.put("lawyer.email", nullToEmpty(lawyer.getEmail()));
        variables.put("lawyer.licenseNo", nullToEmpty(lawyer.getLawyerLicenseNo()));
    }

    /**
     * 收集合同变量
     */
    private void collectContractVariables(Map<String, Object> variables, Contract contract) {
        variables.put("contract.no", nullToEmpty(contract.getContractNo()));
        variables.put("contract.name", nullToEmpty(contract.getName()));
        variables.put("contract.feeType", nullToEmpty(contract.getFeeType()));
        variables.put("contract.feeTypeName", getFeeTypeName(contract.getFeeType()));
        variables.put("contract.paymentTerms", nullToEmpty(contract.getPaymentTerms()));
        variables.put("contract.caseType", nullToEmpty(contract.getCaseType()));
        variables.put("contract.opposingParty", nullToEmpty(contract.getOpposingParty()));
        variables.put("contract.causeOfAction", nullToEmpty(contract.getCauseOfAction()));
        
        if (contract.getTotalAmount() != null) {
            variables.put("contract.totalAmount", String.format("%.2f", contract.getTotalAmount()));
            variables.put("contract.totalAmountCN", convertToChineseAmount(contract.getTotalAmount()));
        } else {
            variables.put("contract.totalAmount", "0.00");
            variables.put("contract.totalAmountCN", "零元整");
        }
        
        if (contract.getSignDate() != null) {
            variables.put("contract.signDate", contract.getSignDate().format(DATE_FORMATTER));
            variables.put("contract.year", String.valueOf(contract.getSignDate().getYear()));
        } else {
            variables.put("contract.signDate", "");
            variables.put("contract.year", String.valueOf(LocalDate.now().getYear()));
        }
        
        // 代理权限和审理阶段 - 目前使用默认值，可后续在合同实体中扩展
        // 诉讼案件默认使用特别代理，非诉默认使用一般代理
        String authType = "LITIGATION".equals(contract.getContractType()) || 
                         "CIVIL".equals(contract.getCaseType()) || 
                         "CRIMINAL".equals(contract.getCaseType()) ? "SPECIAL" : "GENERAL";
        variables.put("authorizationType", authType.equals("SPECIAL") ? "特别代理" : "一般代理");
        variables.put("authorizationScope", getAuthorizationScope(authType));
        variables.put("trialStage", getTrialStageName(contract.getTrialStage()));
        variables.put("assistantNames", ""); // 助理律师名称，可后续扩展
        variables.put("specialTerms", "无"); // 特别约定，可后续在合同实体中扩展
    }

    /**
     * 收集审批变量
     */
    private void collectApprovalVariables(Map<String, Object> variables, Approval approval) {
        variables.put("approval.status", nullToEmpty(approval.getStatus()));
        variables.put("approval.statusName", getApprovalStatusName(approval.getStatus()));
        variables.put("approval.approverName", nullToEmpty(approval.getApproverName()));
        variables.put("approval.comment", nullToEmpty(approval.getComment()));
        
        if (approval.getApprovedAt() != null) {
            variables.put("approval.approvedAt", approval.getApprovedAt().format(DATE_FORMATTER));
        } else {
            variables.put("approval.approvedAt", "");
        }
        if (approval.getCreatedAt() != null) {
            variables.put("approval.createdAt", approval.getCreatedAt().format(DATE_FORMATTER));
        } else {
            variables.put("approval.createdAt", "");
        }
    }

    /**
     * 收集日期变量
     */
    private void collectDateVariables(Map<String, Object> variables) {
        LocalDate today = LocalDate.now();
        variables.put("date.today", today.format(DATE_FORMATTER));
        variables.put("date.todayShort", today.format(DATE_SHORT_FORMATTER));
        variables.put("date.year", String.valueOf(today.getYear()));
        variables.put("date.month", String.valueOf(today.getMonthValue()));
        variables.put("date.day", String.valueOf(today.getDayOfMonth()));
    }

    /**
     * 收集律所变量
     */
    private void collectFirmVariables(Map<String, Object> variables) {
        // 从系统配置中获取律所信息
        String firmName = getConfigValue("firm.name", "律师事务所");
        String firmAddress = getConfigValue("firm.address", "");
        String firmPhone = getConfigValue("firm.phone", "");
        String firmLegalRep = getConfigValue("firm.legalRep", "");
        
        variables.put("firm.name", firmName);
        variables.put("firm.address", firmAddress);
        variables.put("firm.phone", firmPhone);
        variables.put("firm.legalRep", firmLegalRep);
    }

    /**
     * 从系统配置获取值
     */
    private String getConfigValue(String key, String defaultValue) {
        try {
            String value = sysConfigMapper.selectValueByKey(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 替换模板内容中的变量
     * 支持 ${variable.name} 格式的变量
     */
    public String replaceVariables(String templateContent, Map<String, Object> variables) {
        if (templateContent == null) {
            return null;
        }

        String result = templateContent;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(key, value);
        }

        // 清理未替换的变量占位符
        result = result.replaceAll("\\$\\{[^}]+\\}", "");

        return result;
    }

    /**
     * 空值转空字符串
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * 获取案件类型名称
     */
    private String getCaseTypeName(String caseType) {
        if (caseType == null) return "";
        return switch (caseType) {
            case "CIVIL" -> "民事案件";
            case "CRIMINAL" -> "刑事案件";
            case "ADMINISTRATIVE" -> "行政案件";
            case "ARBITRATION" -> "仲裁案件";
            case "NON_LITIGATION" -> "非诉讼业务";
            default -> caseType;
        };
    }

    /**
     * 获取项目类型名称
     */
    private String getMatterTypeName(String matterType) {
        if (matterType == null) return "";
        return switch (matterType) {
            case "LITIGATION" -> "诉讼";
            case "NON_LITIGATION" -> "非诉讼";
            case "LEGAL_COUNSEL" -> "法律顾问";
            case "CONSULTATION" -> "法律咨询";
            default -> matterType;
        };
    }

    /**
     * 获取客户类型名称
     */
    private String getClientTypeName(String clientType) {
        if (clientType == null) return "";
        return switch (clientType) {
            case "ENTERPRISE" -> "企业客户";
            case "INDIVIDUAL" -> "个人客户";
            case "GOVERNMENT" -> "政府机关";
            case "INSTITUTION" -> "事业单位";
            default -> clientType;
        };
    }

    /**
     * 获取收费方式名称
     */
    private String getFeeTypeName(String feeType) {
        if (feeType == null) return "";
        return switch (feeType) {
            case "FIXED" -> "固定收费";
            case "HOURLY" -> "计时收费";
            case "CONTINGENCY" -> "风险代理";
            case "RETAINER" -> "顾问费";
            case "MIXED" -> "混合收费";
            default -> feeType;
        };
    }

    /**
     * 获取审批状态名称
     */
    private String getApprovalStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "CANCELLED" -> "已撤销";
            default -> status;
        };
    }

    /**
     * 获取代理权限范围描述
     */
    private String getAuthorizationScope(String authType) {
        if (authType == null || "GENERAL".equals(authType)) {
            return "一般代理：拟写诉讼（仲裁）文书、起诉（立案）、应诉、调查取证、参加庭审、签收诉讼（仲裁）法律文书等诉讼活动。";
        } else if ("SPECIAL".equals(authType)) {
            return "特别代理：承认、变更、放弃诉讼（仲裁）请求，决定是否调解、和解并签订调解、和解协议，提起反诉或上诉，代为申请执行并收付执行款物，以及行使与案件有关的其他诉讼权利。";
        }
        return "";
    }

    /**
     * 获取审理阶段名称
     */
    private String getTrialStageName(String trialStage) {
        if (trialStage == null) return "一审";
        return switch (trialStage) {
            case "FIRST_TRIAL" -> "一审";
            case "SECOND_TRIAL" -> "二审";
            case "RETRIAL" -> "再审";
            case "EXECUTION" -> "执行";
            default -> trialStage;
        };
    }

    /**
     * 获取代理阶段名称
     */
    private String getLitigationStageName(String litigationStage) {
        if (litigationStage == null) return "";
        return switch (litigationStage) {
            // 通用阶段
            case "FIRST_INSTANCE" -> "一审";
            case "SECOND_INSTANCE" -> "二审";
            case "RETRIAL" -> "再审";
            case "EXECUTION" -> "执行";
            case "ARBITRATION" -> "仲裁阶段";
            // 刑事案件特有
            case "INVESTIGATION" -> "侦查阶段";
            case "PROSECUTION_REVIEW" -> "审查起诉";
            case "DEATH_PENALTY_REVIEW" -> "死刑复核";
            // 行政案件特有
            case "ADMINISTRATIVE_RECONSIDERATION" -> "行政复议";
            // 执行案件特有
            case "EXECUTION_OBJECTION" -> "执行异议";
            case "EXECUTION_REVIEW" -> "执行复议";
            // 非诉
            case "NON_LITIGATION" -> "非诉服务";
            default -> litigationStage;
        };
    }

    /**
     * 金额转中文大写
     */
    private String convertToChineseAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "零元整";
        }

        String[] cnNumbers = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] cnUnits = {"", "拾", "佰", "仟"};
        String[] cnGroupUnits = {"", "万", "亿"};

        // 取整数部分
        long integerPart = amount.longValue();
        if (integerPart == 0) {
            return "零元整";
        }

        StringBuilder result = new StringBuilder();
        int groupIndex = 0;
        boolean needZero = false;

        while (integerPart > 0) {
            int group = (int) (integerPart % 10000);
            if (group > 0) {
                StringBuilder groupStr = new StringBuilder();
                int unitIndex = 0;
                while (group > 0) {
                    int digit = group % 10;
                    if (digit > 0) {
                        if (needZero && unitIndex == 0) {
                            groupStr.insert(0, "零");
                        }
                        groupStr.insert(0, cnNumbers[digit] + cnUnits[unitIndex]);
                        needZero = false;
                    } else {
                        needZero = true;
                    }
                    group /= 10;
                    unitIndex++;
                }
                result.insert(0, groupStr.toString() + cnGroupUnits[groupIndex]);
            }
            integerPart /= 10000;
            groupIndex++;
        }

        result.append("元整");
        return result.toString();
    }
}

