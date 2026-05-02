package com.archivesystem.dto.category;

import lombok.Builder;
import lombok.Value;

/**
 * 分类统计响应 DTO.
 */
@Value
@Builder
public class CategoryStatisticsResponse {

    long archiveCount;
}
