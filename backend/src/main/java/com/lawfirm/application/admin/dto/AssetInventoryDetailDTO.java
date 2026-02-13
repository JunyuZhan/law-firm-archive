package com.lawfirm.application.admin.dto;

import lombok.Data;

/** 资产盘点明细DTO（M8-033） */
@Data
public class AssetInventoryDetailDTO {
  /** 明细ID */
  private Long id;

  /** 盘点ID */
  private Long inventoryId;

  /** 资产ID */
  private Long assetId;

  /** 资产编号 */
  private String assetNo;

  /** 资产名称 */
  private String assetName;

  /** 预期状态 */
  private String expectedStatus;

  /** 实际状态 */
  private String actualStatus;

  /** 预期位置 */
  private String expectedLocation;

  /** 实际位置 */
  private String actualLocation;

  /** 预期使用人ID */
  private Long expectedUserId;

  /** 预期使用人名称 */
  private String expectedUserName;

  /** 实际使用人ID */
  private Long actualUserId;

  /** 实际使用人名称 */
  private String actualUserName;

  /** 差异类型 */
  private String discrepancyType;

  /** 差异类型名称 */
  private String discrepancyTypeName;

  /** 差异描述 */
  private String discrepancyDesc;

  /** 备注 */
  private String remark;
}
