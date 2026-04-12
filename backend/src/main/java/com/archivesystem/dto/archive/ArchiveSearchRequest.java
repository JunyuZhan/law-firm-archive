package com.archivesystem.dto.archive;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案搜索请求DTO
 * @author junyuzhan
 */
@Data
public class ArchiveSearchRequest {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RESULT_WINDOW = 10000;

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
     * 是否检索正文/OCR内容.
     * 默认关闭，避免普通检索落到大文本字段。
     */
    private Boolean includeFileContent = false;

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
    private Integer pageNum = DEFAULT_PAGE_NUM;

    /**
     * 每页大小
     */
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 获取ES查询的起始位置
     */
    public int getFrom() {
        int from = (getPageNum() - 1) * getPageSize();
        if (from >= MAX_RESULT_WINDOW) {
            return Math.max(0, MAX_RESULT_WINDOW - getPageSize());
        }
        return from;
    }
}
