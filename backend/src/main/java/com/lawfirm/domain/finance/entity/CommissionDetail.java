package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 提成分配明细实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_commission_detail")
public class CommissionDetail extends BaseEntity {

  /** 提成记录ID */
  private Long commissionId;

  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 案件角色 */
  private String roleInMatter;

  /** 分配比例 */
  private BigDecimal allocationRate;

  /** 提成金额 */
  private BigDecimal commissionAmount;

  /** 税费 */
  private BigDecimal taxAmount;

  /** 净提成 */
  private BigDecimal netAmount;
}
