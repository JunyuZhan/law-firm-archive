package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档模板DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentTemplateDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 模板编号. */
  private String templateNo;

  /** 模板名称. */
  private String name;

  /** 分类ID. */
  private Long categoryId;

  /** 分类名称. */
  private String categoryName;

  /** 模板类型. */
  private String templateType;

  /** 模板类型名称. */
  private String templateTypeName;

  /** 文件名. */
  private String fileName;

  /** 文件路径. */
  private String filePath;

  /** 文件大小. */
  private Long fileSize;

  /** 文件大小显示. */
  private String fileSizeDisplay;

  /** 变量列表. */
  private String variables;

  /** 描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 使用次数. */
  private Integer useCount;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
