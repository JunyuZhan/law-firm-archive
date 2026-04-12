package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 备份中心控制器.
 * @author junyuzhan
 */
@Tag(name = "备份中心", description = "备份目标与备份任务管理")
@RestController
@RequestMapping("/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "备份概览")
    @GetMapping("/overview")
    public Result<BackupOverview> getOverview() {
        return Result.success(backupService.getOverview());
    }

    @Operation(summary = "备份目标列表")
    @GetMapping("/targets")
    public Result<List<BackupTargetResponse>> getTargets() {
        return Result.success(backupService.getTargets());
    }

    @Operation(summary = "获取备份目标")
    @GetMapping("/targets/{id}")
    public Result<BackupTargetResponse> getTarget(@PathVariable Long id) {
        return Result.success(backupService.getTarget(id));
    }

    @Operation(summary = "创建备份目标")
    @PostMapping("/targets")
    public Result<BackupTargetResponse> createTarget(@Valid @RequestBody BackupTargetRequest request) {
        return Result.success("创建成功", backupService.createTarget(request));
    }

    @Operation(summary = "更新备份目标")
    @PutMapping("/targets/{id}")
    public Result<BackupTargetResponse> updateTarget(@PathVariable Long id, @Valid @RequestBody BackupTargetRequest request) {
        return Result.success("更新成功", backupService.updateTarget(id, request));
    }

    @Operation(summary = "删除备份目标")
    @DeleteMapping("/targets/{id}")
    public Result<Void> deleteTarget(@PathVariable Long id) {
        backupService.deleteTarget(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "验证备份目标")
    @PostMapping("/targets/{id}/verify")
    public Result<BackupTargetResponse> verifyTarget(@PathVariable Long id) {
        return Result.success("验证完成", backupService.verifyTarget(id));
    }

    @Operation(summary = "备份集列表")
    @GetMapping("/sets")
    public Result<List<BackupSetResponse>> getBackupSets(@RequestParam(required = false) Long targetId) {
        return Result.success(backupService.getBackupSets(targetId));
    }

    @Operation(summary = "备份任务列表")
    @GetMapping("/jobs")
    public Result<PageResult<BackupJob>> getJobs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(backupService.getBackupJobs(pageNum, pageSize));
    }

    @Operation(summary = "手动执行备份")
    @PostMapping("/run")
    public Result<BackupJob> runBackup(@RequestParam Long targetId) {
        return Result.success("备份任务已启动", backupService.runManualBackup(targetId));
    }
}
