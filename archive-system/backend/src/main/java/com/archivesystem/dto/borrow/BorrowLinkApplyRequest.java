package com.archivesystem.dto.borrow;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电子借阅链接申请请求DTO.
 * 用于外部系统申请电子借阅链接
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowLinkApplyRequest {

    /** 档案ID（与archiveNo二选一） */
    private Long archiveId;

    /** 档案号（与archiveId二选一） */
    private String archiveNo;

    /** 申请人ID（来源系统的用户ID） */
    @NotBlank(message = "申请人ID不能为空")
    @Size(max = 100, message = "申请人ID长度不能超过100")
    private String userId;

    /** 申请人姓名 */
    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 100, message = "申请人姓名长度不能超过100")
    private String userName;

    /** 借阅目的 */
    @NotBlank(message = "借阅目的不能为空")
    @Size(max = 500, message = "借阅目的长度不能超过500")
    private String purpose;

    /** 有效期天数（默认7天，最长30天） */
    @Min(value = 1, message = "有效期最少1天")
    @Max(value = 30, message = "有效期最长30天")
    @Builder.Default
    private Integer expireDays = 7;

    /** 是否允许下载（默认允许） */
    @Builder.Default
    private Boolean allowDownload = true;

    /** 最大访问次数（NULL不限制） */
    @Min(value = 1, message = "最大访问次数至少为1")
    private Integer maxAccessCount;
}
