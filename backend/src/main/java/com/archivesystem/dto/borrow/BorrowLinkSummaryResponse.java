package com.archivesystem.dto.borrow;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 借阅链接摘要响应 DTO.
 */
@Value
@Builder
public class BorrowLinkSummaryResponse {

    Long id;
    Long archiveId;
    String archiveNo;
    String sourceType;
    String sourceUserName;
    String borrowPurpose;
    LocalDateTime expireAt;
    Integer maxAccessCount;
    Boolean allowDownload;
    Integer accessCount;
    Integer downloadCount;
    LocalDateTime lastAccessAt;
    String lastAccessIp;
    String status;
    LocalDateTime createdAt;

    public static BorrowLinkSummaryResponse from(BorrowLinkResponse link) {
        if (link == null) {
            return null;
        }

        return BorrowLinkSummaryResponse.builder()
                .id(link.getId())
                .archiveId(link.getArchiveId())
                .archiveNo(link.getArchiveNo())
                .sourceType(link.getSourceType())
                .sourceUserName(link.getSourceUserName())
                .borrowPurpose(link.getBorrowPurpose())
                .expireAt(link.getExpireAt())
                .maxAccessCount(link.getMaxAccessCount())
                .allowDownload(link.getAllowDownload())
                .accessCount(link.getAccessCount())
                .downloadCount(link.getDownloadCount())
                .lastAccessAt(link.getLastAccessAt())
                .lastAccessIp(link.getLastAccessIp())
                .status(link.getStatus())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
