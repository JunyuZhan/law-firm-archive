package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 资产盘点DTO（M8-033） */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInventoryDTO extends BaseDTO {
  /** 盘点ID */
  private Long id;

  /** 盘点编号 */
  private String inventoryNo;

  /** 盘点日期 */
  private LocalDate inventoryDate;

  /** 盘点类型 */
  private String inventoryType;

  /** 盘点类型名称 */
  private String inventoryTypeName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 盘点地点 */
  private String location;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 应盘数量 */
  private Integer totalCount;

  /** 实盘数量 */
  private Integer actualCount;

  /** 盘盈数量 */
  private Integer surplusCount;

  /** 盘亏数量 */
  private Integer shortageCount;

  /** 备注 */
  private String remark;

  /** 盘点明细列表 */
  private List<AssetInventoryDetailDTO> details;
}
