package com.archivesystem.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 档案查询DTO.
 * @author junyuzhan
 */
@Data
public class ArchiveQueryDTO {

    /** 档案编号（模糊查询） */
    private String archiveNo;

    /** 档案名称（模糊查询） */
    private String archiveName;

    /** 档案类型 */
    private String archiveType;

    /** 档案分类 */
    private String category;

    /** 来源类型 */
    private String sourceType;

    /** 来源编号（模糊查询） */
    private String sourceNo;

    /** 客户名称（模糊查询） */
    private String clientName;

    /** 主办人（模糊查询） */
    private String responsiblePerson;

    /** 存放位置ID */
    private Long locationId;

    /** 状态 */
    private String status;

    /** 保管期限 */
    private String retentionPeriod;

    /** 结案日期起始 */
    private LocalDate caseCloseDateFrom;

    /** 结案日期结束 */
    private LocalDate caseCloseDateTo;

    /** 接收日期起始 */
    private LocalDate receivedAtFrom;

    /** 接收日期结束 */
    private LocalDate receivedAtTo;

    /** 关键词（全文搜索） */
    private String keyword;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 20;

    /** 排序字段 */
    private String sortField;

    /** 排序方向：asc/desc */
    private String sortOrder;
}
