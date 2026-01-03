package com.lawfirm.application.archive.dto;

import lombok.Data;

/**
 * 库位容量监控DTO（M7-014）
 */
@Data
public class LocationCapacityDTO {
    /**
     * 库位ID
     */
    private Long locationId;

    /**
     * 库位编码
     */
    private String locationCode;

    /**
     * 库位名称
     */
    private String locationName;

    /**
     * 总容量
     */
    private Integer totalCapacity;

    /**
     * 已用容量
     */
    private Integer usedCapacity;

    /**
     * 可用容量
     */
    private Integer availableCapacity;

    /**
     * 使用率（百分比）
     */
    private Double usageRate;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;
}

