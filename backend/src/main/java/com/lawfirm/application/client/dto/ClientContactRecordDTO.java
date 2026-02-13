package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户联系记录 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientContactRecordDTO extends BaseDTO {

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 联系人ID */
  private Long contactId;

  /** 联系人姓名 */
  private String contactPerson;

  /** 联系方式 */
  private String contactMethod;

  /** 联系方式名称 */
  private String contactMethodName;

  /** 联系日期 */
  private LocalDateTime contactDate;

  /** 联系时长（分钟） */
  private Integer contactDuration;

  /** 联系地点 */
  private String contactLocation;

  /** 联系内容 */
  private String contactContent;

  /** 联系结果 */
  private String contactResult;

  /** 下次跟进日期 */
  private LocalDate nextFollowUpDate;

  /** 是否提醒跟进 */
  private Boolean followUpReminder;

  /** 创建人ID */
  private Long createdBy;

  /** 创建人名称 */
  private String createdByName;
}
