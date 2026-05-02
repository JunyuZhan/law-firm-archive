package com.archivesystem.dto.statistics;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 保管期限统计响应 DTO.
 */
@Value
@Builder
public class RetentionStatisticsResponse {

    String period;
    String name;
    long count;

    public static RetentionStatisticsResponse from(Map<String, Object> data) {
        return RetentionStatisticsResponse.builder()
                .period(asString(data, "period"))
                .name(asString(data, "name"))
                .count(asLong(data, "count"))
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

    private static String asString(Map<String, Object> data, String key) {
        if (data == null || data.get(key) == null) {
            return null;
        }
        return String.valueOf(data.get(key));
    }
}
