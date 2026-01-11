package com.lawfirm.application.openapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 数据推送请求
 */
@Data
public class PushRequest {

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long matterId;

    /**
     * 客户ID（可选，默认使用项目关联的客户）
     */
    private Long clientId;

    /**
     * 推送范围
     */
    @NotEmpty(message = "推送范围不能为空")
    private List<String> scopes;

    /**
     * 推送类型（可选）
     * MANUAL - 手动推送
     * AUTO - 自动推送
     * UPDATE - 数据更新
     */
    private String pushType;

    /**
     * 数据有效期（天）
     */
    private Integer validDays;

    /**
     * 要推送的文档ID列表（当 scopes 包含 DOCUMENT_FILES 时使用）
     * 如果为空，则推送所有公开文档
     */
    private List<Long> documentIds;

    /**
     * 备注说明（可选）
     */
    private String remark;
}

