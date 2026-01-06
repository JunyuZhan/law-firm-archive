package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目-客户关联 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterClientDTO extends BaseDTO {

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 客户名称
     */
    private String clientName;

    /**
     * 客户类型
     */
    private String clientType;

    /**
     * 客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人
     */
    private String clientRole;

    /**
     * 客户角色名称
     */
    private String clientRoleName;

    /**
     * 是否主要客户
     */
    private Boolean isPrimary;

    /**
     * 获取客户角色名称
     */
    public static String getClientRoleName(String clientRole) {
        if (clientRole == null) return "";
        return switch (clientRole) {
            case "PLAINTIFF" -> "原告";
            case "DEFENDANT" -> "被告";
            case "THIRD_PARTY" -> "第三人";
            case "APPLICANT" -> "申请人";
            case "RESPONDENT" -> "被申请人";
            default -> clientRole;
        };
    }
}

