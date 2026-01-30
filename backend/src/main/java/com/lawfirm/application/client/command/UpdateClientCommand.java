package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 更新客户命令 */
@Data
public class UpdateClientCommand {

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long id;

  /** 客户名称 */
  private String name;

  /** 客户类型 */
  private String clientType;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 身份证号 */
  private String idCard;

  /** 法定代表人 */
  private String legalRepresentative;

  /** 注册地址 */
  private String registeredAddress;

  /** 联系人 */
  private String contactPerson;

  /** 联系电话 */
  private String contactPhone;

  /** 联系邮箱 */
  private String contactEmail;

  /** 行业 */
  private String industry;

  /** 来源 */
  private String source;

  /** 客户级别 */
  private String level;

  /** 客户类别 */
  private String category;

  /** 状态 */
  private String status;

  /** 创建人ID */
  private Long originatorId;

  /** 负责律师ID */
  private Long responsibleLawyerId;

  /** 首次合作日期 */
  private LocalDate firstCooperationDate;

  /** 备注 */
  private String remark;
}
