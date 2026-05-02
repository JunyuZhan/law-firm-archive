package com.archivesystem.dto.statistics;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 月度趋势统计响应 DTO.
 */
@Value
@Builder
public class MonthlyTrendStatisticsResponse {

    int month;
    String monthName;
    long count;

    public static MonthlyTrendStatisticsResponse from(Map<String, Object> data) {
        return MonthlyTrendStatisticsResponse.builder()
                .month(asInt(data, "month"))
                .monthName(asString(data, "monthName"))
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

    private static int asInt(Map<String, Object> data, String key) {
        if (data == null) {
            return 0;
        }
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String asString(Map<String, Object> data, String key) {
        if (data == null || data.get(key) == null) {
            return null;
        }
        return String.valueOf(data.get(key));
    }
}
