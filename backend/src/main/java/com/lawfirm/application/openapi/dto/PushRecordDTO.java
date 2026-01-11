package com.lawfirm.application.openapi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推送记录 DTO
 */
@Data
@Builder
public class PushRecordDTO {

    private Long id;

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 项目名称
     */
    private String matterName;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 客户名称
     */
    private String clientName;

    /**
     * 推送类型
     */
    private String pushType;

    /**
     * 推送范围
     */
    private List<String> scopes;

    /**
     * 状态
     */
    private String status;

    /**
     * 客户服务系统的数据ID
     */
    private String externalId;

    /**
     * 客户访问链接（来自客户服务系统）
     */
    private String externalUrl;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 数据有效期
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

