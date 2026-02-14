package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class ConfigController {

    private final ConfigService configService;

    /**
     * 获取所有配置（按分组）
     */
    @GetMapping
    @Operation(summary = "获取所有配置", description = "获取所有配置项，按分组返回")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<Map<String, List<SysConfig>>> getAllGrouped() {
        return Result.success(configService.getAllGrouped());
    }

    /**
     * 获取配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取配置列表", description = "获取所有配置项列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<SysConfig>> getAll() {
        return Result.success(configService.getAll());
    }

    /**
     * 根据分组获取配置
     */
    @GetMapping("/group/{group}")
    @Operation(summary = "按分组获取配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<SysConfig>> getByGroup(@PathVariable String group) {
        return Result.success(configService.getByGroup(group));
    }

    /**
     * 获取站点配置（公开接口，无需认证）
     * 用于登录页面等公共页面加载系统名称、Logo等信息
     */
    @GetMapping("/public/site")
    @Operation(summary = "获取站点配置（公开）", description = "无需认证，用于登录页面等")
    public Result<List<SysConfig>> getPublicSiteConfig() {
        return Result.success(configService.getByGroup("SITE"));
    }

    /**
     * 根据配置键获取配置
     */
    @GetMapping("/{key}")
    @Operation(summary = "获取单个配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<SysConfig> getByKey(@PathVariable String key) {
        return Result.success(configService.getByKey(key));
    }

    /**
     * 更新配置
     */
    @PutMapping("/{key}")
    @Operation(summary = "更新配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        configService.updateConfig(key, value);
        return Result.success("更新成功", null);
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    @Operation(summary = "批量更新配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configs) {
        configService.batchUpdateConfigs(configs);
        return Result.success("批量更新成功", null);
    }

    /**
     * 创建配置
     */
    @PostMapping
    @Operation(summary = "创建配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SysConfig> createConfig(@RequestBody SysConfig config) {
        SysConfig created = configService.createConfig(config);
        return Result.success("创建成功", created);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return Result.success("删除成功", null);
    }

    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新配置缓存")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.success("缓存刷新成功", null);
    }

    /**
     * 获取档案号配置
     */
    @GetMapping("/archive-no")
    @Operation(summary = "获取档案号配置", description = "获取档案号规则相关配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<SysConfig>> getArchiveNoConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_ARCHIVE_NO));
    }

    /**
     * 获取保管期限配置
     */
    @GetMapping("/retention")
    @Operation(summary = "获取保管期限配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<SysConfig>> getRetentionConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_RETENTION));
    }

    /**
     * 获取系统参数配置
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统参数配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVIST')")
    public Result<List<SysConfig>> getSystemConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_SYSTEM));
    }
}
