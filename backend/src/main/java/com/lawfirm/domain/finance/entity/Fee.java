package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 收费记录实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_fee")
public class Fee extends BaseEntity {

  /** 收费编号 */
  private String feeNo;

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 收费类型：LAWYER_FEE-律师费, COURT_FEE-诉讼费, TRAVEL-差旅费, OTHER-其他 */
  private String feeType;

  /** 收费项目名称 */
  private String feeName;

  /** 应收金额 */
  private BigDecimal amount;

  /** 已收金额 */
  private BigDecimal paidAmount;

  /** 币种 */
  private String currency;

  /** 计划收款日期 */
  private LocalDate plannedDate;

  /** 实际收款日期 */
  private LocalDate actualDate;

  /** 状态：PENDING-待收款, PARTIAL-部分收款, PAID-已收款, CANCELLED-已取消 */
  private String status;

  /** 负责人ID */
  private Long responsibleId;

  /** 备注 */
  private String remark;
}
