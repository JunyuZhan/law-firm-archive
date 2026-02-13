package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计控制器.
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "数据统计接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取概览统计")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(statisticsService.getOverview());
    }

    @GetMapping("/by-type")
    @Operation(summary = "按档案类型统计")
    public Result<List<Map<String, Object>>> countByType() {
        return Result.success(statisticsService.countByArchiveType());
    }

    @GetMapping("/by-retention")
    @Operation(summary = "按保管期限统计")
    public Result<List<Map<String, Object>>> countByRetention() {
        return Result.success(statisticsService.countByRetentionPeriod());
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态统计")
    public Result<List<Map<String, Object>>> countByStatus() {
        return Result.success(statisticsService.countByStatus());
    }

    @GetMapping("/trend")
    @Operation(summary = "月度趋势统计")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return Result.success(statisticsService.countByMonth(year));
    }

    @GetMapping("/borrow")
    @Operation(summary = "借阅统计")
    public Result<Map<String, Object>> getBorrowStats() {
        return Result.success(statisticsService.getBorrowStatistics());
    }

    @GetMapping("/storage")
    @Operation(summary = "存储统计")
    public Result<Map<String, Object>> getStorageStats() {
        return Result.success(statisticsService.getStorageStatistics());
    }
}
