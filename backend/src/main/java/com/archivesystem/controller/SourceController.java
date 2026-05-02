package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.dto.source.ExternalSourceApiKeyResponse;
import com.archivesystem.dto.source.ExternalSourceCreateResponse;
import com.archivesystem.dto.source.ExternalSourceResponse;
import com.archivesystem.dto.source.ExternalSourceSummaryResponse;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.security.ApiKeyAuthFilter;
import com.archivesystem.security.OutboundUrlValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 档案来源控制器.
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/sources")
@RequiredArgsConstructor
@Tag(name = "档案来源管理", description = "管理外部档案来源")
public class SourceController {
    private static final String SOURCE_CONNECTION_FAILURE_PUBLIC_MESSAGE = "连接失败，请检查API地址、网络和服务状态";
    private static final List<String> SUPPORTED_SOURCE_TYPES = List.of(
            ExternalSource.TYPE_LAW_FIRM,
            ExternalSource.TYPE_COURT,
            ExternalSource.TYPE_ENTERPRISE,
            ExternalSource.TYPE_OTHER
    );

    private final ExternalSourceMapper externalSourceMapper;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final OutboundUrlValidator outboundUrlValidator;
    private final RestTemplate restTemplate;

    @GetMapping
    @Operation(summary = "获取来源列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<ExternalSourceSummaryResponse>> list() {
        List<ExternalSource> sources = externalSourceMapper.selectList(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getDeleted, false)
                        .orderByDesc(ExternalSource::getCreatedAt));
        return Result.success(sources.stream()
                .map(ExternalSourceSummaryResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取来源详情")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<ExternalSourceResponse> getById(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        return Result.success(ExternalSourceResponse.from(source, false, true));
    }

    @PostMapping
    @Operation(summary = "创建来源")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<ExternalSourceCreateResponse> create(@Valid @RequestBody ExternalSource source) {
        normalizeSource(source);
        // 检查编码是否重复
        ExternalSource existing = externalSourceMapper.selectOne(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getSourceCode, source.getSourceCode())
                        .eq(ExternalSource::getDeleted, false));
        if (existing != null) {
            return Result.error("400", "来源编码已存在");
        }
        // 自动生成 API Key，数据库存储SHA-256哈希值
        String plainApiKey;
        if (source.getApiKey() == null || source.getApiKey().isEmpty()) {
            plainApiKey = apiKeyAuthFilter.generateApiKey();
        } else {
            plainApiKey = source.getApiKey();
        }
        String hashedApiKey = apiKeyAuthFilter.hashApiKeyPublic(plainApiKey);
        if (hasDuplicateApiKey(hashedApiKey, null)) {
            return Result.error("409", "API Key 已被其他来源使用");
        }
        source.setApiKey(hashedApiKey);
        externalSourceMapper.insert(source);
        apiKeyAuthFilter.clearApiKeyCache(hashedApiKey);
        // 返回时包含明文 API Key，仅创建时返回一次
        source.setApiKey(plainApiKey);
        return Result.success("创建成功，请保存 API Key", ExternalSourceCreateResponse.from(source));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新来源")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExternalSource source) {
        ExternalSource existing = externalSourceMapper.selectById(id);
        if (existing == null || existing.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        normalizeSource(source);
        ExternalSource duplicate = externalSourceMapper.selectOne(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getSourceCode, source.getSourceCode())
                        .eq(ExternalSource::getDeleted, false)
                        .ne(ExternalSource::getId, id));
        if (duplicate != null) {
            return Result.error("409", "来源编码已存在");
        }
        source.setId(id);
        // 如果没有传入新密钥，保留原密钥（哈希值）
        if (source.getApiKey() == null || source.getApiKey().isEmpty()) {
            source.setApiKey(existing.getApiKey());
        } else if (!source.getApiKey().equals(existing.getApiKey())) {
            // API Key 变更，清除旧缓存并存储新哈希值
            String hashedApiKey = apiKeyAuthFilter.hashApiKeyPublic(source.getApiKey());
            if (hasDuplicateApiKey(hashedApiKey, id)) {
                return Result.error("409", "API Key 已被其他来源使用");
            }
            apiKeyAuthFilter.clearApiKeyCache(existing.getApiKey());
            source.setApiKey(hashedApiKey);
        }
        externalSourceMapper.updateById(source);
        // 清除新 API Key 的缓存（如果之前被标记为无效）
        apiKeyAuthFilter.clearApiKeyCache(source.getApiKey());
        return Result.success("更新成功", null);
    }

