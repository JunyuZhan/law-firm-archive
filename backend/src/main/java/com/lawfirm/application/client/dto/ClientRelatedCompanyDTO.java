package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户关联企业 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientRelatedCompanyDTO extends BaseDTO {

    private Long clientId;
    private String relatedCompanyName;
    private String relatedCompanyType;
    private String relatedCompanyTypeName;
    private String creditCode;
    private String registeredAddress;
    private String legalRepresentative;
    private String relationshipDescription;
    private String remark;
}

