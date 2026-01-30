package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.MigrationDTO;
import com.lawfirm.application.system.service.MigrationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 数据库迁移管理接口 */
@Tag(name = "数据库迁移", description = "数据库迁移脚本管理")
@RestController
@RequestMapping("/system/migration")
@RequiredArgsConstructor
public class MigrationController {

  /** 迁移应用服务 */
  private final MigrationAppService migrationAppService;

  /**
   * 扫描迁移脚本
   *
   * @return 迁移脚本列表
   */
  @Operation(summary = "扫描迁移脚本")
  @GetMapping("/scan")
  @RequirePermission("sys:migration:list")
  public Result<List<MigrationDTO>> scanScripts() {
    return Result.success(migrationAppService.scanMigrationScripts());
  }

  /**
   * 查询迁移记录列表
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 迁移记录分页结果
   */
  @Operation(summary = "查询迁移记录列表")
  @GetMapping("/list")
  @RequirePermission("sys:migration:list")
  public Result<PageResult<MigrationDTO>> listMigrations(
      @RequestParam(defaultValue = "1") final int pageNum,
      @RequestParam(defaultValue = "20") final int pageSize) {
    return Result.success(migrationAppService.listMigrations(pageNum, pageSize));
  }

  /**
   * 获取迁移详情
   *
   * @param id 迁移ID
   * @return 迁移详情
   */
  @Operation(summary = "获取迁移详情")
  @GetMapping("/{id}")
  @RequirePermission("sys:migration:view")
  public Result<MigrationDTO> getMigration(@PathVariable final Long id) {
    return Result.success(migrationAppService.getMigrationById(id));
  }

  /**
   * 执行迁移脚本
   *
   * @param version 迁移版本
   * @param confirmCode 确认码
   * @return 迁移结果
   */
  @Operation(summary = "执行迁移脚本")
  @PostMapping("/execute/{version}")
  @RequirePermission("sys:migration:execute")
  @OperationLog(module = "数据库迁移", action = "执行迁移脚本")
  public Result<MigrationDTO> executeMigration(
      @PathVariable final String version, @RequestParam final String confirmCode) {
    return Result.success(migrationAppService.executeMigration(version, confirmCode));
  }
}
