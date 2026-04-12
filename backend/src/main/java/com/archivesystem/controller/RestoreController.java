package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.RestoreExecuteRequest;
import com.archivesystem.dto.backup.RestoreMaintenanceStatus;
import com.archivesystem.entity.RestoreJob;
import com.archivesystem.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 恢复中心控制器.
 * @author junyuzhan
 */
@Tag(name = "恢复中心", description = "恢复任务台账")
@RestController
@RequestMapping("/restores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class RestoreController {

    private final BackupService backupService;

    @Operation(summary = "维护模式状态")
    @GetMapping("/maintenance")
    public Result<RestoreMaintenanceStatus> getMaintenanceStatus() {
        return Result.success(backupService.getRestoreMaintenanceStatus());
    }

    @Operation(summary = "恢复状态", description = "兼容旧版前端/探测调用，返回当前维护模式状态")
    @GetMapping("/status")
    public Result<RestoreMaintenanceStatus> getRestoreStatus() {
        return Result.success(backupService.getRestoreMaintenanceStatus());
    }

    @Operation(summary = "设置维护模式")
    @PutMapping("/maintenance")
    public Result<RestoreMaintenanceStatus> updateMaintenanceStatus(@RequestParam Boolean enabled) {
        return Result.success("维护模式已更新", backupService.setRestoreMaintenanceMode(enabled));
    }

    @Operation(summary = "恢复源备份集列表")
    @GetMapping("/sets")
    public Result<List<BackupSetResponse>> getBackupSets(@RequestParam(required = false) Long targetId) {
        return Result.success(backupService.getBackupSets(targetId));
    }

    @Operation(summary = "执行恢复")
    @PostMapping("/run")
    public Result<RestoreJob> runRestore(@Valid @RequestBody RestoreExecuteRequest request) {
        return Result.success("恢复任务已启动", backupService.runRestore(request));
    }

    @Operation(summary = "恢复任务列表")
    @GetMapping("/jobs")
    public Result<PageResult<RestoreJob>> getJobs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(backupService.getRestoreJobs(pageNum, pageSize));
    }
}
