package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 印章DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 印章编号. */
  private String sealNo;

  /** 印章名称. */
  private String name;

  /** 印章类型. */
  private String sealType;

  /** 印章类型名称. */
  private String sealTypeName;

  /** 保管人ID. */
  private Long keeperId;

  /** 保管人姓名. */
  private String keeperName;

  /** 印章图片URL. */
  private String imageUrl;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 描述. */
  private String description;

  /** 使用次数（统计）. */
  private Integer usageCount;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
