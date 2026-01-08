package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQueryDTO extends PageQuery {

    /**
     * 标题（模糊搜索）
     */
    private String title;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 安全级别
     */
    private String securityLevel;

    /**
     * 状态
     */
    private String status;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 创建人ID（用于"我的文书"查询）
     */
    private Long createdBy;
}