    @PostMapping("/{id}/regenerate-key")
    @Operation(summary = "重新生成API Key")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<ExternalSourceApiKeyResponse> regenerateApiKey(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        // 清除旧缓存
        apiKeyAuthFilter.clearApiKeyCache(source.getApiKey());
        // 生成新 Key（明文），数据库存储哈希值
        String plainApiKey = apiKeyAuthFilter.generateApiKey();
        String hashedApiKey = apiKeyAuthFilter.hashApiKeyPublic(plainApiKey);
        source.setApiKey(hashedApiKey);
        externalSourceMapper.updateById(source);
        apiKeyAuthFilter.clearApiKeyCache(hashedApiKey);
        log.info("重新生成API Key: sourceId={}, sourceCode={}", id, source.getSourceCode());
        return Result.success("API Key 已重新生成，请保存", ExternalSourceApiKeyResponse.builder()
                .apiKey(plainApiKey)
                .build());
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换启用状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        source.setEnabled(enabled);
        externalSourceMapper.updateById(source);
        return Result.success(enabled ? "已启用" : "已禁用", null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> test(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        
        String apiUrl = source.getApiUrl();
        LocalDateTime now = LocalDateTime.now();
        
        // 未配置 API URL 时不允许测试，避免误导为“连接成功”
        if (apiUrl == null || apiUrl.isEmpty()) {
            source.setLastSyncAt(now);
            source.setLastSyncStatus("FAILED");
            source.setLastSyncMessage("未配置API地址，无法测试连接");
            externalSourceMapper.updateById(source);
            return Result.error("400", "请先配置API地址后再测试连接");
        }
        
        try {
            outboundUrlValidator.validate(apiUrl, "API地址");
            // 尝试访问API URL（HEAD请求，减少流量）
            log.info("测试来源连接: id={}, url={}", id, apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, 
                    HttpMethod.HEAD, 
                    null, 
                    String.class);
            
            boolean success = response.getStatusCode().is2xxSuccessful() 
                    || response.getStatusCode().is3xxRedirection();
            
            source.setLastSyncAt(now);
            source.setLastSyncStatus(success ? "SUCCESS" : "FAILED");
            source.setLastSyncMessage(success ? "连接成功" : "HTTP " + response.getStatusCode());
            externalSourceMapper.updateById(source);
            
            if (success) {
                return Result.success("连接测试成功", null);
            } else {
                return Result.error("502", "连接返回状态: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("测试来源连接失败: id={}", id, e);
            source.setLastSyncAt(now);
            source.setLastSyncStatus("FAILED");
            source.setLastSyncMessage(SOURCE_CONNECTION_FAILURE_PUBLIC_MESSAGE);
            externalSourceMapper.updateById(source);
            return Result.error("502", SOURCE_CONNECTION_FAILURE_PUBLIC_MESSAGE);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除来源")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source != null && !Boolean.TRUE.equals(source.getDeleted())) {
            source.setDeleted(true);
            externalSourceMapper.updateById(source);
        }
        return Result.success("删除成功", null);
    }

    private void normalizeSource(ExternalSource source) {
        source.setSourceCode(trimToNull(source.getSourceCode()));
        source.setSourceName(trimToNull(source.getSourceName()));
        source.setSourceType(trimToNull(source.getSourceType()));
        source.setDescription(trimToNull(source.getDescription()));
        source.setApiUrl(trimToNull(source.getApiUrl()));
        source.setApiKey(trimToNull(source.getApiKey()));
        source.setAuthType(firstNonBlank(trimToNull(source.getAuthType()), ExternalSource.AUTH_API_KEY));
        validateSourceType(source.getSourceType());
        validateAuthType(source.getAuthType());
        if (source.getApiUrl() != null) {
            outboundUrlValidator.validate(source.getApiUrl(), "API地址");
        }
        normalizeExtraConfig(source);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasDuplicateApiKey(String hashedApiKey, Long excludeId) {
        if (hashedApiKey == null || hashedApiKey.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<ExternalSource> wrapper = new LambdaQueryWrapper<ExternalSource>()
                .eq(ExternalSource::getApiKey, hashedApiKey)
                .eq(ExternalSource::getDeleted, false);
        if (excludeId != null) {
            wrapper.ne(ExternalSource::getId, excludeId);
        }
        return externalSourceMapper.selectOne(wrapper) != null;
    }

    private void validateSourceType(String sourceType) {
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw new BusinessException("不支持的来源类型");
        }
    }

    private void validateAuthType(String authType) {
        if (!ExternalSource.AUTH_API_KEY.equals(authType)) {
            throw new BusinessException("当前版本仅支持 API_KEY 认证方式");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void normalizeExtraConfig(ExternalSource source) {
        Map<String, Object> extraConfig = source.getExtraConfig();
        if (extraConfig == null || extraConfig.isEmpty()) {
            source.setExtraConfig(null);
            return;
        }

        Object callbackValue = extraConfig.get("callbackUrl");
        String callbackUrl = callbackValue != null ? trimToNull(String.valueOf(callbackValue)) : null;
        if (callbackUrl != null) {
            outboundUrlValidator.validate(callbackUrl, "回调地址");
            source.setExtraConfig(Map.of("callbackUrl", callbackUrl));
            return;
        }
        source.setExtraConfig(null);
    }
}
