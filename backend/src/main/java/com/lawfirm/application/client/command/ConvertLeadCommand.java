package com.lawfirm.application.client.command;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 案源转化命令
 */
@Data
public class ConvertLeadCommand {

    @NotNull(message = "案源ID不能为空")
    private Long leadId;

    /**
     * 是否创建新客户（true-创建新客户, false-关联已有客户）
     */
    private Boolean createNewClient;

    /**
     * 已有客户ID（createNewClient=false时必填）
     */
    private Long clientId;

    /**
     * 新客户信息（createNewClient=true时必填）
     */
    private String clientName;
    private String clientType;
    private String contactPhone;
    private String contactEmail;

    /**
     * 是否同时创建项目
     */
    private Boolean createMatter;

    /**
     * 项目信息（createMatter=true时必填）
     */
    private String matterName;
    private String matterType;
    private String businessType;
    private BigDecimal contractAmount;
    private Long leadLawyerId;
}

