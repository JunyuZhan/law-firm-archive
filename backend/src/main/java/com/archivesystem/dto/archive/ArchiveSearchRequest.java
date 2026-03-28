package com.archivesystem.dto.archive;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案搜索请求DTO
 */
@Data
public class ArchiveSearchRequest {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索字段（为空则搜索所有字段）
     * 可选值: title, archiveNo, caseNo, caseName, clientName, keywords, fileContent
     */
    private List<String> searchFields;

    /**
     * 全宗ID筛选
     */
    private Long fondsId;

    /**
     * 分类ID筛选
     */
    private Long categoryId;

    /**
     * 档案类型筛选
     */
    private String archiveType;

    /**
     * 保管期限筛选
     */
    private String retentionPeriod;

    /**
     * 密级筛选
     */
    private String securityLevel;

    /**
     * 状态筛选
     */
    private String status;

    /**
     * 来源类型筛选
     */
    private String sourceType;

    /**
     * 主办律师筛选
     */
    private String lawyerName;

    /**
     * 归档日期开始
     */
    private LocalDate archiveDateStart;

    /**
     * 归档日期结束
     */
    private LocalDate archiveDateEnd;

    /**
     * 归档年份筛选
     */
    private Integer archiveYear;

    /**
     * 是否高亮显示
     */
    private Boolean highlight = true;

    /**
     * 是否返回聚合统计
     */
    private Boolean aggregation = false;

    /**
     * 排序字段
     */
    private String sortField = "receivedAt";

    /**
     * 排序方向（asc/desc）
     */
    private String sortOrder = "desc";

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 获取ES查询的起始位置
     */
    public int getFrom() {
        return (pageNum - 1) * pageSize;
    }
}
