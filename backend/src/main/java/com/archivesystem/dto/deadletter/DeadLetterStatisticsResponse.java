package com.archivesystem.dto.deadletter;

import com.archivesystem.entity.DeadLetterRecord;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 死信消息统计响应 DTO.
 */
@Value
@Builder
public class DeadLetterStatisticsResponse {

    int pending;
    int retrying;
    int success;
    int failed;
    int ignored;

    public static DeadLetterStatisticsResponse from(Map<String, Integer> stats) {
        return DeadLetterStatisticsResponse.builder()
                .pending(get(stats, DeadLetterRecord.STATUS_PENDING))
                .retrying(get(stats, DeadLetterRecord.STATUS_RETRYING))
                .success(get(stats, DeadLetterRecord.STATUS_SUCCESS))
                .failed(get(stats, DeadLetterRecord.STATUS_FAILED))
                .ignored(get(stats, DeadLetterRecord.STATUS_IGNORED))
                .build();
    }

    private static int get(Map<String, Integer> stats, String key) {
        if (stats == null) {
            return 0;
        }
        return stats.getOrDefault(key, 0);
    }
}
