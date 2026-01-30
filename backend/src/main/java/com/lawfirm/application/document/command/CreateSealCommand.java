package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建印章命令 */
@Data
public class CreateSealCommand {

  /** 印章名称. */
  @NotBlank(message = "印章名称不能为空")
  private String name;

  /** 印章类型. */
  @NotBlank(message = "印章类型不能为空")
  private String sealType;

  /** 保管人ID. */
  @NotNull(message = "保管人不能为空")
  private Long keeperId;

  /** 保管人姓名. */
  private String keeperName;

  /** 印章图片URL. */
  private String imageUrl;

  /** 描述. */
  private String description;
}
