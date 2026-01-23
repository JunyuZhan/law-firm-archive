package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.finance.service.ContractNumberGenerator;
import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.VersionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    private final Environment environment;

    @Operation(summary = "获取所有配置")
    @GetMapping
    @RequirePermission("sys:config:list")
    public Result<List<SysConfigDTO>> listConfigs(@RequestParam(required = false) String keyPrefix) {
        if (keyPrefix != null && !keyPrefix.isEmpty()) {
            return Result.success(configAppService.listConfigsByPrefix(keyPrefix));
        }
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
    public Result<Void> updateConfigByKey(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            // 兼容直接传字符串的情况
            value = body.toString();
        }
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

    // ============ 系统维护模式相关接口 ============

    @Operation(summary = "获取维护模式状态")
    @GetMapping("/maintenance/status")
    @RequirePermission("sys:config:list")
    public Result<Map<String, Object>> getMaintenanceStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        String enabled = configAppService.getConfigValue("sys.maintenance.enabled");
        String message = configAppService.getConfigValue("sys.maintenance.message");
        status.put("enabled", "true".equalsIgnoreCase(enabled));
        status.put("message", message != null ? message : "系统正在维护中，请稍后再试");
        return Result.success(status);
    }

    @Operation(summary = "开启维护模式")
    @PostMapping("/maintenance/enable")
    @RequirePermission("sys:config:update")
    @OperationLog(module = "系统维护", action = "开启维护模式")
    public Result<Void> enableMaintenanceMode(@RequestBody(required = false) Map<String, String> params) {
        String message = params != null ? params.get("message") : null;
        if (message == null || message.isEmpty()) {
            message = "系统正在维护中，预计维护时间：30分钟，请稍后再试";
        }
        configAppService.updateConfigByKey("sys.maintenance.enabled", "true");
        configAppService.updateConfigByKey("sys.maintenance.message", message);
        return Result.success();
    }

    @Operation(summary = "关闭维护模式")
    @PostMapping("/maintenance/disable")
    @RequirePermission("sys:config:update")
    @OperationLog(module = "系统维护", action = "关闭维护模式")
    public Result<Void> disableMaintenanceMode() {
        configAppService.updateConfigByKey("sys.maintenance.enabled", "false");
        return Result.success();
    }

    // ============ 系统版本信息相关接口 ============

    @Operation(summary = "获取系统版本信息")
    @GetMapping("/version")
    public Result<Map<String, Object>> getVersionInfo() {
        VersionUtils.VersionInfo versionInfo = VersionUtils.getVersionInfo();
        String[] activeProfiles = environment.getActiveProfiles();
        
        // 优先从数据库读取版本号（如果配置了 sys.version）
        // 这样可以支持更灵活的版本号格式，如 0.4、1.2 等简单数字格式
        String dbVersion = null;
        try {
            dbVersion = configAppService.getConfigValue("sys.version");
        } catch (Exception e) {
            // 忽略错误，使用构建时的版本号
        }
        
        Map<String, Object> info = new HashMap<>();
        // 如果数据库中有版本号配置，优先使用数据库版本（支持简单格式如 0.4）
        // 否则使用构建时的版本号（如 1.0.0-SNAPSHOT）
        info.put("version", dbVersion != null && !dbVersion.isEmpty() ? dbVersion : versionInfo.getVersion());
        info.put("buildVersion", versionInfo.getVersion()); // 保留构建版本号
        info.put("buildTime", versionInfo.getBuildTime());
        info.put("gitCommit", versionInfo.getGitCommit());
        info.put("profile", activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return Result.success(info);
    }
}
