package com.lawfirm.application.admin.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建采购申请命令
 */
@Data
public class CreatePurchaseRequestCommand {

    /**
     * 申请标题
     */
    private String title;

    /**
     * 采购类型
     */
    private String purchaseType;

    /**
     * 期望到货日期
     */
    private LocalDate expectedDate;

    /**
     * 采购原因
     */
    private String reason;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 采购明细
     */
    private List<ItemCommand> items;

    @Data
    public static class ItemCommand {
        private String itemName;
        private String specification;
        private String unit;
        private Integer quantity;
        private BigDecimal estimatedPrice;
        private String remarks;
    }
}
