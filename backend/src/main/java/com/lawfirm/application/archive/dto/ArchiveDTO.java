package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 档案DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveDTO extends BaseDTO {

  /** 档案号 */
  private String archiveNo;

  /** 项目ID */
  private Long matterId;

  /** 项目编号 */
  private String matterNo;

  /** 项目名称 */
  private String matterName;

  /** 档案名称 */
  private String archiveName;

  /** 档案类型 */
  private String archiveType;

  /** 档案类型名称 */
  private String archiveTypeName;

  /** 客户名称 */
  private String clientName;

  /** 主办律师 */
  private String mainLawyerName;

  /** 结案日期 */
  private LocalDate caseCloseDate;

  /** 卷数 */
  private Integer volumeCount;

  /** 页数 */
  private Integer pageCount;

  /** 目录 */
  private String catalog;

  /** 库位ID */
  private Long locationId;

  /** 库位名称 */
  private String locationName;

  /** 箱号 */
  private String boxNo;

  /** 保管期限 */
  private String retentionPeriod;

  /** 保管期限名称 */
  private String retentionPeriodName;

  /** 保管到期日 */
  private LocalDate retentionExpireDate;

  /** 是否有电子档案 */
  private Boolean hasElectronic;

  /** 电子档案URL */
  private String electronicUrl;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 入库人ID */
  private Long storedBy;

  /** 入库人姓名 */
  private String storedByName;

  /** 入库时间 */
  private LocalDateTime storedAt;

  /** 销毁日期 */
  private LocalDate destroyDate;

  /** 销毁原因 */
  private String destroyReason;

  /** 销毁审批人ID */
  private Long destroyApproverId;

  /** 备注 */
  private String remarks;

  /** 借阅记录 */
  private List<ArchiveBorrowDTO> borrows;
}
