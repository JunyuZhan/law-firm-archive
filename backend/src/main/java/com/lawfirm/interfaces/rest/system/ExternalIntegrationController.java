package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 外部系统集成管理控制器
 */
@Tag(name = "外部系统集成管理")
@RestController
@RequestMapping("/system/integration")
@RequiredArgsConstructor
public class ExternalIntegrationController {

    private final ExternalIntegrationAppService integrationAppService;

    @Operation(summary = "分页查询集成配置")
    @GetMapping
    @RequirePermission("system:integration:list")
    public Result<PageResult<ExternalIntegrationDTO>> list(ExternalIntegrationQueryDTO query) {
        return Result.success(integrationAppService.listIntegrations(query));
    }

    @Operation(summary = "获取所有集成配置")
    @GetMapping("/all")
    @RequirePermission("system:integration:list")
    public Result<List<ExternalIntegrationDTO>> listAll() {
        return Result.success(integrationAppService.listAllIntegrations());
    }

    @Operation(summary = "获取指定类型的启用集成")
    @GetMapping("/enabled")
    @RequirePermission("system:integration:list")
    public Result<List<ExternalIntegrationDTO>> listEnabled(
            @Parameter(description = "集成类型：ARCHIVE/AI/OTHER") @RequestParam String type) {
        return Result.success(integrationAppService.listEnabledByType(type));
    }

    @Operation(summary = "获取集成详情")
    @GetMapping("/{id}")
    @RequirePermission("system:integration:list")
    public Result<ExternalIntegrationDTO> getById(@PathVariable Long id) {
        return Result.success(integrationAppService.getIntegrationById(id));
    }

    @Operation(summary = "根据编码获取集成详情")
    @GetMapping("/code/{code}")
    @RequirePermission("system:integration:list")
    public Result<ExternalIntegrationDTO> getByCode(@PathVariable String code) {
        return Result.success(integrationAppService.getIntegrationByCode(code));
    }

    @Operation(summary = "创建集成配置")
    @PostMapping
    @RequirePermission("system:integration:edit")
    @OperationLog(module = "系统管理", action = "创建外部系统集成配置")
    public Result<ExternalIntegrationDTO> create(
            @RequestBody @Validated UpdateExternalIntegrationCommand command) {
        return Result.success(integrationAppService.createIntegration(command));
    }

    @Operation(summary = "更新集成配置")
    @PutMapping("/{id}")
    @RequirePermission("system:integration:edit")
    @OperationLog(module = "系统管理", action = "更新外部系统集成配置")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody @Validated UpdateExternalIntegrationCommand command) {
        command.setId(id);
        integrationAppService.updateIntegration(command);
        return Result.success();
    }

    @Operation(summary = "启用集成")
    @PostMapping("/{id}/enable")
    @RequirePermission("system:integration:edit")
    @OperationLog(module = "系统管理", action = "启用外部系统集成")
    public Result<Void> enable(@PathVariable Long id) {
        integrationAppService.enableIntegration(id);
        return Result.success();
    }

    @Operation(summary = "禁用集成")
    @PostMapping("/{id}/disable")
    @RequirePermission("system:integration:edit")
    @OperationLog(module = "系统管理", action = "禁用外部系统集成")
    public Result<Void> disable(@PathVariable Long id) {
        integrationAppService.disableIntegration(id);
        return Result.success();
    }

    @Operation(summary = "测试连接")
    @PostMapping("/{id}/test")
    @RequirePermission("system:integration:edit")
    @OperationLog(module = "系统管理", action = "测试外部系统连接")
    public Result<ExternalIntegrationDTO> testConnection(@PathVariable Long id) {
        return Result.success(integrationAppService.testConnection(id));
    }
}

