package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 项目卷宗目录实体 实际项目的目录结构，基于模板生成，可自定义修改 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter_dossier_item")
public class MatterDossierItem extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 父目录ID，0表示顶级 */
  @lombok.Builder.Default private Long parentId = 0L;

  /** 目录名称 */
  private String name;

  /** 类型: FOLDER-目录, FILE-文件占位 */
  @lombok.Builder.Default private String itemType = "FOLDER";

  /** 文件分类 */
  private String fileCategory;

  /** 排序号 */
  @lombok.Builder.Default private Integer sortOrder = 0;

  /** 文件数量（统计） */
  @lombok.Builder.Default private Integer documentCount = 0;

  public String getName() {
    return name;
  }

  public void setMatterId(final Long matterId) {
    this.matterId = matterId;
  }

  public void setParentId(final Long parentId) {
    this.parentId = parentId;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setItemType(final String itemType) {
    this.itemType = itemType;
  }

  public void setFileCategory(final String fileCategory) {
    this.fileCategory = fileCategory;
  }

  public void setSortOrder(final Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public void setDocumentCount(final Integer documentCount) {
    this.documentCount = documentCount;
  }
}
