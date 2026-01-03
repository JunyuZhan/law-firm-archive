package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryDTO extends PageQuery {

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 执行人ID
     */
    private Long assigneeId;

    /**
     * 状态
     */
    private String status;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 标题（模糊搜索）
     */
    private String title;
}
