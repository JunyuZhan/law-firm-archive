package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 导出日志实体 用于记录司法局报备等数据导出操作
 *
 * <p>Requirements: 6.7
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_export_log")
public class ExportLog extends BaseEntity {

  /** 导出类型：JUDICIAL_FILING-司法局报备, CONTRACT_LIST-合同列表等 */
  private String exportType;

  /** 导出参数（JSON格式，如年月、筛选条件等） */
  private String exportParams;

  /** 导出记录数 */
  private Integer recordCount;

  /** 导出人ID */
  private Long exportedBy;

  /** 导出时间 */
  private LocalDateTime exportedAt;

  /** 导出文件名 */
  private String fileName;

  /** 文件大小（字节） */
  private Long fileSize;
}
