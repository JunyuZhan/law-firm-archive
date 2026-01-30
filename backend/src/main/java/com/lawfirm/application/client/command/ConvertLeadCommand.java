package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 案源转化命令 */
@Data
public class ConvertLeadCommand {

  /** 案源ID */
  @NotNull(message = "案源ID不能为空")
  private Long leadId;

  /** 是否创建新客户（true-创建新客户, false-关联已有客户） */
  private Boolean createNewClient;

  /** 已有客户ID（createNewClient=false时必填） */
  private Long clientId;

  /** 新客户信息（createNewClient=true时必填） */
  private String clientName;

  /** 客户类型 */
  private String clientType;

  /** 联系电话 */
  private String contactPhone;

  /** 联系邮箱 */
  private String contactEmail;

  /** 是否同时创建项目 */
  private Boolean createMatter;

  /** 项目信息（createMatter=true时必填） */
  private String matterName;

  /** 项目类型 */
  private String matterType;

  /** 业务类型 */
  private String businessType;

  /** 合同金额 */
  private BigDecimal contractAmount;

  /** 主办律师ID */
  private Long leadLawyerId;
}
