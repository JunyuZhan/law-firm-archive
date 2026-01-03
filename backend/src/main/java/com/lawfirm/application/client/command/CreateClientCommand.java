package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建客户命令
 */
@Data
public class CreateClientCommand {

    @NotBlank(message = "客户名称不能为空")
    private String name;

    @NotBlank(message = "客户类型不能为空")
    private String clientType;

    /**
     * 统一社会信用代码（企业客户必填）
     */
    private String creditCode;

    /**
     * 身份证号（个人客户必填）
     */
    private String idCard;

    private String legalRepresentative;
    private String registeredAddress;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String source;
    private String level;
    private String category;
    private Long originatorId;
    private Long responsibleLawyerId;
    private LocalDate firstCooperationDate;
    private String remark;
}

