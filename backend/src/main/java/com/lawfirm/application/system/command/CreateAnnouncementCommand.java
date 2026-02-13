package com.lawfirm.application.system.command;

import java.time.LocalDateTime;
import lombok.Data;

/** 创建公告命令. */
@Data
public class CreateAnnouncementCommand {
  /** 标题. */
  private String title;

  /** 内容. */
  private String content;

  /** 类型. */
  private String type;

  /** 优先级. */
  private Integer priority;

  /** 过期时间. */
  private LocalDateTime expireTime;

  /** 是否置顶. */
  private Boolean isTop;
}
