package com.lawfirm.application.admin.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 采购明细DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemDTO {

  /** 明细ID */
  private Long id;

  /** 采购申请ID */
  private Long requestId;

  /** 物品名称 */
  private String itemName;

  /** 规格 */
  private String specification;

  /** 单位 */
  private String unit;

  /** 数量 */
  private Integer quantity;

  /** 预估单价 */
  private BigDecimal estimatedPrice;

  /** 实际单价 */
  private BigDecimal actualPrice;

  /** 预估金额 */
  private BigDecimal estimatedAmount;

  /** 实际金额 */
  private BigDecimal actualAmount;

  /** 已入库数量 */
  private Integer receivedQuantity;

  /** 备注 */
  private String remarks;

  /** 是否已全部入库 */
  private Boolean fullyReceived;
}
