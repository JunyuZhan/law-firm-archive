package com.lawfirm.application.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户访问令牌 DTO
 */
@Data
@Schema(description = "客户访问令牌信息")
public class ClientAccessTokenDTO {

    @Schema(description = "令牌ID")
    private Long id;

    @Schema(description = "访问令牌（创建时返回完整令牌，查询时脱敏）")
    private String token;

    @Schema(description = "客户ID")
    private Long clientId;

    @Schema(description = "客户名称")
    private String clientName;

    @Schema(description = "项目ID")
    private Long matterId;

    @Schema(description = "项目名称")
    private String matterName;

    @Schema(description = "授权范围列表")
    private List<String> scopes;

    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "最大访问次数")
    private Integer maxAccessCount;

    @Schema(description = "已访问次数")
    private Integer accessCount;

    @Schema(description = "IP白名单")
    private String ipWhitelist;

    @Schema(description = "最后访问IP")
    private String lastAccessIp;

    @Schema(description = "最后访问时间")
    private LocalDateTime lastAccessAt;

    @Schema(description = "状态：ACTIVE-有效, REVOKED-已撤销, EXPIRED-已过期")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建人姓名")
    private String creatorName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "客户门户访问链接")
    private String portalUrl;
}

