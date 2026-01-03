package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 劳动合同查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractQueryDTO extends PageQuery {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同状态
     */
    private String status;
}

