package com.lawfirm.application.clientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 下载日志回调请求（客户服务系统回调）
 */
@Data
public class DownloadLogCallbackRequest {

    /** 律所系统的项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long matterId;

    /** 客户ID */
    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    /** 客户服务系统的文件ID */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    /** 文件名 */
    private String fileName;

    /** 下载时间 */
    @NotNull(message = "下载时间不能为空")
    private LocalDateTime downloadTime;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 事件类型（固定值：DOWNLOAD） */
    @NotNull(message = "事件类型不能为空")
    private String eventType;
}
