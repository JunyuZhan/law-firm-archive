package com.lawfirm.application.system.command;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建公告命令
 */
@Data
public class CreateAnnouncementCommand {
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private LocalDateTime expireTime;
    private Boolean isTop;
}
