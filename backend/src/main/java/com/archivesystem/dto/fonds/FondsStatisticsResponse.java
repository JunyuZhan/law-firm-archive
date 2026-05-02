package com.archivesystem.dto.fonds;

import lombok.Builder;
import lombok.Value;

/**
 * 全宗统计响应 DTO.
 */
@Value
@Builder
public class FondsStatisticsResponse {

    long archiveCount;
}
