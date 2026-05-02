package com.archivesystem.dto.borrow;

import lombok.Builder;
import lombok.Value;

/**
 * 借阅链接过期状态更新响应 DTO.
 */
@Value
@Builder
public class BorrowLinkUpdateExpiredResponse {

    int updatedCount;
}
