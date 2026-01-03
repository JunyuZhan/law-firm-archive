package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户股东信息 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientShareholderDTO extends BaseDTO {

    private Long clientId;
    private String shareholderName;
    private String shareholderType;
    private String shareholderTypeName;
    private String idCard;
    private String creditCode;
    private BigDecimal shareholdingRatio;
    private BigDecimal investmentAmount;
    private LocalDate investmentDate;
    private String position;
    private String remark;
}

