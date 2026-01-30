package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统公告DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnouncementDTO extends BaseDTO {
  /** ID. */
  private Long id;

  /** 标题. */
  private String title;

  /** 内容. */
  private String content;

  /** 类型. */
  private String type;

  /** 类型名称. */
  private String typeName;

  /** 优先级. */
  private Integer priority;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 发布时间. */
  private LocalDateTime publishTime;

  /** 过期时间. */
  private LocalDateTime expireTime;

  /** 是否置顶. */
  private Boolean isTop;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
