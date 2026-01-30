package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新合同命令 */
@Data
public class UpdateContractCommand {

  /** 合同ID */
  @NotNull(message = "合同ID不能为空")
  private Long id;

  /** 合同名称 */
  private String name;

  /** 合同类型 */
  private String contractType;

  /** 客户ID */
  private Long clientId;

  /** 案件ID */
  private Long matterId;

  /** 收费方式 */
  private String feeType;

  /** 合同总金额 */
  private BigDecimal totalAmount;

  /** 币种 */
  private String currency;

  /** 签约日期 */
  private LocalDate signDate;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 到期日期 */
  private LocalDate expiryDate;

  /** 签约人ID */
  private Long signerId;

  /** 部门ID */
  private Long departmentId;

  /** 付款条件 */
  private String paymentTerms;

  /** 文件URL */
  private String fileUrl;

  /** 备注 */
  private String remark;

  // ========== 扩展字段（合同模块完善）==========

  /** 案件类型 */
  private String caseType;

  /** 案由代码 */
  private String causeOfAction;

  /** 审理阶段 */
  private String trialStage;

  /** 标的金额 */
  private BigDecimal claimAmount;

  /** 管辖法院 */
  private String jurisdictionCourt;

  /** 对方当事人 */
  private String opposingParty;

  /** 利冲审查状态 */
  private String conflictCheckStatus;

  /** 归档状态 */
  private String archiveStatus;

  /** 预支差旅费 */
  private BigDecimal advanceTravelFee;

  /** 风险代理比例（0-100） */
  private BigDecimal riskRatio;

  /** 印章使用记录 */
  private String sealRecord;

  // ========== 提成分配方案 ==========

  /** 提成规则ID（选择的预设方案） */
  private Long commissionRuleId;

  /** 律所比例（%） */
  private BigDecimal firmRate;

  /** 主办律师比例（%） */
  private BigDecimal leadLawyerRate;

  /** 协办律师比例（%） */
  private BigDecimal assistLawyerRate;

  /** 辅助人员比例（%） */
  private BigDecimal supportStaffRate;

  /** 案源人比例（%） */
  private BigDecimal originatorRate;

  /** 案情摘要（用于审批表） */
  private String caseSummary;
}
