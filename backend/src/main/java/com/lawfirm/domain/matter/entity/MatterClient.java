package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 项目-客户关联实体（支持多客户） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter_client")
public class MatterClient extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /**
   * 客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人,
   * APPELLANT-上诉人, APPELLEE-被上诉人, EXECUTION_APPLICANT-申请执行人, EXECUTION_RESPONDENT-被执行人,
   * SUSPECT-犯罪嫌疑人, DEFENDANT_CRIMINAL-被告人, RETRIAL_APPLICANT-再审申请人, RETRIAL_RESPONDENT-再审被申请人
   */
  private String clientRole;

  /** 是否主要客户（用于显示和结算） */
  private Boolean isPrimary;
}
