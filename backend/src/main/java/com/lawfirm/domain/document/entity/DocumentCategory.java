package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 文档分类实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("doc_category")
public class DocumentCategory extends BaseEntity {

  /** 分类名称 */
  private String name;

  /** 父分类ID，0表示顶级 */
  @lombok.Builder.Default private Long parentId = 0L;

  /** 排序 */
  @lombok.Builder.Default private Integer sortOrder = 0;

  /** 描述 */
  private String description;
}
