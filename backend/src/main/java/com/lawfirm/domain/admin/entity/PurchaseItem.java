package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 采购明细实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_purchase_item")
public class PurchaseItem extends BaseEntity {

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 物品名称
     */
    private String itemName;

    /**
     * 规格型号
     */
    private String specification;

    /**
     * 单位
     */
    private String unit;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 预估单价
     */
    private BigDecimal estimatedPrice;

    /**
     * 实际单价
     */
    private BigDecimal actualPrice;

    /**
     * 预估金额
     */
    private BigDecimal estimatedAmount;

    /**
     * 实际金额
     */
    private BigDecimal actualAmount;

    /**
     * 已入库数量
     */
    private Integer receivedQuantity;

    /**
     * 备注
     */
    private String remarks;
}
