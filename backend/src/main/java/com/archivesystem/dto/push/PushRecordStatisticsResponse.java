package com.archivesystem.dto.push;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 推送记录统计响应 DTO.
 */
@Value
@Builder
public class PushRecordStatisticsResponse {

    long total;
    long today;
    long pending;
    long processing;
    long success;
    long failed;
    long partial;

    public static PushRecordStatisticsResponse from(Map<String, Object> stats) {
        return PushRecordStatisticsResponse.builder()
                .total(getLong(stats, "total"))
                .today(getLong(stats, "today"))
                .pending(getLong(stats, "pending"))
                .processing(getLong(stats, "processing"))
                .success(getLong(stats, "success"))
                .failed(getLong(stats, "failed"))
                .partial(getLong(stats, "partial"))
                .build();
    }

    private static long getLong(Map<String, Object> stats, String key) {
        if (stats == null) {
            return 0L;
        }
        Object value = stats.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
