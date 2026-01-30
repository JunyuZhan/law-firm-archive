package com.lawfirm.application.admin.command;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 创建出函申请命令 */
@Data
public class CreateLetterApplicationCommand {
  /** 模板ID */
  private Long templateId;

  /** 项目ID */
  private Long matterId;

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

  /** 出函律师ID列表 */
  private List<Long> lawyerIds;

  /** 份数 */
  private Integer copies;

  /** 期望日期 */
  private LocalDate expectedDate;

  /** 审批人ID */
  private Long approverId;

  /** 备注 */
  private String remark;
}
