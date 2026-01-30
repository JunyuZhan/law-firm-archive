package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 知识收藏实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_collection")
public class KnowledgeCollection extends BaseEntity {

  /** 用户ID. */
  private Long userId;

  /** 目标类型: LAW法规/CASE案例. */
  private String targetType;

  /** 目标ID. */
  private Long targetId;

  // 目标类型常量
  /** 目标类型：法规. */
  public static final String TYPE_LAW = "LAW";

  /** 目标类型：案例. */
  public static final String TYPE_CASE = "CASE";
}
