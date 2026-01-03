package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新客户命令
 */
@Data
public class UpdateClientCommand {

    @NotNull(message = "客户ID不能为空")
    private Long id;

    private String name;
    private String clientType;
    private String creditCode;
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
    private String status;
    private Long originatorId;
    private Long responsibleLawyerId;
    private LocalDate firstCooperationDate;
    private String remark;
}

