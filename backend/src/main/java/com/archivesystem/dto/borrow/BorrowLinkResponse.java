package com.archivesystem.dto.borrow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅链接管理响应 DTO.
 */
@Data
@Builder
public class BorrowLinkResponse {
    private Long id;
    private Long borrowId;
    private Long archiveId;
    private String archiveNo;
    private String sourceType;
    private String sourceUserName;
    private String borrowPurpose;
    private LocalDateTime expireAt;
    private Integer maxAccessCount;
    private Boolean allowDownload;
    private Integer accessCount;
    private Integer downloadCount;
    private LocalDateTime lastAccessAt;
    private String lastAccessIp;
    private String status;
    private String revokeReason;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private String accessUrl;
}
