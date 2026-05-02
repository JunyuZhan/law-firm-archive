package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.config.DependencyStatusItemDTO;
import com.archivesystem.dto.config.PublicSiteConfigResponse;
import com.archivesystem.dto.config.RegistryUpdateCheckDTO;
import com.archivesystem.dto.config.SiteLogoUploadResponse;
import com.archivesystem.dto.config.SysConfigResponse;
import com.archivesystem.dto.config.SystemDependencyStatusDTO;
import com.archivesystem.dto.config.SystemRuntimeInfoDTO;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.service.AlertService;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import com.archivesystem.service.RegistryUpdateCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 系统配置控制器
 * @author junyuzhan
 */
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class ConfigController {

    private final ConfigService configService;
    private final AlertService alertService;
    private final MinioService minioService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;
    private final RegistryUpdateCheckService registryUpdateCheckService;

    private static final String SITE_LOGO_CONFIG_KEY = "system.site.logo";
    private static final String SITE_LOGO_OBJECT_KEY = "system.site.logo.object";
    private static final String SITE_LOGO_OBJECT_PREFIX = "site/logo/";
    private static final Set<String> PUBLIC_SITE_CONFIG_KEYS = Set.of(
            "system.site.name",
            "system.site.name.en",
            SITE_LOGO_CONFIG_KEY,
            "system.site.icp",
            "system.site.copyright"
    );

    @Value("${spring.application.name:archive-system}")
    private String applicationName;

    @Value("${APP_VERSION:}")
    private String appVersion;

    @Value("${APP_COMMIT_SHA:}")
    private String appCommitSha;

    @Value("${APP_BUILD_TIME:}")
    private String appBuildTime;

    /**
     * 获取所有配置（按分组）
     */
    @GetMapping
    @Operation(summary = "获取所有配置", description = "获取所有配置项，按分组返回")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Map<String, List<SysConfigResponse>>> getAllGrouped() {
        Map<String, List<SysConfigResponse>> result = new LinkedHashMap<>();
        configService.getAllGrouped().forEach((group, configs) ->
                result.put(group, configs.stream().map(SysConfigResponse::from).toList()));
        return Result.success(result);
    }

    /**
     * 获取配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取配置列表", description = "获取所有配置项列表")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfigResponse>> getAll() {
        return Result.success(configService.getAll().stream().map(SysConfigResponse::from).toList());
    }

    /**
     * 根据分组获取配置
     */
    @GetMapping("/group/{group}")
    @Operation(summary = "按分组获取配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfigResponse>> getByGroup(@PathVariable String group) {
        return Result.success(configService.getByGroup(group).stream().map(SysConfigResponse::from).toList());
    }

    /**
     * 获取站点配置（公开接口，无需认证）
     * 用于登录页面等公共页面加载系统名称、Logo等信息
     */
    @GetMapping("/public/site")
    @Operation(summary = "获取站点配置（公开）", description = "无需认证，用于登录页面等")
    public Result<List<PublicSiteConfigResponse>> getPublicSiteConfig() {
        List<SysConfig> publicConfigs = configService.getByGroup("SITE").stream()
                .filter(config -> config != null && PUBLIC_SITE_CONFIG_KEYS.contains(config.getConfigKey()))
                .toList();
        return Result.success(publicConfigs.stream().map(PublicSiteConfigResponse::from).toList());
    }

    /**
     * 获取当前站点 Logo 文件.
     */
    @GetMapping("/public/site/logo")
    @Operation(summary = "获取站点Logo（公开）", description = "读取当前上传的站点Logo")
    public ResponseEntity<?> getPublicSiteLogo() {
        String objectName = configService.getValue(SITE_LOGO_OBJECT_KEY);
        if (!StringUtils.hasText(objectName) || !objectName.startsWith(SITE_LOGO_OBJECT_PREFIX)) {
            return ResponseEntity.notFound().build();
        }

        try {
            String contentType = minioService.getContentType(objectName);
            if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaType.parseMediaType(contentType);
            InputStream inputStream = minioService.getFile(objectName);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                    .contentType(mediaType)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据配置键获取配置
     */
    @GetMapping("/{key}")
    @Operation(summary = "获取单个配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SysConfigResponse> getByKey(@PathVariable String key) {
        return Result.success(SysConfigResponse.from(configService.getByKey(key)));
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
     * 上传站点 Logo.
     */
    @PostMapping("/site/logo")
    @Operation(summary = "上传站点Logo")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SiteLogoUploadResponse> uploadSiteLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("400", "请选择要上传的Logo文件");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            return Result.error("400", "Logo仅支持图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024L) {
            return Result.error("400", "Logo文件大小不能超过2MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = "png";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        String objectName = "site/logo/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        minioService.uploadFile(file, objectName);

        String logoUrl = "/api/configs/public/site/logo?t=" + Instant.now().toEpochMilli();
        configService.saveConfig(SITE_LOGO_OBJECT_KEY, objectName, "SITE", "Logo 对象路径", SysConfig.TYPE_STRING, false, 6);
        configService.saveConfig(SITE_LOGO_CONFIG_KEY, logoUrl, "SITE", "Logo URL", SysConfig.TYPE_STRING, true, 5);

        return Result.success("Logo上传成功", SiteLogoUploadResponse.builder()
                .logoUrl(logoUrl)
                .objectName(objectName)
                .build());
    }

    /**
     * 创建配置
     */
    @PostMapping
    @Operation(summary = "创建配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SysConfigResponse> createConfig(@RequestBody SysConfig config) {
        SysConfig created = configService.createConfig(config);
        return Result.success("创建成功", SysConfigResponse.from(created));
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
     * 发送测试邮件（验证 SMTP 与收件人）
     */
    @PostMapping("/test-mail")
    @Operation(summary = "发送测试邮件")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> sendTestMail(@RequestBody(required = false) Map<String, String> body) {
        String to = body != null ? body.get("to") : null;
        alertService.sendTestMail(to);
        return Result.success("测试邮件已发送", null);
    }

    /**
     * 获取档案号配置
     */
    @GetMapping("/archive-no")
    @Operation(summary = "获取档案号配置", description = "获取档案号规则相关配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfigResponse>> getArchiveNoConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_ARCHIVE_NO).stream()
                .map(SysConfigResponse::from)
                .toList());
    }

    /**
     * 获取保管期限配置
     */
    @GetMapping("/retention")
    @Operation(summary = "获取保管期限配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfigResponse>> getRetentionConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_RETENTION).stream()
                .map(SysConfigResponse::from)
                .toList());
    }

    /**
     * 获取系统参数配置
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统参数配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfigResponse>> getSystemConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_SYSTEM).stream()
                .map(SysConfigResponse::from)
                .toList());
    }

    /**
     * 获取系统版本与运行信息
     */
    @GetMapping("/runtime-info")
    @Operation(summary = "获取系统版本与运行信息")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SystemRuntimeInfoDTO> getRuntimeInfo() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String backendVersion = buildProperties != null ? buildProperties.getVersion() : "unknown";
        String productVersion = StringUtils.hasText(appVersion) ? appVersion : backendVersion;
        String commitSha = StringUtils.hasText(appCommitSha) ? appCommitSha : "unknown";
        String buildTime = StringUtils.hasText(appBuildTime)
                ? appBuildTime
                : (buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString()
                : "unknown");

        return Result.success(SystemRuntimeInfoDTO.builder()
                .applicationName(applicationName)
                .productVersion(productVersion)
                .backendVersion(backendVersion)
                .commitSha(commitSha)
                .buildTime(buildTime)
                .build());
    }

    /**
     * 检查配置的镜像仓库是否存在可用更新。
     */
    @GetMapping("/registry-update-check")
    @Operation(summary = "检查镜像仓库是否有可用更新")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<RegistryUpdateCheckDTO> checkRegistryUpdate() {
        return Result.success(registryUpdateCheckService.checkRegistryUpdate());
    }

    /**
     * 获取关键依赖运行状态
     */
    @GetMapping("/dependency-status")
    @Operation(summary = "获取关键依赖运行状态")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SystemDependencyStatusDTO> getDependencyStatus() {
        HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
        if (healthEndpoint == null) {
            return Result.success(SystemDependencyStatusDTO.builder()
                    .overallStatus("UNKNOWN")
                    .items(List.of())
                    .build());
        }

        HealthComponent root = healthEndpoint.health();
        Map<String, HealthComponent> components = extractComponents(root);
        List<DependencyStatusItemDTO> items = new ArrayList<>();
        items.add(toDependencyItem("db", "PostgreSQL", components.get("db")));
        items.add(toDependencyItem("redis", "Redis", components.get("redis")));
        items.add(toDependencyItem("minio", "MinIO", components.get("minio")));
        items.add(toDependencyItem("rabbit", "RabbitMQ", components.get("rabbit")));
        items.add(toDependencyItem("elasticsearch", "Elasticsearch", components.get("elasticsearch")));

        boolean allUp = items.stream().allMatch(item -> "UP".equalsIgnoreCase(item.getStatus()));
        return Result.success(SystemDependencyStatusDTO.builder()
                .overallStatus(allUp ? "UP" : "DEGRADED")
                .items(items)
                .build());
    }


    private DependencyStatusItemDTO toDependencyItem(String key, String label, HealthComponent component) {
        if (component == null) {
            return DependencyStatusItemDTO.builder()
                    .key(key)
                    .label(label)
                    .status("UNKNOWN")
                    .details(Map.of("message", "未获取到健康检查信息"))
                    .build();
        }

        Status status = component.getStatus();
        Map<String, Object> details = component instanceof org.springframework.boot.actuate.health.Health health
                ? sanitizeHealthDetails(health.getDetails())
                : Map.of();

        return DependencyStatusItemDTO.builder()
                .key(key)
                .label(label)
                .status(status != null ? status.getCode() : "UNKNOWN")
                .details(details)
                .build();
    }

    private Map<String, HealthComponent> extractComponents(HealthComponent root) {
        if (root instanceof SystemHealth systemHealth) {
            return systemHealth.getComponents();
        }
        if (root instanceof CompositeHealth compositeHealth) {
            return compositeHealth.getComponents();
        }
        return Map.of();
    }

    private Map<String, Object> sanitizeHealthDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        details.forEach((key, value) -> sanitized.put(key, sanitizeHealthValue(key, value)));
        return sanitized;
    }

    private Object sanitizeHealthValue(String key, Object value) {
        if (isSensitiveHealthKey(key)) {
            return "******";
        }
        if (value instanceof Map<?, ?> nestedMap) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            nestedMap.forEach((nestedKey, nestedValue) ->
                    sanitized.put(String.valueOf(nestedKey), sanitizeHealthValue(String.valueOf(nestedKey), nestedValue)));
            return sanitized;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> sanitizeHealthValue(key, item))
                    .toList();
        }
        if (value instanceof String text && looksSensitiveHealthValue(text)) {
            return "******";
        }
        return value;
    }

    private boolean isSensitiveHealthKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("credential")
                || normalized.contains("username")
                || normalized.contains("user")
                || normalized.contains("jdbc")
                || normalized.contains("url")
                || normalized.contains("uri");
    }

    private boolean looksSensitiveHealthValue(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("jdbc:")
                || normalized.contains("://")
                || normalized.contains("password=")
                || normalized.contains("user=");
    }

}
