package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.archive.ArchiveQueryRequest;
import com.archivesystem.dto.statistics.ArchiveTypeStatisticsResponse;
import com.archivesystem.dto.statistics.BorrowStatisticsResponse;
import com.archivesystem.dto.statistics.MonthlyTrendStatisticsResponse;
import com.archivesystem.dto.statistics.OverviewStatisticsResponse;
import com.archivesystem.dto.statistics.RetentionStatisticsResponse;
import com.archivesystem.dto.statistics.ScanBatchStatisticsResponse;
import com.archivesystem.dto.statistics.StatusStatisticsResponse;
import com.archivesystem.dto.statistics.StorageStatisticsResponse;
import com.archivesystem.service.ReportService;
import com.archivesystem.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计控制器.
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "数据统计接口")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final ReportService reportService;

    @GetMapping("/overview")
    @Operation(summary = "获取概览统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<OverviewStatisticsResponse> getOverview() {
        return Result.success(OverviewStatisticsResponse.from(statisticsService.getOverview()));
    }

    @GetMapping("/by-type")
    @Operation(summary = "按档案类型统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<List<ArchiveTypeStatisticsResponse>> countByType() {
        return Result.success(statisticsService.countByArchiveType().stream()
                .map(ArchiveTypeStatisticsResponse::from)
                .toList());
    }

    @GetMapping("/by-retention")
    @Operation(summary = "按保管期限统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<List<RetentionStatisticsResponse>> countByRetention() {
        return Result.success(statisticsService.countByRetentionPeriod().stream()
                .map(RetentionStatisticsResponse::from)
                .toList());
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<List<StatusStatisticsResponse>> countByStatus() {
        return Result.success(statisticsService.countByStatus().stream()
                .map(StatusStatisticsResponse::from)
                .toList());
    }

    @GetMapping("/trend")
    @Operation(summary = "月度趋势统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<List<MonthlyTrendStatisticsResponse>> getTrend(
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return Result.success(statisticsService.countByMonth(year).stream()
                .map(MonthlyTrendStatisticsResponse::from)
                .toList());
    }

    @GetMapping("/borrow")
    @Operation(summary = "借阅统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<BorrowStatisticsResponse> getBorrowStats() {
        return Result.success(BorrowStatisticsResponse.from(statisticsService.getBorrowStatistics()));
    }

    @GetMapping("/storage")
    @Operation(summary = "存储统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<StorageStatisticsResponse> getStorageStats() {
        return Result.success(StorageStatisticsResponse.from(statisticsService.getStorageStatistics()));
    }

    @GetMapping("/scan-batches")
    @Operation(summary = "扫描批次统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public Result<List<ScanBatchStatisticsResponse>> getScanBatchStats(
            @RequestParam(required = false) String keyword) {
        return Result.success(statisticsService.getScanBatchStatistics(keyword).stream()
                .map(ScanBatchStatisticsResponse::from)
                .toList());
    }

    // ===== 报表导出接口 =====

    @GetMapping("/export/overview")
    @Operation(summary = "导出统计概览报表", description = "导出Excel格式的统计概览报表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public void exportOverview(
            @RequestParam(required = false) Integer year,
            HttpServletResponse response) throws IOException {
        setExcelResponse(response, "档案统计概览报表");
        reportService.exportOverviewReport(year, response.getOutputStream());
    }

    @GetMapping("/export/archives")
    @Operation(summary = "导出档案清单", description = "导出Excel格式的档案清单")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public void exportArchiveList(
            ArchiveQueryRequest request,
            HttpServletResponse response) throws IOException {
        setExcelResponse(response, "档案清单");
        reportService.exportArchiveList(request, response.getOutputStream());
    }

    @GetMapping("/export/borrow")
    @Operation(summary = "导出借阅报表", description = "导出Excel格式的借阅统计报表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_REVIEWER', 'ARCHIVE_MANAGER')")
    public void exportBorrowReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {
        setExcelResponse(response, "借阅统计报表");
        reportService.exportBorrowReport(startDate, endDate, response.getOutputStream());
    }

    @GetMapping("/export/operation-log")
    @Operation(summary = "导出操作日志", description = "导出Excel格式的操作日志报表")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void exportOperationLog(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {
        setExcelResponse(response, "操作日志报表");
        reportService.exportOperationLogReport(startDate, endDate, response.getOutputStream());
    }

    /**
     * 设置Excel下载响应头
     */
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encodedFileName = URLEncoder.encode(fileName + "_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
