package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 档案库位 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveLocationDTO extends BaseDTO {

    private String locationCode;
    private String locationName;
    private String room;
    private String cabinet;
    private String shelf;
    private String position;
    private Integer totalCapacity;
    private Integer usedCapacity;
    private Integer availableCapacity;
    private String status;
    private String statusName;
    private String remarks;
}

