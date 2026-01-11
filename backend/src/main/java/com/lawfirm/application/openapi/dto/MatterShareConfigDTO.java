package com.lawfirm.application.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 项目共享配置 DTO
 */
@Data
@Schema(description = "项目共享配置")
public class MatterShareConfigDTO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long matterId;

    @Schema(description = "项目名称")
    private String matterName;

    @Schema(description = "是否共享基本信息")
    private Boolean shareBasicInfo;

    @Schema(description = "是否共享进度信息")
    private Boolean shareProgress;

    @Schema(description = "是否共享任务列表")
    private Boolean shareTaskList;

    @Schema(description = "是否共享关键期限")
    private Boolean shareDeadline;

    @Schema(description = "是否共享文档列表")
    private Boolean shareDocumentList;

    @Schema(description = "是否共享团队信息")
    private Boolean shareTeamInfo;

    @Schema(description = "是否共享费用信息")
    private Boolean shareFeeInfo;

    @Schema(description = "隐藏字段列表")
    private List<String> hiddenFields;

    @Schema(description = "是否启用共享")
    private Boolean enabled;

    @Schema(description = "已创建的令牌数量")
    private Integer tokenCount;
}

