package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.common.exception.BadRequestException;
import com.archivesystem.dto.backup.BackupJobResponse;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.dto.backup.BackupTargetSummaryResponse;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@Validated
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
    public Result<List<BackupTargetSummaryResponse>> getTargets() {
        return Result.success(backupService.getTargets().stream().map(BackupTargetSummaryResponse::from).toList());
    }

    @Operation(summary = "获取备份目标")
    @GetMapping("/targets/{id}")
    public Result<BackupTargetResponse> getTarget(@PathVariable @Positive(message = "id 必须为正数") Long id) {
        requirePositive(id, "id");
        return Result.success(backupService.getTarget(id));
    }

    @Operation(summary = "创建备份目标")
    @PostMapping("/targets")
    public Result<BackupTargetSummaryResponse> createTarget(@Valid @RequestBody BackupTargetRequest request) {
        return Result.success("创建成功", BackupTargetSummaryResponse.from(backupService.createTarget(request)));
    }

    @Operation(summary = "更新备份目标")
    @PutMapping("/targets/{id}")
    public Result<BackupTargetSummaryResponse> updateTarget(@PathVariable @Positive(message = "id 必须为正数") Long id,
                                                            @Valid @RequestBody BackupTargetRequest request) {
        requirePositive(id, "id");
        return Result.success("更新成功", BackupTargetSummaryResponse.from(backupService.updateTarget(id, request)));
    }

    @Operation(summary = "删除备份目标")
    @DeleteMapping("/targets/{id}")
    public Result<Void> deleteTarget(@PathVariable @Positive(message = "id 必须为正数") Long id) {
        requirePositive(id, "id");
        backupService.deleteTarget(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "验证备份目标")
    @PostMapping("/targets/{id}/verify")
    public Result<BackupTargetSummaryResponse> verifyTarget(@PathVariable @Positive(message = "id 必须为正数") Long id) {
        requirePositive(id, "id");
        return Result.success("验证完成", BackupTargetSummaryResponse.from(backupService.verifyTarget(id)));
    }

    @Operation(summary = "备份集列表")
    @GetMapping("/sets")
    public Result<List<BackupSetResponse>> getBackupSets(@RequestParam(required = false)
                                                         @Positive(message = "targetId 必须为正数") Long targetId) {
        requirePositiveIfPresent(targetId, "targetId");
        return Result.success(backupService.getBackupSets(targetId));
    }

    @Operation(summary = "备份任务列表")
    @GetMapping("/jobs")
    public Result<PageResult<BackupJobResponse>> getJobs(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1")
            @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        PageResult<BackupJob> result = backupService.getBackupJobs(pageNum, pageSize);
        return Result.success(PageResult.of(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getRecords().stream().map(BackupJobResponse::from).toList()
        ));
    }

    @Operation(summary = "手动执行备份")
    @PostMapping("/run")
    public Result<BackupJobResponse> runBackup(@RequestParam @Positive(message = "targetId 必须为正数") Long targetId) {
        requirePositive(targetId, "targetId");
        BackupJobResponse response = BackupJobResponse.from(backupService.runManualBackup(targetId));
        return Result.success(response.getStatusMessage(), response);
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new BadRequestException(fieldName + " 必须为正数");
        }
    }

    private void requirePositiveIfPresent(Long value, String fieldName) {
        if (value != null && value < 1) {
            throw new BadRequestException(fieldName + " 必须为正数");
        }
    }
}
