package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训完成情况 DTO
 */
@Data
public class TrainingCompletionDTO {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 培训通知ID
     */
    private Long noticeId;
    
    /**
     * 培训通知标题
     */
    private String noticeTitle;
    
    /**
     * 员工ID
     */
    private Long employeeId;
    
    /**
     * 员工姓名
     */
    private String employeeName;
    
    /**
     * 部门名称
     */
    private String departmentName;
    
    /**
     * 证书URL
     */
    private String certificateUrl;
    
    /**
     * 证书文件名
     */
    private String certificateName;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;
}

