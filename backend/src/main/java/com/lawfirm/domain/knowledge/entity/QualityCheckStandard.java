package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 质量检查标准实体（M10-030）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("quality_check_standard")
public class QualityCheckStandard extends BaseEntity {

    /**
     * 标准编号
     */
    private String standardNo;

    /**
     * 标准名称
     */
    private String standardName;

    /**
     * 分类：CONTRACT-合同, DOCUMENT-文书, PROCEDURE-程序, OTHER-其他
     */
    private String category;

    /**
     * 标准描述
     */
    private String description;

    /**
     * 检查项（JSON格式）
     */
    private String checkItems;

    /**
     * 适用案件类型
     */
    private String applicableMatterTypes;

    /**
     * 权重
     */
    private BigDecimal weight;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sortOrder;

    public static final String CATEGORY_CONTRACT = "CONTRACT";
    public static final String CATEGORY_DOCUMENT = "DOCUMENT";
    public static final String CATEGORY_PROCEDURE = "PROCEDURE";
    public static final String CATEGORY_OTHER = "OTHER";
}

