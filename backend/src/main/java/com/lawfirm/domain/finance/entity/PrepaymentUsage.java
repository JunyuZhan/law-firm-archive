package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 预收款核销记录实体
 *
 * <p>记录预收款的每次核销使用情况
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_prepayment_usage")
public class PrepaymentUsage extends BaseEntity {

  /** 预收款ID */
  private Long prepaymentId;

  /** 收费记录ID（核销到哪个收费项） */
  private Long feeId;

  /** 项目ID */
  private Long matterId;

  /** 核销金额 */
  private BigDecimal amount;

  /** 核销时间 */
  private LocalDateTime usageTime;

  /** 操作人ID */
  private Long operatorId;

  /** 备注 */
  private String remark;
}
