package com.lawfirm.domain.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 档案实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive")
public class Archive extends BaseEntity {

  /** 档案号 */
  private String archiveNo;

  /** 案件ID */
  private Long matterId;

  /** 档案名称 */
  private String archiveName;

  /** 档案类型：LITIGATION(诉讼), NON_LITIGATION(非诉), CONSULTATION(咨询) */
  private String archiveType;

  /** 案件编号（冗余） */
  private String matterNo;

  /** 案件名称（冗余） */
  private String matterName;

  /** 客户名称（冗余） */
  private String clientName;

  /** 主办律师（冗余） */
  private String mainLawyerName;

  /** 结案日期 */
  private LocalDate caseCloseDate;

  /** 卷数 */
  private Integer volumeCount;

  /** 总页数 */
  private Integer pageCount;

  /** 档案目录（JSON格式） */
  private String catalog;

  /** 库位ID */
  private Long locationId;

  /** 档案盒编号 */
  private String boxNo;

  /** 保管期限：PERMANENT(永久), 30_YEARS, 15_YEARS, 10_YEARS, 5_YEARS */
  private String retentionPeriod;

  /** 保管到期日 */
  private LocalDate retentionExpireDate;

  /** 是否有电子档案 */
  private Boolean hasElectronic;

  /** 电子档案地址 */
  private String electronicUrl;

  /** 状态：PENDING(待入库), STORED(已入库), BORROWED(借出), DESTROYED(已销毁) */
  private String status;

  /** 入库人ID */
  private Long storedBy;

  /** 入库时间 */
  private LocalDateTime storedAt;

  /** 销毁日期（兼容旧数据） */
  private LocalDate destroyDate;

  /** 销毁原因（兼容旧数据） */
  private String destroyReason;

  /** 销毁审批人ID（兼容旧数据） */
  private Long destroyApproverId;

  /** 备注 */
  private String remarks;

  // ========== 新增字段：档案迁移相关 ==========

  /** 迁移日期 */
  private LocalDate migrateDate;

  /** 迁移原因 */
  private String migrateReason;

  /** 迁移审批人ID */
  private Long migrateApproverId;

  /** 迁移目标 */
  private String migrateTarget;

  /** 文件是否已删除 */
  @lombok.Builder.Default private Boolean filesDeleted = false;

  /** 档案快照（JSON） */
  private String archiveSnapshot;

  public Long getMigrateApproverId() {
    return migrateApproverId;
  }

  public void setMigrateApproverId(final Long migrateApproverId) {
    this.migrateApproverId = migrateApproverId;
  }

  public String getMigrateTarget() {
    return migrateTarget;
  }

  public void setMigrateTarget(final String migrateTarget) {
    this.migrateTarget = migrateTarget;
  }

  public Boolean getFilesDeleted() {
    return filesDeleted;
  }

  public void setFilesDeleted(final Boolean filesDeleted) {
    this.filesDeleted = filesDeleted;
  }

  public String getArchiveSnapshot() {
    return archiveSnapshot;
  }

  public void setArchiveSnapshot(final String archiveSnapshot) {
    this.archiveSnapshot = archiveSnapshot;
  }
}
