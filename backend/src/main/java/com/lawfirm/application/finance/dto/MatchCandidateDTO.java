package com.lawfirm.application.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 匹配候选项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCandidateDTO {

    /**
     * 收费记录ID
     */
    private Long feeId;

    /**
     * 收费编号
     */
    private String feeNo;

    /**
     * 收费项目名称
     */
    private String feeName;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 项目名称
     */
    private String matterName;

    /**
     * 项目编号
     */
    private String matterNo;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 客户名称
     */
    private String clientName;

    /**
     * 应收金额
     */
    private BigDecimal expectedAmount;

    /**
     * 待收金额
     */
    private BigDecimal unpaidAmount;

    /**
     * 计划收款日期
     */
    private LocalDate plannedDate;

    /**
     * 匹配度分数（0-1）
     */
    private Double score;

    /**
     * 匹配原因列表
     */
    private List<String> matchReasons;
}
