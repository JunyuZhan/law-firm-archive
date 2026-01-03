package com.lawfirm.application.admin.dto;

import lombok.Data;

/**
 * 资产盘点明细DTO（M8-033）
 */
@Data
public class AssetInventoryDetailDTO {
    private Long id;
    private Long inventoryId;
    private Long assetId;
    private String assetNo;
    private String assetName;
    private String expectedStatus;
    private String actualStatus;
    private String expectedLocation;
    private String actualLocation;
    private Long expectedUserId;
    private String expectedUserName;
    private Long actualUserId;
    private String actualUserName;
    private String discrepancyType;
    private String discrepancyTypeName;
    private String discrepancyDesc;
    private String remark;
}

