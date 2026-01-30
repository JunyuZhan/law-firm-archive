package com.lawfirm.application.admin.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 创建采购申请命令 */
@Data
public class CreatePurchaseRequestCommand {

  /** 申请标题 */
  private String title;

  /** 采购类型 */
  private String purchaseType;

  /** 期望到货日期 */
  private LocalDate expectedDate;

  /** 采购原因 */
  private String reason;

  /** 供应商ID */
  private Long supplierId;

  /** 备注 */
  private String remarks;

  /** 采购明细 */
  private List<ItemCommand> items;

  /** 采购明细项 */
  @Data
  public static class ItemCommand {
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

    /** 备注 */
    private String remarks;
  }
}
