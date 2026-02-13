package com.lawfirm.application.clientservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 项目共享配置 DTO */
@Data
@Schema(description = "项目共享配置")
public class MatterShareConfigDTO {

  /** 配置ID */
  @Schema(description = "配置ID")
  private Long id;

  /** 项目ID */
  @Schema(description = "项目ID")
  private Long matterId;

  /** 项目名称 */
  @Schema(description = "项目名称")
  private String matterName;

  /** 是否共享基本信息 */
  @Schema(description = "是否共享基本信息")
  private Boolean shareBasicInfo;

  /** 是否共享进度信息 */
  @Schema(description = "是否共享进度信息")
  private Boolean shareProgress;

  /** 是否共享任务列表 */
  @Schema(description = "是否共享任务列表")
  private Boolean shareTaskList;

  /** 是否共享关键期限 */
  @Schema(description = "是否共享关键期限")
  private Boolean shareDeadline;

  /** 是否共享文档列表 */
  @Schema(description = "是否共享文档列表")
  private Boolean shareDocumentList;

  /** 是否共享团队信息 */
  @Schema(description = "是否共享团队信息")
  private Boolean shareTeamInfo;

  /** 是否共享费用信息 */
  @Schema(description = "是否共享费用信息")
  private Boolean shareFeeInfo;

  /** 隐藏字段列表 */
  @Schema(description = "隐藏字段列表")
  private List<String> hiddenFields;

  /** 是否启用共享 */
  @Schema(description = "是否启用共享")
  private Boolean enabled;

  /** 已创建的令牌数量 */
  @Schema(description = "已创建的令牌数量")
  private Integer tokenCount;
}
