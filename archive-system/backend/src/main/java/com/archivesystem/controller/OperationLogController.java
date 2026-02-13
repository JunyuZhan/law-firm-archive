package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器.
 */
@Tag(name = "操作日志", description = "操作日志查询与导出")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询日志")
    @GetMapping
    public Result<PageResult<OperationLog>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(operationLogService.query(keyword, objectType, operationType, 
                operatorId, startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "根据档案ID查询日志")
    @GetMapping("/archive/{archiveId}")
    public Result<List<OperationLog>> getByArchive(@PathVariable Long archiveId) {
        return Result.success(operationLogService.getByArchiveId(archiveId));
    }

    @Operation(summary = "根据对象查询日志")
    @GetMapping("/object/{objectType}/{objectId}")
    public Result<List<OperationLog>> getByObject(
            @PathVariable String objectType,
            @PathVariable String objectId) {
        return Result.success(operationLogService.getByObject(objectType, objectId));
    }

    @Operation(summary = "获取操作统计")
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(operationLogService.getOperationStatistics(startDate, endDate));
    }

    @Operation(summary = "导出日志")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        byte[] data = operationLogService.exportLogs(objectType, operationType, operatorId, startDate, endDate);
        
        String filename = "操作日志_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }
}
