package com.archivesystem.dto.statistics;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 借阅统计响应 DTO.
 */
@Value
@Builder
public class BorrowStatisticsResponse {

    long totalBorrows;
    long monthlyBorrows;
    long overdue;

    public static BorrowStatisticsResponse from(Map<String, Object> data) {
        return BorrowStatisticsResponse.builder()
                .totalBorrows(asLong(data, "totalBorrows"))
                .monthlyBorrows(asLong(data, "monthlyBorrows"))
                .overdue(asLong(data, "overdue"))
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
