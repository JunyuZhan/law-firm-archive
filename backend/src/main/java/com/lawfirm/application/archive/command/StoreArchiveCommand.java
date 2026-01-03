package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 档案入库命令
 */
@Data
public class StoreArchiveCommand {

    @NotNull(message = "档案ID不能为空")
    private Long archiveId;

    @NotNull(message = "库位ID不能为空")
    private Long locationId;

    private String boxNo;
}

