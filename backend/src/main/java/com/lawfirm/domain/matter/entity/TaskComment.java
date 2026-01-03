package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 任务评论实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_comment", autoResultMap = true)
public class TaskComment extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 附件列表（JSON格式，存储文件URL列表）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;

    /**
     * @提醒的用户ID列表（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> mentionedUserIds;
}

