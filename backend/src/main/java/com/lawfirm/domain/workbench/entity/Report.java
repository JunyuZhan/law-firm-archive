package com.lawfirm.domain.workbench.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 报表记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("workbench_report")
public class Report extends BaseEntity {

    /**
     * 报表编号
     */
    private String reportNo;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表类型：REVENUE-收入报表, MATTER-案件报表, CLIENT-客户报表, LAWYER_PERFORMANCE-律师业绩报表
     */
    private String reportType;

    /**
     * 报表格式：EXCEL, PDF
     */
    private String format;

    /**
     * 状态：GENERATING-生成中, COMPLETED-已完成, FAILED-失败
     */
    private String status;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 报表参数（JSON格式）
     */
    private String parameters;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 生成人ID
     */
    private Long generatedBy;

    /**
     * 生成人姓名
     */
    private String generatedByName;
}

