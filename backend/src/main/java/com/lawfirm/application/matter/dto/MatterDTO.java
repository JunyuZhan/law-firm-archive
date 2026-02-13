package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案件 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterDTO extends BaseDTO {

  /** 案件编号. */
  private String matterNo;

  /** 案件名称. */
  private String name;

  /** 案件类型. */
  private String matterType;

  /** 案件类型名称. */
  private String matterTypeName;

  /** 业务类型. */
  private String businessType;

  /** 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政. */
  private String caseType;

  /** 案件类型名称. */
  private String caseTypeName;

  /** 代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行等. */
  private String litigationStage;

  /** 代理阶段名称. */
  private String litigationStageName;

  /** 案由代码. */
  private String causeOfAction;

  /** 案由名称. */
  private String causeOfActionName;

  /** 主要客户ID（向后兼容）. */
  private Long clientId;

  /** 主要客户名称. */
  private String clientName;

  /** 客户列表（多客户支持）. */
  private List<MatterClientDTO> clients;

  /** 对方当事人. */
  private String opposingParty;

  /** 对方律师姓名. */
  private String opposingLawyerName;

  /** 对方律师执业证号. */
  private String opposingLawyerLicenseNo;

  /** 对方律师所在律所. */
  private String opposingLawyerFirm;

  /** 对方律师电话. */
  private String opposingLawyerPhone;

  /** 对方律师邮箱. */
  private String opposingLawyerEmail;

  /** 案件描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 案源人ID. */
  private Long originatorId;

  /** 案源人名称. */
  private String originatorName;

  /** 主办律师ID. */
  private Long leadLawyerId;

  /** 主办律师名称. */
  private String leadLawyerName;

  /** 部门ID. */
  private Long departmentId;

  /** 部门名称. */
  private String departmentName;

  /** 收费方式. */
  private String feeType;

  /** 收费方式名称. */
  private String feeTypeName;

  /** 预估费用. */
  private BigDecimal estimatedFee;

  /** 实际费用. */
  private BigDecimal actualFee;

  /** 立案日期. */
  private LocalDate filingDate;

  /** 预计结案日期. */
  private LocalDate expectedClosingDate;

  /** 实际结案日期. */
  private LocalDate actualClosingDate;

  /** 标的金额. */
  private BigDecimal claimAmount;

  /** 案件结果 */
  private String outcome;

  /** 合同ID */
  private Long contractId;

  /** 合同编号 */
  private String contractNo;

  /** 合同金额 */
  private BigDecimal contractAmount;

  /** 备注 */
  private String remark;

  /** 利冲状态 */
  private String conflictStatus;

  /** 团队成员列表 */
  private List<MatterParticipantDTO> participants;
}
