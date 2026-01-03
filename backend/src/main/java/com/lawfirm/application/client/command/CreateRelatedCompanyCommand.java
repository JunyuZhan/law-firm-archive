package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建关联企业命令
 */
@Data
public class CreateRelatedCompanyCommand {

    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    @NotBlank(message = "关联企业名称不能为空")
    private String relatedCompanyName;

    @NotBlank(message = "关联类型不能为空")
    private String relatedCompanyType; // PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司

    private String creditCode; // 统一社会信用代码
    private String registeredAddress; // 注册地址
    private String legalRepresentative; // 法定代表人
    private String relationshipDescription; // 关联关系描述
    private String remark; // 备注
}

