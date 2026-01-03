package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 资产领用命令
 */
@Data
public class AssetReceiveCommand {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    /**
     * 领用人ID（不填则为当前用户）
     */
    private Long userId;

    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;

    /**
     * 领用原因/用途
     */
    private String reason;

    private String remarks;
}
