package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 国家赔偿信息 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class StateCompensationDTO extends BaseDTO {

  /** 案件ID. */
  private Long matterId;

  /** 案件编号. */
  private String matterNo;

  /** 案件名称. */
  private String matterName;

  // ========== 赔偿义务机关 ==========

  /** 赔偿义务机关名称. */
  private String obligorOrgName;

  /** 赔偿义务机关类型. */
  private String obligorOrgType;

  /** 赔偿义务机关类型名称. */
  private String obligorOrgTypeName;

  // ========== 致损行为 ==========

  /** 案件来源. */
  private String caseSource;

  /** 案件来源名称. */
  private String caseSourceName;

  /** 致损行为描述. */
  private String damageDescription;

  // ========== 刑事赔偿特有字段 ==========

  /** 刑事案件是否终止. */
  private Boolean criminalCaseTerminated;

  /** 刑事案件编号. */
  private String criminalCaseNo;

  /** 赔偿委员会. */
  private String compensationCommittee;

  // ========== 程序日期 ==========

  /** 申请日期. */
  private LocalDate applicationDate;

  /** 受理日期. */
  private LocalDate acceptanceDate;

  /** 决定日期. */
  private LocalDate decisionDate;

  /** 复议申请日期. */
  private LocalDate reconsiderationDate;

  /** 复议决定日期. */
  private LocalDate reconsiderationDecisionDate;

  /** 委员会申请日期. */
  private LocalDate committeeAppDate;

  /** 委员会决定日期. */
  private LocalDate committeeDecisionDate;

  /** 行政诉讼立案日期. */
  private LocalDate adminLitigationFilingDate;

  /** 行政诉讼法院名称. */
  private String adminLitigationCourtName;

  // ========== 赔偿请求 ==========

  /** 请求金额. */
  private BigDecimal claimAmount;

  /** 赔偿项目. */
  private String compensationItems;

  // ========== 决定结果 ==========

  /** 决定结果. */
  private String decisionResult;

  /** 决定结果名称. */
  private String decisionResultName;

  /** 批准金额. */
  private BigDecimal approvedAmount;

  /** 支付状态. */
  private String paymentStatus;

  /** 支付状态名称. */
  private String paymentStatusName;

  /** 支付日期. */
  private LocalDate paymentDate;

  /** 备注. */
  private String remark;
}
