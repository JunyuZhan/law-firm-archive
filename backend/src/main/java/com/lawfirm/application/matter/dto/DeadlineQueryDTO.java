package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/** 期限提醒查询 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeadlineQueryDTO extends PageQuery {

  /** 案件ID. */
  private Long matterId;

  /** 期限类型. */
  private String deadlineType;

  /** 状态. */
  private String status;

  /** 期限日期开始. */
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate deadlineDateStart;

  /** 期限日期结束. */
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate deadlineDateEnd;
}
