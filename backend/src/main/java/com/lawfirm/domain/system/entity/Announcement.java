package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 系统公告实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_announcement")
public class Announcement extends BaseEntity {

  /** 公告标题 */
  private String title;

  /** 公告内容 */
  private String content;

  /** 类型 */
  private String type;

  /** 优先级 */
  private Integer priority;

  /** 状态 */
  private String status;

  /** 发布时间 */
  private LocalDateTime publishTime;

  /** 过期时间 */
  private LocalDateTime expireTime;

  /** 是否置顶 */
  private Boolean isTop;

  /** 类型：通知 */
  public static final String TYPE_NOTICE = "NOTICE";

  /** 类型：公告 */
  public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";

  /** 类型：警告 */
  public static final String TYPE_WARNING = "WARNING";

  /** 状态：草稿 */
  public static final String STATUS_DRAFT = "DRAFT";

  /** 状态：已发布 */
  public static final String STATUS_PUBLISHED = "PUBLISHED";

  /** 状态：已过期 */
  public static final String STATUS_EXPIRED = "EXPIRED";
}
