package com.lawfirm.domain.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 档案库位实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_location")
public class ArchiveLocation extends BaseEntity {

    /**
     * 库位编码
     */
    private String locationCode;

    /**
     * 库位名称
     */
    private String locationName;

    /**
     * 档案室
     */
    private String room;

    /**
     * 档案柜
     */
    private String cabinet;

    /**
     * 层
     */
    private String shelf;

    /**
     * 位置
     */
    private String position;

    /**
     * 总容量（卷）
     */
    private Integer totalCapacity;

    /**
     * 已用容量
     */
    private Integer usedCapacity;

    /**
     * 状态：AVAILABLE(可用), FULL(已满), MAINTENANCE(维护中)
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;
}

