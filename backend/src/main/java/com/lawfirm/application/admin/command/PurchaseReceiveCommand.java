package com.lawfirm.application.admin.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 采购入库命令
 */
@Data
public class PurchaseReceiveCommand {

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 采购明细ID
     */
    private Long itemId;

    /**
     * 入库数量
     */
    private Integer quantity;

    /**
     * 入库日期
     */
    private LocalDate receiveDate;

    /**
     * 存放位置
     */
    private String location;

    /**
     * 是否转为资产
     */
    private Boolean convertToAsset;

    /**
     * 备注
     */
    private String remarks;
}
