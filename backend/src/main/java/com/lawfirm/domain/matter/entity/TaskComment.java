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
     * 附件列表（JSONB格式，标准化存储结构）
     * 格式：[{"bucket_name":"law-firm","storage_path":"matters/M_{matterId}/{YYYY-MM}/任务附件/","physical_name":"{YYYYMMDD}_{UUID}_{originalName}","file_hash":"{SHA-256}","original_name":"{原始文件名}","file_size":{字节数},"mime_type":"{MIME类型}","uploaded_at":"{ISO8601时间}"}]
     * 必填字段：bucket_name, storage_path, physical_name, file_hash, original_name
     * 可选字段：file_size, mime_type, uploaded_at
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Object> attachments;

    /**
     * @提醒的用户ID列表（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> mentionedUserIds;
}

