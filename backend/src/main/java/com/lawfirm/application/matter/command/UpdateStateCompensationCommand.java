package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新国家赔偿信息命令. */
@Data
public class UpdateStateCompensationCommand {

  /** 国家赔偿信息ID. */
  @NotNull(message = "ID不能为空")
  private Long id;

  // ========== 赔偿义务机关 ==========

  /** 赔偿义务机关名称. */
  private String obligorOrgName;

  /** 赔偿义务机关类型. */
  private String obligorOrgType;

  // ========== 致损行为 ==========

  /** 案件来源. */
  private String caseSource;

  /** 致损行为描述. */
  private String damageDescription;

  // ========== 刑事赔偿特有字段 ==========

  /** 刑事诉讼是否已终结. */
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

  /** 赔偿委员会申请日期. */
  private LocalDate committeeAppDate;

  /** 赔偿委员会决定日期. */
  private LocalDate committeeDecisionDate;

  /** 行政诉讼立案日期. */
  private LocalDate adminLitigationFilingDate;

  /** 行政诉讼法院名称. */
  private String adminLitigationCourtName;

  // ========== 赔偿请求 ==========

  /** 请求赔偿金额. */
  private BigDecimal claimAmount;

  /** 赔偿项目. */
  private String compensationItems;

  // ========== 决定结果 ==========

  /** 决定结果. */
  private String decisionResult;

  /** 批准赔偿金额. */
  private BigDecimal approvedAmount;

  /** 支付状态. */
  private String paymentStatus;

  /** 支付日期. */
  private LocalDate paymentDate;

  /** 备注. */
  private String remark;
}
