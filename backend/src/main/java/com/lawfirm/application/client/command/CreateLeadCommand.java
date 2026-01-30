package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 创建案源命令 */
@Data
public class CreateLeadCommand {

  /** 案源名称 */
  @NotBlank(message = "案源名称不能为空")
  private String leadName;

  /** 案源类型（INDIVIDUAL, ENTERPRISE） */
  private String leadType;

  /** 联系人姓名 */
  private String contactName;

  /** 联系电话 */
  private String contactPhone;

  /** 联系邮箱 */
  private String contactEmail;

  /** 来源渠道 */
  private String sourceChannel;

  /** 来源详情 */
  private String sourceDetail;

  /** 优先级（HIGH, NORMAL, LOW） */
  private String priority;

  /** 业务类型（LITIGATION, NON_LITIGATION） */
  private String businessType;

  /** 预估金额 */
  private BigDecimal estimatedAmount;

  /** 描述 */
  private String description;

  /** 下次跟进时间 */
  private LocalDateTime nextFollowTime;

  /** 创建人ID */
  private Long originatorId;

  /** 负责人ID */
  private Long responsibleUserId;

  /** 备注 */
  private String remark;
}
