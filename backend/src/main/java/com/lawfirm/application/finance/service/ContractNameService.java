package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.domain.client.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 合同名称生成服务
 *
 * @author system
 */
@Service
public class ContractNameService {

  /** 合同应用服务（延迟加载以打破循环依赖） */
  @Lazy @Autowired private ContractAppService contractAppService;

  /**
   * 生成合同名称 优先根据模板类型（contractType），其次根据案件类型（caseType）使用不同的命名规则： 1.
   * 刑事辩护模板（CRIMINAL_DEFENSE）：被告人/犯罪嫌疑人姓名 + "涉嫌" + 罪名（例如："任七涉嫌盗窃罪"） 2. 民事代理模板（CIVIL_PROXY）：客户名 + "与"
   * + 对方当事人 + 案由名称（例如："张三与李四离婚纠纷"） 3. 行政代理模板（ADMINISTRATIVE_PROXY）：客户名 + "与" + 对方当事人 +
   * 案由名称（例如："王五与xx政府行政协议纠纷"） 4. 其他情况：客户名 + 合同类型名称
   *
   * @param command 创建合同命令
   * @param client 客户实体
   * @return 合同名称
   */
  public String generateContractName(final CreateContractCommand command, final Client client) {
    String clientName = client.getName();
    String contractType = command.getContractType(); // 合同类型 = 模板类型
    String caseType = command.getCaseType();

    // 优先根据模板类型判断命名规则
    if ("CRIMINAL_DEFENSE".equals(contractType)) {
      String result = generateCriminalContractName(command, clientName);
      if (result != null) {
        return result;
      }
      // 如果都没有，使用默认格式
      return clientName + "涉嫌刑事案件";
    }

    if ("CIVIL_PROXY".equals(contractType) || "ADMINISTRATIVE_PROXY".equals(contractType)) {
      // 民事代理或行政代理模板：客户名 + "与" + 对方当事人 + 案由名称
      // 确定案由类型（民事或行政）
      String effectiveCaseType = "CIVIL_PROXY".equals(contractType) ? "CIVIL" : "ADMINISTRATIVE";
      // 如果命令中指定了案件类型，优先使用命令中的
      if (caseType != null && ("CIVIL".equals(caseType) || "ADMINISTRATIVE".equals(caseType))) {
        effectiveCaseType = caseType;
      }

      String result = generateCivilOrAdminContractName(command, clientName, effectiveCaseType);
      if (result != null) {
        return result;
      }
      // 如果都没有，使用默认格式
      String typeName = contractAppService.getContractTypeName(contractType);
      return clientName + typeName;
    }

    // 如果模板类型无法确定命名规则，回退到根据案件类型判断
    if ("CRIMINAL".equals(caseType)) {
      String result = generateCriminalContractName(command, clientName);
      if (result != null) {
        return result;
      }
    }

    if ("CIVIL".equals(caseType) || "ADMINISTRATIVE".equals(caseType)) {
      String result = generateCivilOrAdminContractName(command, clientName, caseType);
      if (result != null) {
        return result;
      }
    }

    // 最终回退规则：客户名称 + 合同类型名称
    String typeName = contractAppService.getContractTypeName(contractType);
    return clientName + (typeName != null ? typeName : "");
  }

  /**
   * 生成刑事合同名称
   *
   * @param command 创建合同命令
   * @param clientName 客户名称
   * @return 合同名称
   */
  private String generateCriminalContractName(
      final CreateContractCommand command, final String clientName) {
    String defendantName = command.getDefendantName();
    String criminalCharge = command.getCriminalCharge();

    if (defendantName != null
        && !defendantName.isBlank()
        && criminalCharge != null
        && !criminalCharge.isBlank()) {
      return defendantName + "涉嫌" + criminalCharge;
    } else if (defendantName != null && !defendantName.isBlank()) {
      return defendantName + "涉嫌刑事案件";
    } else if (criminalCharge != null && !criminalCharge.isBlank()) {
      return clientName + "涉嫌" + criminalCharge;
    }
    return null;
  }

  /**
   * 生成民事或行政合同名称
   *
   * @param command 创建合同命令
   * @param clientName 客户名称
   * @param caseType 案件类型
   * @return 合同名称
   */
  private String generateCivilOrAdminContractName(
      final CreateContractCommand command, final String clientName, final String caseType) {
    String opposingParty =
        command.getOpposingParty() != null && !command.getOpposingParty().isBlank()
            ? command.getOpposingParty()
            : "";
    String causeName =
        contractAppService.getCauseOfActionName(command.getCauseOfAction(), caseType);

    if (!opposingParty.isEmpty() && !causeName.isEmpty()) {
      // 完整格式：客户名 + "与" + 对方名 + 案由名称
      StringBuilder nameBuilder = new StringBuilder(clientName);
      nameBuilder.append("与").append(opposingParty);
      nameBuilder.append(causeName);
      // 如果案由名称中不包含"纠纷"、"争议"等后缀，则自动添加"纠纷"
      if (!causeName.contains("纠纷")
          && !causeName.contains("争议")
          && !causeName.contains("案")
          && !causeName.contains("事项")) {
        nameBuilder.append("纠纷");
      }
      return nameBuilder.toString();
    } else if (!causeName.isEmpty()) {
      // 只有案由，没有对方当事人：客户名 + 案由名称
      StringBuilder nameBuilder = new StringBuilder(clientName);
      nameBuilder.append(causeName);
      // 如果案由名称中不包含"纠纷"、"争议"等后缀，则自动添加"纠纷"
      if (!causeName.contains("纠纷")
          && !causeName.contains("争议")
          && !causeName.contains("案")
          && !causeName.contains("事项")) {
        nameBuilder.append("纠纷");
      }
      return nameBuilder.toString();
    } else if (!opposingParty.isEmpty()) {
      // 只有对方当事人，没有案由：客户名 + "与" + 对方当事人
      return clientName + "与" + opposingParty;
    }
    return null;
  }
}
