package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建档案命令. */
@Data
public class CreateArchiveCommand {

  /** 案件ID */
  @NotNull(message = "案件ID不能为空")
  private Long matterId;

  /** 档案名称 */
  private String archiveName;

  /** 档案类型 */
  private String archiveType;

  /** 卷数 */
  private Integer volumeCount;

  /** 页数 */
  private Integer pageCount;

  /** 目录 */
  private String catalog;

  /** 保管期限 */
  private String retentionPeriod;

  /** 是否有电子档案 */
  private Boolean hasElectronic;

  /** 电子档案URL */
  private String electronicUrl;

  /** 备注 */
  private String remarks;

  /** 用户选择要包含的数据源ID列表 如果为空，则包含所有启用的数据源 */
  private List<Long> selectedDataSourceIds;

  /** 归档数据快照（JSON格式） 由系统自动生成，包含项目所有相关数据 */
  private String archiveSnapshot;
}
