package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 会议室DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingRoomDTO extends BaseDTO {
  /** 会议室ID */
  private Long id;

  /** 会议室名称 */
  private String name;

  /** 会议室编号 */
  private String code;

  /** 位置 */
  private String location;

  /** 容量（人数） */
  private Integer capacity;

  /** 设备 */
  private String equipment;

  /** 描述 */
  private String description;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序 */
  private Integer sortOrder;
}
