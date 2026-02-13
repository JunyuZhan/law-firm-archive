package com.lawfirm.domain.clientservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 项目共享配置实体 控制项目哪些信息可以对外共享 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("openapi_matter_share_config")
public class MatterShareConfig extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 是否共享基本信息（项目名称、类型、状态） */
  @lombok.Builder.Default private Boolean shareBasicInfo = true;

  /** 是否共享进度信息 */
  @lombok.Builder.Default private Boolean shareProgress = true;

  /** 是否共享任务列表 */
  @lombok.Builder.Default private Boolean shareTaskList = false;

  /** 是否共享关键期限 */
  @lombok.Builder.Default private Boolean shareDeadline = true;

  /** 是否共享文档列表（仅标题，不含内容） */
  @lombok.Builder.Default private Boolean shareDocumentList = false;

  /** 是否共享团队信息（律师姓名、联系方式） */
  @lombok.Builder.Default private Boolean shareTeamInfo = true;

  /** 是否共享费用信息（已收款/待收款） */
  @lombok.Builder.Default private Boolean shareFeeInfo = false;

  /** 需要隐藏的字段列表（JSON数组） */
  private String hiddenFields;

  /** 是否启用共享 */
  @lombok.Builder.Default private Boolean enabled = false;
}
