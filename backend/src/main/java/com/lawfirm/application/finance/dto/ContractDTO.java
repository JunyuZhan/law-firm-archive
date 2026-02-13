package com.lawfirm.application.finance.dto;

import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 合同 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDTO extends BaseDTO {

  /** 合同编号 */
  private String contractNo;

  /** 合同名称 */
  private String name;

  /** 使用的模板ID */
  private Long templateId;

  /** 合同内容（基于模板生成） */
  private String content;

  /** 合同类型 */
  private String contractType;

  /** 合同类型名称 */
  private String contractTypeName;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 收费方式 */
  private String feeType;

  /** 收费方式名称 */
  private String feeTypeName;

  /** 合同总金额 */
  private BigDecimal totalAmount;

  /** 已付金额 */
  private BigDecimal paidAmount;

  /** 未付金额 */
  private BigDecimal unpaidAmount;

  /** 币种 */
  private String currency;

  /** 签约日期 */
  private LocalDate signDate;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 到期日期 */
  private LocalDate expiryDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 签约人ID */
  private Long signerId;

  /** 签约人名称 */
  private String signerName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 创建人ID，用于前端判断操作权限 */
  private Long createdBy;

  /** 付款条件 */
  private String paymentTerms;

  /** 文件URL */
  private String fileUrl;

  /** 备注 */
  private String remark;

  // ========== 扩展字段（合同模块完善）==========

  /** 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政 */
  private String caseType;

  /** 案件类型名称 */
  private String caseTypeName;

  /** 案由代码 */
  private String causeOfAction;

  /** 案由名称 */
  private String causeOfActionName;

  /** 审理阶段 */
  private String trialStage;

  /** 审理阶段名称 */
  private String trialStageName;

  /** 标的金额 */
  private BigDecimal claimAmount;

  /** 管辖法院 */
  private String jurisdictionCourt;

  /** 对方当事人 */
  private String opposingParty;

  /** 利冲审查状态 */
  private String conflictCheckStatus;

  /** 利冲审查状态名称 */
  private String conflictCheckStatusName;

  /** 归档状态 */
  private String archiveStatus;

  /** 归档状态名称 */
  private String archiveStatusName;

  /** 预支差旅费 */
  private BigDecimal advanceTravelFee;

  /** 风险代理比例 */
  private BigDecimal riskRatio;

  /** 印章使用记录 */
  private String sealRecord;

  /** 付款计划列表 */
  private List<ContractPaymentScheduleDTO> paymentSchedules;

  /** 参与人列表 */
  private List<ContractParticipantDTO> participants;

  /** 关联的审批单列表（最新的在前） */
  private List<ApprovalDTO> approvals;

  /** 当前待审批的审批单（如果有） */
  private ApprovalDTO currentApproval;

  // ========== 提成分配方案 ==========

  /** 提成规则ID */
  private Long commissionRuleId;

  /** 律所比例(%) */
  private BigDecimal firmRate;

  /** 主办律师比例(%) */
  private BigDecimal leadLawyerRate;

  /** 协办律师比例(%) */
  private BigDecimal assistLawyerRate;

  /** 辅助人员比例(%) */
  private BigDecimal supportStaffRate;

  /** 案源人比例(%) */
  private BigDecimal originatorRate;

  /** 案情摘要（用于审批表） */
  private String caseSummary;
}
