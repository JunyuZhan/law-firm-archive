package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 客户标签实体。 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_client_tag")
public class ClientTag extends BaseEntity {

  /** 标签名称 */
  private String tagName;

  /** 标签颜色（十六进制） */
  private String tagColor;

  /** 标签描述 */
  private String description;

  /** 排序顺序 */
  private Integer sortOrder;
}
