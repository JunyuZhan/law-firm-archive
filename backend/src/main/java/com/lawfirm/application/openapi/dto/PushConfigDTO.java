package com.lawfirm.application.openapi.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 推送配置 DTO
 */
@Data
@Builder
public class PushConfigDTO {

    private Long id;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 是否启用推送
     */
    private Boolean enabled;

    /**
     * 推送范围
     */
    private List<String> scopes;

    /**
     * 项目更新时自动推送
     */
    private Boolean autoPushOnUpdate;

    /**
     * 数据有效期（天）
     */
    private Integer validDays;
}

