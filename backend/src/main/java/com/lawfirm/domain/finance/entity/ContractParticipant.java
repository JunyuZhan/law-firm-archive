package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 合同参与人实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("contract_participant")
public class ContractParticipant extends BaseEntity {

  /** 角色常量 */
  /** 承办律师角色 */
  public static final String ROLE_LEAD = "LEAD";

  /** 协办律师角色 */
  public static final String ROLE_CO_COUNSEL = "CO_COUNSEL";

  /** 案源人角色 */
  public static final String ROLE_ORIGINATOR = "ORIGINATOR";

  /** 律师助理角色 */
  public static final String ROLE_PARALEGAL = "PARALEGAL";

  /** 合同ID */
  private Long contractId;

  /** 用户ID */
  private Long userId;

  /** 角色：LEAD-承办律师, CO_COUNSEL-协办律师, ORIGINATOR-案源人, PARALEGAL-律师助理 */
  private String role;

  /** 提成比例（百分比） */
  private BigDecimal commissionRate;

  /** 备注 */
  private String remark;

  /**
   * 判断是否为案源人。
   *
   * @return 如果是案源人返回true，否则返回false
   */
  public boolean isOriginator() {
    return ROLE_ORIGINATOR.equals(this.role);
  }

  /**
   * 判断是否为承办律师。
   *
   * @return 如果是承办律师返回true，否则返回false
   */
  public boolean isLeadLawyer() {
    return ROLE_LEAD.equals(this.role);
  }

  /**
   * 判断是否为协办律师。
   *
   * @return 如果是协办律师返回true，否则返回false
   */
  public boolean isCoCounsel() {
    return ROLE_CO_COUNSEL.equals(this.role);
  }

  /**
   * 判断是否为办案人员（承办或协办）。
   *
   * @return 如果是办案人员返回true，否则返回false
   */
  public boolean isCaseHandler() {
    return ROLE_LEAD.equals(this.role) || ROLE_CO_COUNSEL.equals(this.role);
  }
}
