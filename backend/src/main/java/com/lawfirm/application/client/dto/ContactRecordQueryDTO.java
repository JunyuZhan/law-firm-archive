package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 联系记录查询 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContactRecordQueryDTO extends PageQuery {

  /** 客户ID. */
  private Long clientId;

  /** 联系人ID. */
  private Long contactId;

  /** 联系方式. */
  private String contactMethod;

  /** 开始日期. */
  private LocalDate startDate;

  /** 结束日期. */
  private LocalDate endDate;

  /** 是否设置提醒. */
  private Boolean followUpReminder;
}
