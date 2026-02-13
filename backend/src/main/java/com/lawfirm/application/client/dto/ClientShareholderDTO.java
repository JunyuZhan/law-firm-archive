package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户股东信息 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientShareholderDTO extends BaseDTO {

  /** 客户ID */
  private Long clientId;

  /** 股东名称 */
  private String shareholderName;

  /** 股东类型 */
  private String shareholderType;

  /** 股东类型名称 */
  private String shareholderTypeName;

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

  /** 职位 */
  private String position;

  /** 备注 */
  private String remark;
}
