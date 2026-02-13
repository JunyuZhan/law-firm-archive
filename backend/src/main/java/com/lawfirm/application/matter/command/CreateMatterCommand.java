package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 创建案件命令. */
@Data
public class CreateMatterCommand {

  /** 案件名称. */
  @NotBlank(message = "案件名称不能为空")
  private String name;

  /** 案件类型. */
  @NotBlank(message = "案件类型不能为空")
  private String matterType;

  /** 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等. */
  private String caseType;

  /** 代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行等. */
  private String litigationStage;

  /** 案由代码. */
  private String causeOfAction;

  /** 业务类型. */
  private String businessType;

  /** 主要客户ID（向后兼容）. */
  @NotNull(message = "客户不能为空")
  private Long clientId;

  /** 客户列表（支持多客户）. */
  private List<ClientCommand> clients;

  /** 对方当事人. */
  private String opposingParty;

  /** 客户命令. */
  @Data
  public static class ClientCommand {
    /** 客户ID. */
    private Long clientId;

    /** 客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人. */
    private String clientRole;

    /** 是否主要客户. */
    private Boolean isPrimary;
  }

  /** 对方律师信息. */
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

  /** 发起人ID. */
  private Long originatorId;

  /** 主办律师ID. */
  private Long leadLawyerId;

  /** 部门ID. */
  private Long departmentId;

  /** 费用类型. */
  private String feeType;

  /** 预估费用. */
  private BigDecimal estimatedFee;

  /** 立案日期. */
  private LocalDate filingDate;

  /** 预计结案日期. */
  private LocalDate expectedClosingDate;

  /** 标的额. */
  private BigDecimal claimAmount;

  /** 合同ID. */
  private Long contractId;

  /** 备注. */
  private String remark;

  /** 团队成员. */
  private List<ParticipantCommand> participants;

  /** 参与者命令. */
  @Data
  public static class ParticipantCommand {
    /** 用户ID. */
    private Long userId;

    /** 角色. */
    private String role;

    /** 提成比例. */
    private BigDecimal commissionRate;

    /** 是否案源人. */
    private Boolean isOriginator;
  }
}
