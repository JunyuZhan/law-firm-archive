package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表 DTO
 */
@Data
public class ReportDTO {
    private Long id;
    private String reportNo;
    private String reportName;
    private String reportType;          // REVENUE-收入报表, MATTER-案件报表, CLIENT-客户报表, etc.
    private String reportTypeName;
    private String format;               // EXCEL, PDF
    private String status;               // GENERATING-生成中, COMPLETED-已完成, FAILED-失败
    private String statusName;
    private String fileUrl;              // 报表文件下载地址
    private Long fileSize;               // 文件大小（字节）
    private Map<String, Object> parameters;  // 报表参数（JSON）
    private LocalDateTime generatedAt;   // 生成时间
    private Long generatedBy;           // 生成人ID
    private String generatedByName;      // 生成人姓名
    private LocalDateTime createdAt;
}

