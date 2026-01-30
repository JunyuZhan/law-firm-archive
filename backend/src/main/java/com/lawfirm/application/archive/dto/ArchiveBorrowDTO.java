package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 档案借阅DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveBorrowDTO extends BaseDTO {

  /** 借阅编号 */
  private String borrowNo;

  /** 档案ID */
  private Long archiveId;

  /** 档案编号 */
  private String archiveNo;

  /** 档案名称 */
  private String archiveName;

  /** 借阅人ID */
  private Long borrowerId;

  /** 借阅人姓名 */
  private String borrowerName;

  /** 部门 */
  private String department;

  /** 借阅原因 */
  private String borrowReason;

  /** 借阅日期 */
  private LocalDate borrowDate;

  /** 预计归还日期 */
  private LocalDate expectedReturnDate;

  /** 实际归还日期 */
  private LocalDate actualReturnDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approverId;

  /** 审批人姓名 */
  private String approverName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 拒绝原因 */
  private String rejectionReason;

  /** 归还处理人ID */
  private Long returnHandlerId;

  /** 归还处理人姓名 */
  private String returnHandlerName;

  /** 归还状态 */
  private String returnCondition;

  /** 归还状态名称 */
  private String returnConditionName;

  /** 归还备注 */
  private String returnRemarks;

  /** 是否逾期 */
  private Boolean isOverdue;
}
