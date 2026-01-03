package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产盘点DTO（M8-033）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInventoryDTO extends BaseDTO {
    private Long id;
    private String inventoryNo;
    private LocalDate inventoryDate;
    private String inventoryType;
    private String inventoryTypeName;
    private Long departmentId;
    private String departmentName;
    private String location;
    private String status;
    private String statusName;
    private Integer totalCount;
    private Integer actualCount;
    private Integer surplusCount;
    private Integer shortageCount;
    private String remark;
    private List<AssetInventoryDetailDTO> details;
}

