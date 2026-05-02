package com.archivesystem.dto.statistics;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 扫描批次统计响应 DTO.
 */
@Value
@Builder
public class ScanBatchStatisticsResponse {

    String scanBatchNo;
    long fileCount;
    long archiveCount;
    long scannedFileCount;
    long passedCount;
    long pendingCount;
    long failedCount;
    long reviewedCount;
    double scanCoverageRate;
    double reviewCompletionRate;
    double passRate;
    Object latestScanTime;

    public static ScanBatchStatisticsResponse from(Map<String, Object> data) {
        return ScanBatchStatisticsResponse.builder()
                .scanBatchNo(asString(data, "scanBatchNo"))
                .fileCount(asLong(data, "fileCount"))
                .archiveCount(asLong(data, "archiveCount"))
                .scannedFileCount(asLong(data, "scannedFileCount"))
                .passedCount(asLong(data, "passedCount"))
                .pendingCount(asLong(data, "pendingCount"))
                .failedCount(asLong(data, "failedCount"))
                .reviewedCount(asLong(data, "reviewedCount"))
                .scanCoverageRate(asDouble(data, "scanCoverageRate"))
                .reviewCompletionRate(asDouble(data, "reviewCompletionRate"))
                .passRate(asDouble(data, "passRate"))
                .latestScanTime(data == null ? null : data.get("latestScanTime"))
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

    private static double asDouble(Map<String, Object> data, String key) {
        if (data == null) {
            return 0D;
        }
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0D;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static String asString(Map<String, Object> data, String key) {
        if (data == null || data.get(key) == null) {
            return null;
        }
        return String.valueOf(data.get(key));
    }
}
