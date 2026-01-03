package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 任务评论 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskCommentDTO extends BaseDTO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 附件列表
     */
    private List<String> attachments;

    /**
     * @提醒的用户ID列表
     */
    private List<Long> mentionedUserIds;

    /**
     * @提醒的用户姓名列表
     */
    private List<String> mentionedUserNames;

    /**
     * 评论人姓名
     */
    private String creatorName;
}

