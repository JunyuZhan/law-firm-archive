package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.BackupCommand;
import com.lawfirm.application.system.command.RestoreCommand;
import com.lawfirm.application.system.dto.BackupDTO;
import com.lawfirm.application.system.dto.BackupQueryDTO;
import com.lawfirm.application.system.service.BackupAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统备份 Controller
 */
@Tag(name = "系统备份", description = "系统备份和恢复管理")
@RestController
@RequestMapping("/system/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupAppService backupAppService;

    /**
     * 分页查询备份记录
     */
    @GetMapping("/list")
    @RequirePermission("system:backup:list")
    @Operation(summary = "查询备份记录列表")
    public Result<PageResult<BackupDTO>> listBackups(BackupQueryDTO query) {
        PageResult<BackupDTO> result = backupAppService.listBackups(query);
        return Result.success(result);
    }

    /**
     * 获取备份详情
     */
    @GetMapping("/{id}")
    @RequirePermission("system:backup:view")
    @Operation(summary = "获取备份详情")
    public Result<BackupDTO> getBackup(@PathVariable Long id) {
        BackupDTO backup = backupAppService.getBackupById(id);
        return Result.success(backup);
    }

    /**
     * 创建备份
     */
    @PostMapping
    @RequirePermission("system:backup:create")
    @OperationLog(module = "系统备份", action = "创建备份")
    @Operation(summary = "创建备份")
    public Result<BackupDTO> createBackup(@RequestBody @Valid BackupCommand command) {
        BackupDTO backup = backupAppService.createBackup(command);
        return Result.success(backup);
    }

    /**
     * 恢复备份
     */
    @PostMapping("/restore")
    @RequirePermission("system:backup:restore")
    @OperationLog(module = "系统备份", action = "恢复备份")
    @Operation(summary = "恢复备份")
    public Result<Void> restoreBackup(@RequestBody @Valid RestoreCommand command) {
        backupAppService.restoreBackup(command);
        return Result.success();
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/{id}")
    @RequirePermission("system:backup:delete")
    @OperationLog(module = "系统备份", action = "删除备份")
    @Operation(summary = "删除备份")
    public Result<Void> deleteBackup(@PathVariable Long id) {
        backupAppService.deleteBackup(id);
        return Result.success();
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/{id}/download")
    @RequirePermission("system:backup:download")
    @Operation(summary = "下载备份文件")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long id) {
        Resource resource = backupAppService.downloadBackup(id);
        
        String filename = resource.getFilename();
        if (filename == null) {
            filename = "backup_" + id + ".sql";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 导入外部备份文件
     */
    @PostMapping("/import")
    @RequirePermission("system:backup:create")
    @OperationLog(module = "系统备份", action = "导入备份")
    @Operation(summary = "导入外部备份文件", description = "上传外部备份文件（.sql 或 pg_dump 自定义格式），导入后可用于恢复")
    public Result<BackupDTO> importBackup(
            @Parameter(description = "备份文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "备份类型") @RequestParam(defaultValue = "DATABASE") String backupType,
            @Parameter(description = "备份说明") @RequestParam(required = false) String description) {
        BackupDTO backup = backupAppService.importBackup(file, backupType, description);
        return Result.success(backup);
    }
}

