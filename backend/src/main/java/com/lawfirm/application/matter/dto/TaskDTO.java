package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDTO extends BaseDTO {

    private Long id;
    private String taskNo;
    
    private Long matterId;
    private String matterName;
    
    private Long parentId;
    
    private String title;
    private String description;
    
    private String priority;
    private String priorityName;
    
    private Long assigneeId;
    private String assigneeName;
    
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    
    private String status;
    private String statusName;
    private Integer progress;
    
    private LocalDateTime reminderDate;
    private Boolean reminderSent;
    
    /**
     * 是否逾期
     */
    private Boolean overdue;
    
    /**
     * 子任务数
     */
    private Integer subTaskCount;
    
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
