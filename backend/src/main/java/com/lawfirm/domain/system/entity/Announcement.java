package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 */
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

    public static final String TYPE_NOTICE = "NOTICE";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_WARNING = "WARNING";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_EXPIRED = "EXPIRED";
}
