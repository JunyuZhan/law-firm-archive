package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.Archive;
import com.archivesystem.service.RetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 保管期限管理控制器.
 * @author junyuzhan
 */
@RestController
@RequestMapping("/retention")
@RequiredArgsConstructor
@Tag(name = "保管期限管理", description = "到期预警、期限延长")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
public class RetentionController {

    private final RetentionService retentionService;

    /**
     * 获取即将到期的档案列表.
     */
    @GetMapping("/expiring")
    @Operation(summary = "获取即将到期的档案")
    public Result<List<Archive>> getExpiringArchives(
            @RequestParam(defaultValue = "90") Integer days) {
        List<Archive> archives = retentionService.findExpiringArchives(days);
        return Result.success(archives);
    }

    /**
     * 获取已到期的档案列表.
     */
    @GetMapping("/expired")
    @Operation(summary = "获取已到期的档案")
    public Result<List<Archive>> getExpiredArchives() {
        List<Archive> archives = retentionService.findExpiredArchives();
        return Result.success(archives);
    }

    /**
     * 延长档案保管期限.
     */
    @PutMapping("/{archiveId}/extend")
    @Operation(summary = "延长保管期限")
    public Result<Void> extendRetention(
            @PathVariable Long archiveId,
            @RequestBody(required = false) Map<String, String> params) {
        if (params == null) {
            return Result.error("400", "请求参数不能为空");
        }
        String newRetentionPeriod = params.get("newRetentionPeriod");
        if (!StringUtils.hasText(newRetentionPeriod)) {
            return Result.error("400", "新保管期限不能为空");
        }
        String reason = params.get("reason");
        retentionService.extendRetention(archiveId, newRetentionPeriod, reason);
        return Result.success("保管期限已延长", null);
    }

    /**
     * 申请档案销毁.
     */
    @PostMapping("/{archiveId}/destruction/apply")
    @Operation(summary = "申请档案销毁")
    public Result<Void> applyForDestruction(
            @PathVariable Long archiveId,
            @RequestBody(required = false) Map<String, String> params) {
        String reason = params != null ? params.get("reason") : null;
        if (!StringUtils.hasText(reason)) {
            return Result.error("400", "销毁原因不能为空");
        }
        retentionService.applyForDestruction(archiveId, reason);
        return Result.success("销毁申请已提交", null);
    }

    /**
     * 执行档案销毁.
     */
    @PutMapping("/{archiveId}/destruction/execute")
    @Operation(summary = "执行档案销毁")
    public Result<Void> executeDestruction(
            @PathVariable Long archiveId,
            @RequestBody(required = false) Map<String, Object> params) {
        if (params == null) {
            return Result.error("400", "请求参数不能为空");
        }
        
        Long approverId = null;
        Object approverIdObj = params.get("approverId");
        if (approverIdObj != null) {
            try {
                approverId = Long.valueOf(approverIdObj.toString());
            } catch (NumberFormatException e) {
                return Result.error("400", "approverId 格式错误");
            }
        }
        
        String remarks = params.get("remarks") != null ? params.get("remarks").toString() : null;
        retentionService.executeDestruction(archiveId, approverId, remarks);
        return Result.success("档案已销毁", null);
    }
}
