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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 系统备份 Controller */
@Tag(name = "系统备份", description = "系统备份和恢复管理")
@RestController
@RequestMapping("/system/backup")
@RequiredArgsConstructor
public class BackupController {

  /** 备份应用服务 */
  private final BackupAppService backupAppService;

  /**
   * 分页查询备份记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("system:backup:list")
  @Operation(summary = "查询备份记录列表")
  public Result<PageResult<BackupDTO>> listBackups(final BackupQueryDTO query) {
    PageResult<BackupDTO> result = backupAppService.listBackups(query);
    return Result.success(result);
  }

  /**
   * 获取备份详情
   *
   * @param id 备份ID
   * @return 备份详情
   */
  @GetMapping("/{id}")
  @RequirePermission("system:backup:view")
  @Operation(summary = "获取备份详情")
  public Result<BackupDTO> getBackup(@PathVariable final Long id) {
    BackupDTO backup = backupAppService.getBackupById(id);
    return Result.success(backup);
  }

  /**
   * 创建备份
   *
   * @param command 创建命令
   * @return 备份详情
   */
  @PostMapping
  @RequirePermission("system:backup:create")
  @OperationLog(module = "系统备份", action = "创建备份")
  @Operation(summary = "创建备份")
  public Result<BackupDTO> createBackup(@RequestBody @Valid final BackupCommand command) {
    BackupDTO backup = backupAppService.createBackup(command);
    return Result.success(backup);
  }

  /**
   * 恢复备份
   *
   * @param command 恢复命令
   * @return 无返回
   */
  @PostMapping("/restore")
  @RequirePermission("system:backup:restore")
  @OperationLog(module = "系统备份", action = "恢复备份")
  @Operation(summary = "恢复备份")
  public Result<Void> restoreBackup(@RequestBody @Valid final RestoreCommand command) {
    backupAppService.restoreBackup(command);
    return Result.success();
  }

  /**
   * 删除备份
   *
   * @param id 备份ID
   * @return 无返回
   */
  @DeleteMapping("/{id}")
  @RequirePermission("system:backup:delete")
  @OperationLog(module = "系统备份", action = "删除备份")
  @Operation(summary = "删除备份")
  public Result<Void> deleteBackup(@PathVariable final Long id) {
    backupAppService.deleteBackup(id);
    return Result.success();
  }

  /**
   * 下载备份文件
   *
   * @param id 备份ID
   * @return 备份文件
   */
  @GetMapping("/{id}/download")
  @RequirePermission("system:backup:download")
  @Operation(summary = "下载备份文件")
  public ResponseEntity<Resource> downloadBackup(@PathVariable final Long id) {
    Resource resource = backupAppService.downloadBackup(id);

    String filename = resource.getFilename();
    if (filename == null) {
      filename = "backup_" + id + ".sql";
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(resource);
  }

  /**
   * 导入外部备份文件
   *
   * @param file 备份文件
   * @param backupType 备份类型
   * @param description 备份说明
   * @return 备份详情
   */
  @PostMapping("/import")
  @RequirePermission("system:backup:create")
  @OperationLog(module = "系统备份", action = "导入备份")
  @Operation(summary = "导入外部备份文件", description = "上传外部备份文件（.sql 或 pg_dump 自定义格式），导入后可用于恢复")
  public Result<BackupDTO> importBackup(
      @Parameter(description = "备份文件") @RequestParam("file") final MultipartFile file,
      @Parameter(description = "备份类型") @RequestParam(defaultValue = "DATABASE")
          final String backupType,
      @Parameter(description = "备份说明") @RequestParam(required = false) final String description) {
    BackupDTO backup = backupAppService.importBackup(file, backupType, description);
    return Result.success(backup);
  }
}
