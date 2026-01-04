package com.lawfirm.application.contract.dto;

import lombok.Data;

/**
 * 合同模板DTO
 */
@Data
public class ContractTemplateDTO {
    private Long id;
    private String templateNo;
    private String name;
    private String contractType;
    private String contractTypeName;
    private String feeType;
    private String feeTypeName;
    private String content;
    private String clauses;
    private String description;
    private String status;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;

    /**
     * 获取合同类型名称
     */
    public String getContractTypeName() {
        if (contractType == null) return null;
        switch (contractType) {
            case "SERVICE": return "服务合同";
            case "RETAINER": return "常年法顾";
            case "LITIGATION": return "诉讼代理";
            case "NON_LITIGATION": return "非诉项目";
            default: return contractType;
        }
    }

    /**
     * 获取收费方式名称
     */
    public String getFeeTypeName() {
        if (feeType == null) return null;
        switch (feeType) {
            case "FIXED": return "固定收费";
            case "HOURLY": return "计时收费";
            case "CONTINGENCY": return "风险代理";
            case "MIXED": return "混合收费";
            default: return feeType;
        }
    }
}
