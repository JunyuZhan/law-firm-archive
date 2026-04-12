package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.service.PushRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 推送记录控制器.
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/push-records")
@RequiredArgsConstructor
@Tag(name = "推送记录管理", description = "管理外部系统推送的档案记录")
public class PushRecordController {

    private final PushRecordService pushRecordService;

    @GetMapping
    @Operation(summary = "分页查询推送记录")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<IPage<PushRecord>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "来源类型") @RequestParam(required = false) String sourceType,
            @Parameter(description = "推送状态") @RequestParam(required = false) String pushStatus,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "推送批次号") @RequestParam(required = false) String pushBatchNo,
            @Parameter(description = "推送时间开始") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pushedAtStart,
            @Parameter(description = "推送时间结束") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pushedAtEnd) {
        
        Page<PushRecord> page = new Page<>(pageNum, pageSize);
        IPage<PushRecord> result = pushRecordService.page(page, sourceType, pushStatus, keyword, pushBatchNo, pushedAtStart, pushedAtEnd);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询推送记录详情")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<PushRecord> getById(@PathVariable Long id) {
        PushRecord record = pushRecordService.getById(id);
        if (record == null) {
            return Result.error("404", "推送记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取推送统计数据")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> stats = pushRecordService.getStatistics();
        return Result.success(stats);
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待处理的推送记录")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<PushRecord>> getPending() {
        List<PushRecord> records = pushRecordService.getPendingRecords();
        return Result.success(records);
    }

    @GetMapping("/failed")
    @Operation(summary = "获取失败的推送记录")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<PushRecord>> getFailed() {
        List<PushRecord> records = pushRecordService.getFailedRecords();
        return Result.success(records);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试推送")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<String> retry(@PathVariable Long id) {
        pushRecordService.retry(id);
        return Result.success("已重置为待处理状态", null);
    }
}
