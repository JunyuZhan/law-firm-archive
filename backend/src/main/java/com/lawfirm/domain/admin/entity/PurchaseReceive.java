package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 采购入库记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_purchase_receive")
public class PurchaseReceive extends BaseEntity {

    /**
     * 入库单号
     */
    private String receiveNo;

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
     * 入库人ID
     */
    private Long receiverId;

    /**
     * 存放位置
     */
    private String location;

    /**
     * 是否转为资产
     */
    private Boolean convertToAsset;

    /**
     * 关联资产ID
     */
    private Long assetId;

    /**
     * 备注
     */
    private String remarks;
}
