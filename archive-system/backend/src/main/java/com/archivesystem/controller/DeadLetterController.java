package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.DeadLetterRecord;
import com.archivesystem.service.DeadLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 死信消息管理接口.
 */
@RestController
@RequestMapping("/dead-letters")
@RequiredArgsConstructor
@Tag(name = "死信消息管理", description = "处理消息队列中的失败消息")
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    /**
     * 获取死信消息列表.
     */
    @GetMapping
    @Operation(summary = "获取死信消息列表")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<PageResult<DeadLetterRecord>> list(
            @RequestParam(required = false) @Parameter(description = "状态筛选") String status,
            @RequestParam(required = false) @Parameter(description = "队列名称") String queueName,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        PageResult<DeadLetterRecord> result = deadLetterService.getList(status, queueName, page, size);
        return Result.success(result);
    }

    /**
     * 获取死信消息详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取死信消息详情")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<DeadLetterRecord> getById(@PathVariable Long id) {
        DeadLetterRecord record = deadLetterService.getById(id);
        return Result.success(record);
    }

    /**
     * 获取统计信息.
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Map<String, Integer>> getStatistics() {
        Map<String, Integer> stats = deadLetterService.getStatistics();
        return Result.success(stats);
    }

    /**
     * 重试处理消息.
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "重试处理消息")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Boolean> retry(@PathVariable Long id) {
        boolean success = deadLetterService.retry(id);
        return success ? Result.success("重试请求已发送", true) 
                       : Result.error("RETRY_FAILED", "重试失败");
    }

    /**
     * 批量重试.
     */
    @PostMapping("/batch-retry")
    @Operation(summary = "批量重试")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Integer> batchRetry(@RequestBody Long[] ids) {
        int successCount = deadLetterService.batchRetry(ids);
        return Result.success(String.format("成功重试 %d 条消息", successCount), successCount);
    }

    /**
     * 忽略消息.
     */
    @PostMapping("/{id}/ignore")
    @Operation(summary = "忽略消息")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> ignore(
            @PathVariable Long id,
            @RequestParam(required = false) @Parameter(description = "处理备注") String remark) {
        deadLetterService.ignore(id, remark);
        return Result.success("已忽略", null);
    }

    /**
     * 批量忽略.
     */
    @PostMapping("/batch-ignore")
    @Operation(summary = "批量忽略")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Integer> batchIgnore(
            @RequestBody Long[] ids,
            @RequestParam(required = false) @Parameter(description = "处理备注") String remark) {
        int successCount = deadLetterService.batchIgnore(ids, remark);
        return Result.success(String.format("成功忽略 %d 条消息", successCount), successCount);
    }

    /**
     * 手动触发自动重试.
     */
    @PostMapping("/auto-retry")
    @Operation(summary = "触发自动重试")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Integer> triggerAutoRetry(
            @RequestParam(defaultValue = "10") @Parameter(description = "最大处理数量") int limit) {
        int successCount = deadLetterService.autoRetry(limit);
        return Result.success(String.format("自动重试完成，成功 %d 条", successCount), successCount);
    }
}
