package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 采购明细DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemDTO {

    private Long id;
    private Long requestId;
    private String itemName;
    private String specification;
    private String unit;
    private Integer quantity;
    private BigDecimal estimatedPrice;
    private BigDecimal actualPrice;
    private BigDecimal estimatedAmount;
    private BigDecimal actualAmount;
    private Integer receivedQuantity;
    private String remarks;
    
    /**
     * 是否已全部入库
     */
    private Boolean fullyReceived;
}
