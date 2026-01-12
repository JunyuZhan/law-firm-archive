package com.lawfirm.application.openapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 同步客户文件到卷宗请求
 */
@Data
public class ClientFileSyncRequest {

    /**
     * 客户文件ID
     */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    /**
     * 目标卷宗目录ID
     */
    @NotNull(message = "目标卷宗ID不能为空")
    private Long targetDossierId;

    /**
     * 同步后的文件名（可选，不填则使用原文件名）
     */
    private String targetFileName;

    /**
     * 文档类别（可选，同步到文档管理时使用）
     */
    private String documentCategory;

    /**
     * 备注
     */
    private String remark;
}
