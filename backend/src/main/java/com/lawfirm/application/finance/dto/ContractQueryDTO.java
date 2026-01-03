package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 合同查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractQueryDTO extends PageQuery {

    /**
     * 合同编号（模糊）
     */
    private String contractNo;

    /**
     * 合同名称（模糊）
     */
    private String name;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 状态
     */
    private String status;

    /**
     * 签约开始日期
     */
    private LocalDate signDateFrom;

    /**
     * 签约结束日期
     */
    private LocalDate signDateTo;

    /**
     * 签约人ID
     */
    private Long signerId;
}

