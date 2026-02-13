package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 出函申请实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("letter_application")
public class LetterApplication extends BaseEntity {

  /** 申请编号 */
  private String applicationNo;

  /** 模板ID */
  private Long templateId;

  /** 关联项目ID */
  private Long matterId;

  /** 关联客户ID */
  private Long clientId;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人姓名 */
  private String applicantName;

  /** 部门ID */
  private Long departmentId;

  /** 函件类型 */
  private String letterType;

  /** 接收单位 */
  private String targetUnit;

  /** 接收单位联系人 */
  private String targetContact;

  /** 接收单位电话 */
  private String targetPhone;

  /** 接收单位地址 */
  private String targetAddress;

  /** 出函事由 */
  private String purpose;

  /** 出函律师ID列表（逗号分隔） */
  private String lawyerIds;

  /** 出函律师姓名列表 */
  private String lawyerNames;

  /** 生成的函件内容 */
  private String content;

  /** 份数 */
  @lombok.Builder.Default private Integer copies = 1;

  /** 期望日期 */
  private LocalDate expectedDate;

  /** 状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, PRINTED-已打印, RECEIVED-已领取, CANCELLED-已取消 */
  @lombok.Builder.Default private String status = "PENDING";

  /** 指定审批人ID（申请时选择） */
  private Long assignedApproverId;

  /** 审批中心审批记录ID（关联 workbench_approval 表） */
  private Long approvalId;

  /** 审批人ID（实际审批人） */
  private Long approvedBy;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;

  /** 打印人ID（行政） */
  private Long printedBy;

  /** 打印时间 */
  private LocalDateTime printedAt;

  /** 领取人ID */
  private Long receivedBy;

  /** 领取时间 */
  private LocalDateTime receivedAt;

  /** 备注 */
  private String remark;
}
