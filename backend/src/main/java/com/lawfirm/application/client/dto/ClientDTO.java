package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 客户 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientDTO extends BaseDTO {

    private String clientNo;
    private String name;
    private String clientType;
    private String clientTypeName;
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
    private String levelName;
    private String category;
    private String categoryName;
    private String status;
    private String statusName;
    private Long originatorId;
    private String originatorName;
    private Long responsibleLawyerId;
    private String responsibleLawyerName;
    private LocalDate firstCooperationDate;
    private String remark;
}

