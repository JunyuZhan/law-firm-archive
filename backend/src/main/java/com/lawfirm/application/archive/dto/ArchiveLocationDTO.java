package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 档案库位DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveLocationDTO extends BaseDTO {

  /** 库位编码 */
  private String locationCode;

  /** 库位名称 */
  private String locationName;

  /** 房间号 */
  private String room;

  /** 柜号 */
  private String cabinet;

  /** 层号 */
  private String shelf;

  /** 位置号 */
  private String position;

  /** 总容量 */
  private Integer totalCapacity;

  /** 已用容量 */
  private Integer usedCapacity;

  /** 可用容量 */
  private Integer availableCapacity;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 备注 */
  private String remarks;
}
