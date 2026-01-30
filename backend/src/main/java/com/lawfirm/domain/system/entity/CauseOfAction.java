package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** 案由/罪名实体 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_cause_of_action")
public class CauseOfAction extends BaseEntity {

  private static final long serialVersionUID = 1L;

  /** 案由代码 */
  private String code;

  /** 案由名称 */
  private String name;

  /** 类型: CIVIL-民事, CRIMINAL-刑事, ADMIN-行政 */
  private String causeType;

  /** 所属大类代码 */
  private String categoryCode;

  /** 所属大类名称 */
  private String categoryName;

  /** 父级案由代码 */
  private String parentCode;

  /** 层级: 1=一级案由, 2=二级案由 */
  private Integer level;

  /** 排序号 */
  private Integer sortOrder;

  /** 是否启用 */
  private Boolean isActive;
}
