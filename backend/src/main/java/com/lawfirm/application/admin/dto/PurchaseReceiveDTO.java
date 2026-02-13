package com.lawfirm.application.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 采购入库DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReceiveDTO {

  /** 入库ID */
  private Long id;

  /** 入库编号 */
  private String receiveNo;

  /** 采购申请ID */
  private Long requestId;

  /** 采购申请编号 */
  private String requestNo;

  /** 采购明细ID */
  private Long itemId;

  /** 物品名称 */
  private String itemName;

  /** 入库数量 */
  private Integer quantity;

  /** 入库日期 */
  private LocalDate receiveDate;

  /** 接收人ID */
  private Long receiverId;

  /** 接收人名称 */
  private String receiverName;

  /** 存放位置 */
  private String location;

  /** 是否转为资产 */
  private Boolean convertToAsset;

  /** 资产ID */
  private Long assetId;

  /** 资产编号 */
  private String assetNo;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
