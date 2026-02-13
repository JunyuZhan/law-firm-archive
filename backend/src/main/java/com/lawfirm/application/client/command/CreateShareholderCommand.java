package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建股东信息命令 */
@Data
public class CreateShareholderCommand {

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 股东名称 */
  @NotBlank(message = "股东名称不能为空")
  private String shareholderName;

  /** 股东类型（INDIVIDUAL-个人, ENTERPRISE-企业） */
  @NotBlank(message = "股东类型不能为空")
  private String shareholderType;

  /** 个人股东身份证号 */
  private String idCard;

  /** 企业股东统一社会信用代码 */
  private String creditCode;

  /** 持股比例 */
  private BigDecimal shareholdingRatio;

  /** 投资金额 */
  private BigDecimal investmentAmount;

  /** 投资日期 */
  private LocalDate investmentDate;

  /** 职务 */
  private String position;

  /** 备注 */
  private String remark;
}
