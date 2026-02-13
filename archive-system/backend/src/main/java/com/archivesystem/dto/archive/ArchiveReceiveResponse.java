package com.archivesystem.dto.archive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 档案接收响应DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveReceiveResponse {

    /** 档案ID */
    private Long archiveId;

    /** 档案号 */
    private String archiveNo;

    /** 状态 */
    private String status;

    /** 接收时间 */
    private LocalDateTime receivedAt;

    /** 文件数量 */
    private Integer fileCount;

    /** 处理消息 */
    private String message;
}
