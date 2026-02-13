package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 资产盘点明细实体（M8-033）. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("asset_inventory_detail")
public class AssetInventoryDetail extends BaseEntity {

  /** 盘点ID. */
  private Long inventoryId;

  /** 资产ID. */
  private Long assetId;

  /** 预期状态. */
  private String expectedStatus;

  /** 实际状态. */
  private String actualStatus;

  /** 预期位置. */
  private String expectedLocation;

  /** 实际位置. */
  private String actualLocation;

  /** 预期使用人ID. */
  private Long expectedUserId;

  /** 实际使用人ID. */
  private Long actualUserId;

  /** 差异类型：NORMAL-正常, SURPLUS-盘盈, SHORTAGE-盘亏, LOCATION-位置不符, STATUS-状态不符. */
  private String discrepancyType;

  /** 差异说明. */
  private String discrepancyDesc;

  /** 备注. */
  private String remark;

  /** 差异类型：正常. */
  public static final String DISCREPANCY_NORMAL = "NORMAL";

  /** 差异类型：盘盈. */
  public static final String DISCREPANCY_SURPLUS = "SURPLUS";

  /** 差异类型：盘亏. */
  public static final String DISCREPANCY_SHORTAGE = "SHORTAGE";

  /** 差异类型：位置不符. */
  public static final String DISCREPANCY_LOCATION = "LOCATION";

  /** 差异类型：状态不符. */
  public static final String DISCREPANCY_STATUS = "STATUS";
}
