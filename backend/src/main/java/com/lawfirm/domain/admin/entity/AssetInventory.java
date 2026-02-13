package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 资产盘点实体（M8-033）. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("asset_inventory")
public class AssetInventory extends BaseEntity {

  /** 盘点编号. */
  private String inventoryNo;

  /** 盘点日期. */
  private LocalDate inventoryDate;

  /** 盘点类型：FULL-全盘, PARTIAL-抽盘. */
  private String inventoryType;

  /** 盘点部门ID. */
  private Long departmentId;

  /** 盘点位置. */
  private String location;

  /** 状态：IN_PROGRESS-进行中, COMPLETED-已完成. */
  private String status;

  /** 应盘数量. */
  private Integer totalCount;

  /** 实盘数量. */
  private Integer actualCount;

  /** 盘盈数量. */
  private Integer surplusCount;

  /** 盘亏数量. */
  private Integer shortageCount;

  /** 备注. */
  private String remark;

  /** 类型：全盘. */
  public static final String TYPE_FULL = "FULL";

  /** 类型：抽盘. */
  public static final String TYPE_PARTIAL = "PARTIAL";

  /** 状态：进行中. */
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  /** 状态：已完成. */
  public static final String STATUS_COMPLETED = "COMPLETED";
}
