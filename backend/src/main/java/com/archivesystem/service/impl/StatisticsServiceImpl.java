package com.archivesystem.service.impl;

import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 统计服务实现.
 * @author junyuzhan
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final Map<String, String> ARCHIVE_TYPE_NAMES = orderedMap(new String[][]{
            {"DOCUMENT", "文书档案"},
            {"SCIENCE", "科技档案"},
            {"ACCOUNTING", "会计档案"},
            {"PERSONNEL", "人事档案"},
            {"SPECIAL", "专业档案"},
            {"AUDIOVISUAL", "声像档案"}
    });

    private static final Map<String, String> RETENTION_PERIOD_NAMES = orderedMap(new String[][]{
            {"PERMANENT", "永久"},
            {"Y30", "30年"},
            {"Y15", "15年"},
            {"Y10", "10年"},
            {"Y5", "5年"}
    });

    private static final Map<String, String> STATUS_NAMES = orderedMap(new String[][]{
            {"RECEIVED", "已接收"},
            {"CATALOGING", "整理中"},
            {"STORED", "已归档"},
            {"BORROWED", "借出中"}
    });

    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final BorrowApplicationMapper borrowMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // 档案总数
        long totalArchives = archiveMapper.selectCount(
                new LambdaQueryWrapper<Archive>().eq(Archive::getDeleted, false));
        result.put("totalArchives", totalArchives);

        // 本月新增
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthlyNew = archiveMapper.selectCount(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getDeleted, false)
                        .ge(Archive::getCreatedAt, monthStart));
        result.put("monthlyNew", monthlyNew);

        // 电子文件数
        long totalFiles = digitalFileMapper.selectCount(
                new LambdaQueryWrapper<DigitalFile>().eq(DigitalFile::getDeleted, false));
        result.put("totalFiles", totalFiles);

        // 借阅中
        long borrowing = borrowMapper.selectCount(
                new LambdaQueryWrapper<BorrowApplication>()
                        .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_BORROWED)
                        .eq(BorrowApplication::getDeleted, false));
        result.put("borrowing", borrowing);

        // 待审批
        long pendingApproval = borrowMapper.selectCount(
                new LambdaQueryWrapper<BorrowApplication>()
                        .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_PENDING)
                        .eq(BorrowApplication::getDeleted, false));
        result.put("pendingApproval", pendingApproval);

        return result;
    }

    @Override
    public List<Map<String, Object>> countByArchiveType() {
        return buildCategorizedCounts(ARCHIVE_TYPE_NAMES, archiveMapper.countByArchiveType(), "type");
    }

    @Override
    public List<Map<String, Object>> countByRetentionPeriod() {
        return buildCategorizedCounts(RETENTION_PERIOD_NAMES, archiveMapper.countByRetentionPeriod(), "period");
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        return buildCategorizedCounts(STATUS_NAMES, archiveMapper.countByStatus(), "status");
    }

    @Override
    public List<Map<String, Object>> countByMonth(int year) {
        List<Map<String, Object>> result = new ArrayList<>();

        Map<Integer, Long> monthlyCounts = new HashMap<>();
        for (Map<String, Object> row : archiveMapper.countByMonth(year)) {
            monthlyCounts.put(asInt(row.get("month")), asLong(row.get("count")));
        }

        for (int month = 1; month <= 12; month++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("monthName", month + "月");
            item.put("count", monthlyCounts.getOrDefault(month, 0L));
            result.add(item);
        }

        return result;
    }

    @Override
    public Map<String, Object> getBorrowStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 总借阅次数
        long totalBorrows = borrowMapper.selectCount(
                new LambdaQueryWrapper<BorrowApplication>()
                        .in(BorrowApplication::getStatus, 
                            BorrowApplication.STATUS_BORROWED, 
                            BorrowApplication.STATUS_RETURNED)
                        .eq(BorrowApplication::getDeleted, false));
        result.put("totalBorrows", totalBorrows);

        // 本月借阅 (使用 created_at 作为申请时间)
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthlyBorrows = borrowMapper.selectCount(
                new LambdaQueryWrapper<BorrowApplication>()
                        .ge(BorrowApplication::getCreatedAt, monthStart)
                        .eq(BorrowApplication::getDeleted, false));
        result.put("monthlyBorrows", monthlyBorrows);

        // 逾期数量
        long overdue = borrowMapper.selectCount(
                new LambdaQueryWrapper<BorrowApplication>()
                        .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_BORROWED)
                        .lt(BorrowApplication::getExpectedReturnDate, LocalDate.now())
                        .eq(BorrowApplication::getDeleted, false));
        result.put("overdue", overdue);

        return result;
    }

    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> storageSummary = digitalFileMapper.selectStorageSummary();
        long totalSize = asLong(storageSummary != null ? storageSummary.get("totalSize") : null);
        long fileCount = asLong(storageSummary != null ? storageSummary.get("fileCount") : null);

        result.put("totalSize", totalSize);
        result.put("totalSizeFormatted", formatFileSize(totalSize));
        result.put("fileCount", fileCount);

        return result;
    }

    @Override
    public List<Map<String, Object>> getScanBatchStatistics(String keyword) {
        List<Map<String, Object>> rows = digitalFileMapper.selectScanBatchSummaries(keyword);
        if (rows == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            long fileCount = asLong(row.get("fileCount"));
            long scannedFileCount = asLong(row.get("scannedFileCount"));
            long passedCount = asLong(row.get("passedCount"));
            long pendingCount = asLong(row.get("pendingCount"));
            long failedCount = asLong(row.get("failedCount"));
            long reviewedCount = passedCount + failedCount;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scanBatchNo", row.get("scanBatchNo"));
            item.put("fileCount", fileCount);
            item.put("archiveCount", asLong(row.get("archiveCount")));
            item.put("scannedFileCount", scannedFileCount);
            item.put("passedCount", passedCount);
            item.put("pendingCount", pendingCount);
            item.put("failedCount", failedCount);
            item.put("reviewedCount", reviewedCount);
            item.put("scanCoverageRate", calculateRate(scannedFileCount, fileCount));
            item.put("reviewCompletionRate", calculateRate(reviewedCount, fileCount));
            item.put("passRate", calculateRate(passedCount, reviewedCount));
            item.put("latestScanTime", row.get("latestScanTime"));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildCategorizedCounts(Map<String, String> labels,
                                                             List<Map<String, Object>> rows,
                                                             String keyName) {
        Map<String, Long> countMap = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object key = row.get(keyName);
                if (key != null) {
                    countMap.put(String.valueOf(key), asLong(row.get("count")));
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(labels.size());
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(keyName, entry.getKey());
            item.put("name", entry.getValue());
            item.put("count", countMap.getOrDefault(entry.getKey(), 0L));
            result.add(item);
        }
        return result;
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return numerator * 100D / denominator;
    }

    private static Map<String, String> orderedMap(String[][] entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return Collections.unmodifiableMap(result);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / 1024.0 / 1024.0);
        return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
    }
}
