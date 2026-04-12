package com.archivesystem.dto.archive;

import lombok.Data;

import java.time.LocalDate;

/**
 * 档案查询请求DTO.
 * @author junyuzhan
 */
@Data
public class ArchiveQueryRequest {

    /** 关键词（搜索题名、档案号等） */
    private String keyword;

    /** 档案号 */
    private String archiveNo;

    /** 全宗ID */
    private Long fondsId;

    /** 分类ID */
    private Long categoryId;

    /** 档案门类 */
    private String archiveType;

    /** 保管期限 */
    private String retentionPeriod;

    /** 密级 */
    private String securityLevel;

    /** 来源类型 */
    private String sourceType;

    /** 档案形式 */
    private String archiveForm;

    /** 状态 */
    private String status;

    /** 案件编号 */
    private String caseNo;

    /** 扫描批次号 */
    private String scanBatchNo;

    /** 归档日期开始 */
    private LocalDate archiveDateStart;

    /** 归档日期结束 */
    private LocalDate archiveDateEnd;

    /** 创建时间开始 */
    private LocalDate createdAtStart;

    /** 创建时间结束 */
    private LocalDate createdAtEnd;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 20;

    /** 排序字段 */
    private String sortField = "createdAt";

    /** 排序方向 */
    private String sortOrder = "desc";
}
