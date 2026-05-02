package com.archivesystem.dto.statistics;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 概览统计响应 DTO.
 */
@Value
@Builder
public class OverviewStatisticsResponse {

    long totalArchives;
    long monthlyNew;
    long totalFiles;
    long borrowing;
    long pendingApproval;

    public static OverviewStatisticsResponse from(Map<String, Object> data) {
        return OverviewStatisticsResponse.builder()
                .totalArchives(asLong(data, "totalArchives"))
                .monthlyNew(asLong(data, "monthlyNew"))
                .totalFiles(asLong(data, "totalFiles"))
                .borrowing(asLong(data, "borrowing"))
                .pendingApproval(asLong(data, "pendingApproval"))
                .build();
    }

    private static long asLong(Map<String, Object> data, String key) {
        if (data == null) {
            return 0L;
        }
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
