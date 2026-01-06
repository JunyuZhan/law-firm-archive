package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.finance.service.ContractNumberGenerator;
import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置接口
 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigAppService configAppService;
    private final ContractNumberGenerator contractNumberGenerator;

    @Operation(summary = "获取所有配置")
    @GetMapping
    @RequirePermission("sys:config:list")
    public Result<List<SysConfigDTO>> listConfigs() {
        return Result.success(configAppService.listConfigs());
    }

    @Operation(summary = "根据键获取配置")
    @GetMapping("/key/{key}")
    public Result<SysConfigDTO> getConfigByKey(@PathVariable String key) {
        return Result.success(configAppService.getConfigByKey(key));
    }

    @Operation(summary = "批量获取配置")
    @PostMapping("/batch")
    public Result<Map<String, String>> getConfigBatch(@RequestBody List<String> keys) {
        return Result.success(configAppService.getConfigMap(keys));
    }

    @Operation(summary = "创建配置")
    @PostMapping
    @RequirePermission("sys:config:create")
    @OperationLog(module = "系统配置", action = "创建配置")
    public Result<SysConfigDTO> createConfig(@RequestBody UpdateConfigCommand command) {
        return Result.success(configAppService.createConfig(command));
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    @RequirePermission("sys:config:update")
    @OperationLog(module = "系统配置", action = "更新配置")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody UpdateConfigCommand command) {
        command.setId(id);
        configAppService.updateConfig(command);
        return Result.success();
    }

    @Operation(summary = "根据键更新配置值")
    @PutMapping("/key/{key}")
    @RequirePermission("sys:config:update")
    @OperationLog(module = "系统配置", action = "更新配置值")
    public Result<Void> updateConfigByKey(@PathVariable String key, @RequestBody String value) {
        configAppService.updateConfigByKey(key, value);
        return Result.success();
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    @RequirePermission("sys:config:delete")
    @OperationLog(module = "系统配置", action = "删除配置")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        configAppService.deleteConfig(id);
        return Result.success();
    }

    // ============ 合同编号配置相关接口 ============

    @Operation(summary = "预览合同编号规则")
    @PostMapping("/contract-number/preview")
    @RequirePermission("sys:config:list")
    public Result<List<Map<String, String>>> previewContractNumber(@RequestBody Map<String, Object> params) {
        String pattern = (String) params.get("pattern");
        String prefix = (String) params.get("prefix");
        Integer sequenceLength = params.get("sequenceLength") != null 
                ? Integer.valueOf(params.get("sequenceLength").toString()) : null;
        String caseType = (String) params.get("caseType");
        String feeType = (String) params.get("feeType");
        
        List<Map<String, String>> previews = contractNumberGenerator.previewPattern(
                pattern, prefix, sequenceLength, caseType, feeType);
        return Result.success(previews);
    }

    @Operation(summary = "获取合同编号支持的变量")
    @GetMapping("/contract-number/variables")
    public Result<List<Map<String, String>>> getContractNumberVariables() {
        return Result.success(contractNumberGenerator.getSupportedVariables());
    }

    @Operation(summary = "获取推荐的合同编号规则模板")
    @GetMapping("/contract-number/patterns")
    public Result<List<Map<String, String>>> getRecommendedPatterns() {
        return Result.success(contractNumberGenerator.getRecommendedPatterns());
    }

    @Operation(summary = "获取案件类型选项")
    @GetMapping("/contract-number/case-types")
    public Result<List<Map<String, String>>> getCaseTypeOptions() {
        return Result.success(contractNumberGenerator.getCaseTypeOptions());
    }
}
