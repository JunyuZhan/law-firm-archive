package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.MigrationDTO;
import com.lawfirm.application.system.service.MigrationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库迁移管理接口
 */
@Tag(name = "数据库迁移", description = "数据库迁移脚本管理")
@RestController
@RequestMapping("/system/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationAppService migrationAppService;

    @Operation(summary = "扫描迁移脚本")
    @GetMapping("/scan")
    @RequirePermission("sys:migration:list")
    public Result<List<MigrationDTO>> scanScripts() {
        return Result.success(migrationAppService.scanMigrationScripts());
    }

    @Operation(summary = "查询迁移记录列表")
    @GetMapping("/list")
    @RequirePermission("sys:migration:list")
    public Result<PageResult<MigrationDTO>> listMigrations(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(migrationAppService.listMigrations(pageNum, pageSize));
    }

    @Operation(summary = "获取迁移详情")
    @GetMapping("/{id}")
    @RequirePermission("sys:migration:view")
    public Result<MigrationDTO> getMigration(@PathVariable Long id) {
        return Result.success(migrationAppService.getMigrationById(id));
    }

    @Operation(summary = "执行迁移脚本")
    @PostMapping("/execute/{version}")
    @RequirePermission("sys:migration:execute")
    @OperationLog(module = "数据库迁移", action = "执行迁移脚本")
    public Result<MigrationDTO> executeMigration(@PathVariable String version) {
        return Result.success(migrationAppService.executeMigration(version));
    }
}

