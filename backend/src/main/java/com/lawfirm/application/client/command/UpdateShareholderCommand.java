package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新股东信息命令 */
@Data
public class UpdateShareholderCommand {

  /** 股东ID */
  @NotNull(message = "股东ID不能为空")
  private Long id;

  /** 股东名称 */
  private String shareholderName;

  /** 股东类型 */
  private String shareholderType;

  /** 身份证号 */
  private String idCard;

  /** 统一社会信用代码 */
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
