package com.lawfirm.application.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 案源线索 DTO */
@Data
public class LeadDTO {

  /** 案源ID */
  private Long id;

  /** 案源编号 */
  private String leadNo;

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

  /** 最后跟进时间 */
  private LocalDateTime lastFollowTime;

  /** 下次跟进时间 */
  private LocalDateTime nextFollowTime;

  /** 跟进次数 */
  private Integer followCount;

  /** 转化时间 */
  private LocalDateTime convertedAt;

  /** 转化客户ID */
  private Long convertedToClientId;

  /** 转化客户名称 */
  private String convertedToClientName;

  /** 转化项目ID */
  private Long convertedToMatterId;

  /** 转化项目名称 */
  private String convertedToMatterName;

  /** 创建人ID */
  private Long originatorId;

  /** 创建人名称 */
  private String originatorName;

  /** 负责人ID */
  private Long responsibleUserId;

  /** 负责人名称 */
  private String responsibleUserName;

  /** 备注 */
  private String remark;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
