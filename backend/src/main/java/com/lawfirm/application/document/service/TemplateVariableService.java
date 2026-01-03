package com.lawfirm.application.document.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

    /**
     * 收集案件相关的所有变量值
     */
    public Map<String, Object> collectVariables(Long matterId) {
        Map<String, Object> variables = new HashMap<>();

        // 获取案件信息
        Matter matter = matterRepository.getByIdOrThrow(matterId, "案件不存在");
        variables.put("matter.name", matter.getName());
        variables.put("matter.no", matter.getMatterNo());
        variables.put("matter.status", matter.getStatus());
        variables.put("matter.businessType", matter.getBusinessType());
        if (matter.getFilingDate() != null) {
            variables.put("matter.filingDate", matter.getFilingDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }
        if (matter.getExpectedClosingDate() != null) {
            variables.put("matter.expectedClosingDate", matter.getExpectedClosingDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }

        // 获取客户信息
        if (matter.getClientId() != null) {
            Client client = clientRepository.findById(matter.getClientId());
            if (client != null) {
                variables.put("client.name", client.getName());
                variables.put("client.type", client.getClientType());
                variables.put("client.legalPerson", client.getLegalRepresentative());
                variables.put("client.address", client.getRegisteredAddress());
                variables.put("client.phone", client.getContactPhone());
                variables.put("client.email", client.getContactEmail());
                if (client.getCreditCode() != null) {
                    variables.put("client.creditCode", client.getCreditCode());
                }
            }
        }

        // 获取主办律师信息
        if (matter.getLeadLawyerId() != null) {
            User lawyer = userRepository.findById(matter.getLeadLawyerId());
            if (lawyer != null) {
                variables.put("lawyer.name", lawyer.getRealName());
                variables.put("lawyer.phone", lawyer.getPhone() != null ? lawyer.getPhone() : "");
                variables.put("lawyer.email", lawyer.getEmail() != null ? lawyer.getEmail() : "");
                if (lawyer.getLawyerLicenseNo() != null) {
                    variables.put("lawyer.licenseNo", lawyer.getLawyerLicenseNo());
                }
            }
        }

        // 日期变量
        LocalDate today = LocalDate.now();
        variables.put("date.today", today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        variables.put("date.year", String.valueOf(today.getYear()));
        variables.put("date.month", String.valueOf(today.getMonthValue()));
        variables.put("date.day", String.valueOf(today.getDayOfMonth()));

        // 对方律师信息
        if (matter.getOpposingLawyerName() != null) {
            variables.put("opposingLawyer.name", matter.getOpposingLawyerName());
            variables.put("opposingLawyer.licenseNo", matter.getOpposingLawyerLicenseNo());
            variables.put("opposingLawyer.firm", matter.getOpposingLawyerFirm());
            variables.put("opposingLawyer.phone", matter.getOpposingLawyerPhone());
            variables.put("opposingLawyer.email", matter.getOpposingLawyerEmail());
        }

        log.debug("收集模板变量完成: matterId={}, variables={}", matterId, variables.keySet());
        return variables;
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

        return result;
    }
}

