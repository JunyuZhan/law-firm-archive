package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.config.DeliveryDocumentDTO;
import com.archivesystem.dto.config.DependencyStatusItemDTO;
import com.archivesystem.dto.config.SystemDeliveryInfoDTO;
import com.archivesystem.dto.config.SystemDependencyStatusDTO;
import com.archivesystem.dto.config.SystemRuntimeInfoDTO;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
    private final MinioService minioService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;

    private static final String SITE_LOGO_CONFIG_KEY = "system.site.logo";
    private static final String SITE_LOGO_OBJECT_KEY = "system.site.logo.object";
    private static final List<DeliveryDocumentDefinition> DELIVERY_DOCUMENTS = List.of(
            new DeliveryDocumentDefinition(
                    "deployment-upgrade-guide",
                    "部署与升级手册",
                    "用于首次部署、版本升级、回滚和目录规范核查。",
                    "delivery-docs/deployment-upgrade-guide.md",
                    "archive-delivery-deployment-upgrade-guide.md"
            ),
            new DeliveryDocumentDefinition(
                    "release-checklist",
                    "发布前验收清单",
                    "用于发版前统一核对版本、镜像、脚本、测试与回滚准备。",
                    "delivery-docs/release-checklist.md",
                    "archive-delivery-release-checklist.md"
            ),
            new DeliveryDocumentDefinition(
                    "deployment-smoke-test",
                    "部署后冒烟测试流程",
                    "用于部署完成后验证入库、保存、借阅关键链路。",
                    "delivery-docs/deployment-smoke-test.md",
                    "archive-delivery-smoke-test.md"
            ),
            new DeliveryDocumentDefinition(
                    "test-ledger-template",
                    "测试执行台账模板",
                    "用于记录客户、环境、版本、执行结果和是否回滚。",
                    "delivery-docs/test-ledger-template.md",
                    "archive-delivery-test-ledger-template.md"
            )
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
    public Result<Map<String, List<SysConfig>>> getAllGrouped() {
        return Result.success(configService.getAllGrouped());
    }

    /**
     * 获取配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取配置列表", description = "获取所有配置项列表")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfig>> getAll() {
        return Result.success(configService.getAll());
    }

    /**
     * 根据分组获取配置
     */
    @GetMapping("/group/{group}")
    @Operation(summary = "按分组获取配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
     * 获取当前站点 Logo 文件.
     */
    @GetMapping("/public/site/logo")
    @Operation(summary = "获取站点Logo（公开）", description = "读取当前上传的站点Logo")
    public ResponseEntity<?> getPublicSiteLogo() {
        String objectName = configService.getValue(SITE_LOGO_OBJECT_KEY);
        if (!StringUtils.hasText(objectName)) {
            return ResponseEntity.notFound().build();
        }
        InputStream inputStream = minioService.getFile(objectName);
        String contentType = minioService.getContentType(objectName);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (StringUtils.hasText(contentType)) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                .contentType(mediaType)
                .body(new InputStreamResource(inputStream));
    }

    /**
     * 根据配置键获取配置
     */
    @GetMapping("/{key}")
    @Operation(summary = "获取单个配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
     * 上传站点 Logo.
     */
    @PostMapping("/site/logo")
    @Operation(summary = "上传站点Logo")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Map<String, String>> uploadSiteLogo(@RequestParam("file") MultipartFile file) {
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

        return Result.success("Logo上传成功", Map.of(
                "logoUrl", logoUrl,
                "objectName", objectName
        ));
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
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfig>> getArchiveNoConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_ARCHIVE_NO));
    }

    /**
     * 获取保管期限配置
     */
    @GetMapping("/retention")
    @Operation(summary = "获取保管期限配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfig>> getRetentionConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_RETENTION));
    }

    /**
     * 获取系统参数配置
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统参数配置")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<SysConfig>> getSystemConfigs() {
        return Result.success(configService.getByGroup(SysConfig.GROUP_SYSTEM));
    }

    /**
     * 获取系统版本与部署信息
     */
    @GetMapping("/runtime-info")
    @Operation(summary = "获取系统版本与部署信息")
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
                .recommendedMode("Docker Compose")
                .build());
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

    /**
     * 获取镜像交付信息.
     */
    @GetMapping("/delivery-info")
    @Operation(summary = "获取镜像交付信息")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<SystemDeliveryInfoDTO> getDeliveryInfo() {
        List<DeliveryDocumentDTO> documents = DELIVERY_DOCUMENTS.stream()
                .map(item -> DeliveryDocumentDTO.builder()
                        .code(item.code())
                        .title(item.title())
                        .description(item.description())
                        .fileName(item.fileName())
                        .build())
                .toList();

        return Result.success(SystemDeliveryInfoDTO.builder()
                .deliveryMode("标准交付")
                .artifactType("系统发布包 + 部署清单 + 交付文档包")
                .recommendedDeployment("容器化部署")
                .deploymentDirectory("/opt/law-firm-archive")
                .sourceDirectory("")
                .upgradeOwner("系统运维管理员")
                .sourceCodeIncluded(Boolean.FALSE)
                .notes(List.of(
                        "系统交付以标准发布包、部署清单和交付文档包为准。",
                        "业务管理员负责系统内配置、档案、借阅和备份恢复。",
                        "系统升级、回退和运行维护应由运维管理员执行。"
                ))
                .documents(documents)
                .build());
    }

    /**
     * 下载镜像交付文档.
     */
    @GetMapping("/delivery-docs/{code}/download")
    @Operation(summary = "下载镜像交付文档")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<InputStreamResource> downloadDeliveryDoc(@PathVariable String code) throws IOException {
        DeliveryDocumentDefinition document = DELIVERY_DOCUMENTS.stream()
                .filter(item -> item.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "交付文档不存在"));

        Resource resource = new ClassPathResource(document.resourcePath());
        if (!resource.exists()) {
            throw new ResponseStatusException(NOT_FOUND, "交付文档不存在");
        }

        String encodedFileName = UriUtils.encode(document.fileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(new InputStreamResource(resource.getInputStream()));
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
                ? new LinkedHashMap<>(health.getDetails())
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

    private record DeliveryDocumentDefinition(
            String code,
            String title,
            String description,
            String resourcePath,
            String fileName
    ) {
    }
}
