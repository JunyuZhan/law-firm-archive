package com.lawfirm.application.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建客户访问令牌命令
 */
@Data
@Schema(description = "创建客户访问令牌请求")
public class CreateTokenCommand {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", required = true)
    private Long clientId;

    @Schema(description = "项目ID（可选，限定到具体项目）")
    private Long matterId;

    @NotNull(message = "授权范围不能为空")
    @Schema(description = "授权范围列表", example = "[\"MATTER_INFO\", \"MATTER_PROGRESS\", \"LAWYER_INFO\"]")
    private List<String> scopes;

    @NotNull(message = "有效期不能为空")
    @Schema(description = "有效期（天）", example = "30")
    private Integer validDays;

    @Schema(description = "最大访问次数（空表示不限制）")
    private Integer maxAccessCount;

    @Schema(description = "IP白名单（逗号分隔，空表示不限制）")
    private String ipWhitelist;

    @Schema(description = "备注")
    private String remark;
}

