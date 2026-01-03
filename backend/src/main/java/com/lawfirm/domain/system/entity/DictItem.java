package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 数据字典项实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class DictItem extends BaseEntity {

    /** 字典类型ID */
    private Long dictTypeId;

    /** 显示标签 */
    private String label;

    /** 字典值 */
    private String value;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 */
    private String status;

    /** 样式类名 */
    private String cssClass;

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
}
