package com.archivesystem.dto.borrow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子借阅链接访问响应DTO.
 * 返回档案信息和文件列表供公开访问页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowLinkAccessResponse {

    /** 链接是否有效 */
    private Boolean valid;

    /** 无效原因（当valid=false时） */
    private String invalidReason;

    /** 档案信息 */
    private ArchiveInfo archive;

    /** 文件列表 */
    private List<FileInfo> files;

    /** 链接信息 */
    private LinkInfo linkInfo;

    /** 借阅人信息 */
    private BorrowerInfo borrower;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveInfo {
        private Long archiveId;
        private String archiveNo;
        private String title;
        private String archiveType;
        private String retentionPeriod;
        private String securityLevel;
        private String caseName;
        private String caseNo;
        private Integer fileCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private Long fileId;
        private String fileName;
        private String fileExtension;
        private Long fileSize;
        private String mimeType;
        private String fileCategory;
        private String previewUrl;
        private String downloadUrl;
        private Boolean isLongTermFormat;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkInfo {
        private Long linkId;
        private LocalDateTime expireAt;
        private Long remainingSeconds;
        private Boolean allowDownload;
        private Integer accessCount;
        private Integer maxAccessCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowerInfo {
        private String userId;
        private String userName;
        private String purpose;
        private String sourceSystem;
    }

    /**
     * 创建无效响应.
     */
    public static BorrowLinkAccessResponse invalid(String reason) {
        return BorrowLinkAccessResponse.builder()
                .valid(false)
                .invalidReason(reason)
                .build();
    }
}
