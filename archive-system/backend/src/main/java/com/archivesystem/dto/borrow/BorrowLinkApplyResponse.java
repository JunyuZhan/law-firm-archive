package com.archivesystem.dto.borrow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 电子借阅链接申请响应DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowLinkApplyResponse {

    /** 链接ID */
    private Long linkId;

    /** 访问令牌 */
    private String accessToken;

    /** 完整访问URL */
    private String accessUrl;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 是否允许下载 */
    private Boolean allowDownload;

    /** 最大访问次数 */
    private Integer maxAccessCount;

    /** 档案号 */
    private String archiveNo;

    /** 档案标题 */
    private String archiveTitle;
}
