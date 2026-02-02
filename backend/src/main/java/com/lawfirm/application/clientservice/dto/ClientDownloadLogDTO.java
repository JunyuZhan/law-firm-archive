package com.lawfirm.application.clientservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 客户下载日志 DTO
 */
@Data
public class ClientDownloadLogDTO {

    /** 日志ID */
    private Long id;

    /** 项目ID */
    private Long matterId;

    /** 客户ID */
    private Long clientId;

    /** 客户服务系统的文件ID */
    private String fileId;

    /** 文件名 */
    private String fileName;

    /** 下载时间 */
    private LocalDateTime downloadTime;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 事件类型 */
    private String eventType;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
