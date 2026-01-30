package com.lawfirm.application.client.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 案源跟进记录 DTO */
@Data
public class LeadFollowUpDTO {

  /** 跟进记录ID */
  private Long id;

  /** 案源ID */
  private Long leadId;

  /** 跟进方式 */
  private String followType;

  /** 跟进内容 */
  private String followContent;

  /** 跟进结果 */
  private String followResult;

  /** 下次跟进时间 */
  private LocalDateTime nextFollowTime;

  /** 下次跟进计划 */
  private String nextFollowPlan;

  /** 跟进人ID */
  private Long followUserId;

  /** 跟进人名称 */
  private String followUserName;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
