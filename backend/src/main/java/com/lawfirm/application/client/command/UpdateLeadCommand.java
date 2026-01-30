package com.lawfirm.application.client.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新案源命令 */
@Data
public class UpdateLeadCommand {

  /** 案源名称 */
  private String leadName;

  /** 案源类型 */
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

  /** 状态 */
  private String status;

  /** 优先级 */
  private String priority;

  /** 业务类型 */
  private String businessType;

  /** 预估金额 */
  private BigDecimal estimatedAmount;

  /** 描述 */
  private String description;

  /** 下次跟进时间 */
  private LocalDateTime nextFollowTime;

  /** 负责人ID */
  private Long responsibleUserId;

  /** 备注 */
  private String remark;
}
