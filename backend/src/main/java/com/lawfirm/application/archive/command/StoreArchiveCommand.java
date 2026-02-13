package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 档案入库命令. */
@Data
public class StoreArchiveCommand {

  /** 档案ID */
  @NotNull(message = "档案ID不能为空")
  private Long archiveId;

  /** 库位ID */
  @NotNull(message = "库位ID不能为空")
  private Long locationId;

  /** 箱号 */
  private String boxNo;
}
