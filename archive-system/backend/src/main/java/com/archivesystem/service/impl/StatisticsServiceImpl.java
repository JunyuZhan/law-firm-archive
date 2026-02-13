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
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

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
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, String> typeNames = Map.of(
                "DOCUMENT", "文书档案",
                "SCIENCE", "科技档案",
                "ACCOUNTING", "会计档案",
                "PERSONNEL", "人事档案",
                "SPECIAL", "专业档案",
                "AUDIOVISUAL", "声像档案"
        );

        for (Map.Entry<String, String> entry : typeNames.entrySet()) {
            long count = archiveMapper.selectCount(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getArchiveType, entry.getKey())
                            .eq(Archive::getDeleted, false));
            
            Map<String, Object> item = new HashMap<>();
            item.put("type", entry.getKey());
            item.put("name", entry.getValue());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> countByRetentionPeriod() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, String> periodNames = Map.of(
                "PERMANENT", "永久",
                "Y30", "30年",
                "Y15", "15年",
                "Y10", "10年",
                "Y5", "5年"
        );

        for (Map.Entry<String, String> entry : periodNames.entrySet()) {
            long count = archiveMapper.selectCount(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getRetentionPeriod, entry.getKey())
                            .eq(Archive::getDeleted, false));
            
            Map<String, Object> item = new HashMap<>();
            item.put("period", entry.getKey());
            item.put("name", entry.getValue());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, String> statusNames = Map.of(
                "RECEIVED", "已接收",
                "CATALOGING", "整理中",
                "STORED", "已归档",
                "BORROWED", "借出中"
        );

        for (Map.Entry<String, String> entry : statusNames.entrySet()) {
            long count = archiveMapper.selectCount(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getStatus, entry.getKey())
                            .eq(Archive::getDeleted, false));
            
            Map<String, Object> item = new HashMap<>();
            item.put("status", entry.getKey());
            item.put("name", entry.getValue());
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> countByMonth(int year) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime end = start.plusMonths(1);

            long count = archiveMapper.selectCount(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getDeleted, false)
                            .ge(Archive::getCreatedAt, start)
                            .lt(Archive::getCreatedAt, end));

            Map<String, Object> item = new HashMap<>();
            item.put("month", month);
            item.put("monthName", month + "月");
            item.put("count", count);
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

        // 总文件大小
        List<DigitalFile> files = digitalFileMapper.selectList(
                new LambdaQueryWrapper<DigitalFile>()
                        .eq(DigitalFile::getDeleted, false)
                        .select(DigitalFile::getFileSize));

        long totalSize = files.stream()
                .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
                .sum();

        result.put("totalSize", totalSize);
        result.put("totalSizeFormatted", formatFileSize(totalSize));
        result.put("fileCount", files.size());

        return result;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / 1024.0 / 1024.0);
        return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
    }
}
