package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建资产盘点命令（M8-033）
 */
@Data
public class CreateAssetInventoryCommand {
    /**
     * 盘点日期
     */
    @NotNull(message = "盘点日期不能为空")
    private LocalDate inventoryDate;

    /**
     * 盘点类型：FULL-全盘, PARTIAL-抽盘
     */
    @NotNull(message = "盘点类型不能为空")
    private String inventoryType;

    /**
     * 盘点部门ID
     */
    private Long departmentId;

    /**
     * 盘点位置
     */
    private String location;

    /**
     * 备注
     */
    private String remark;

    /**
     * 资产ID列表（抽盘时使用）
     */
    private List<Long> assetIds;
}

