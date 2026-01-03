package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新关联企业命令
 */
@Data
public class UpdateRelatedCompanyCommand {

    @NotNull(message = "关联企业ID不能为空")
    private Long id;

    private String relatedCompanyName;
    private String relatedCompanyType;
    private String creditCode;
    private String registeredAddress;
    private String legalRepresentative;
    private String relationshipDescription;
    private String remark;
}

