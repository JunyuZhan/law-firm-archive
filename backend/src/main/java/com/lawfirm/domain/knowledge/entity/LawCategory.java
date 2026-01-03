package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 法规分类实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("law_category")
public class LawCategory extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 父分类ID */
    private Long parentId;

    /** 层级 */
    private Integer level;

    /** 排序 */
    private Integer sortOrder;

    /** 描述 */
    private String description;
}
