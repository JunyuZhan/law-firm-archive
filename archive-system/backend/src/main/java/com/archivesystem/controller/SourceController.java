package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.security.ApiKeyAuthFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案来源控制器.
 */
@Slf4j
@RestController
@RequestMapping("/sources")
@RequiredArgsConstructor
@Tag(name = "档案来源管理", description = "管理外部档案来源")
public class SourceController {

    private final ExternalSourceMapper externalSourceMapper;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    @Operation(summary = "获取来源列表")
    public Result<List<ExternalSource>> list() {
        List<ExternalSource> sources = externalSourceMapper.selectList(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getDeleted, false)
                        .orderByDesc(ExternalSource::getCreatedAt));
        // 不返回敏感信息
        sources.forEach(s -> s.setApiKey(null));
        return Result.success(sources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取来源详情")
    public Result<ExternalSource> getById(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        // 不返回敏感信息
        source.setApiKey(null);
        return Result.success(source);
    }

    @PostMapping
    @Operation(summary = "创建来源")
    public Result<ExternalSource> create(@RequestBody ExternalSource source) {
        // 检查编码是否重复
        ExternalSource existing = externalSourceMapper.selectOne(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getSourceCode, source.getSourceCode())
                        .eq(ExternalSource::getDeleted, false));
        if (existing != null) {
            return Result.error("400", "来源编码已存在");
        }
        // 自动生成 API Key
        if (source.getApiKey() == null || source.getApiKey().isEmpty()) {
            source.setApiKey(apiKeyAuthFilter.generateApiKey());
        }
        externalSourceMapper.insert(source);
        // 返回时包含 API Key，仅创建时返回一次
        return Result.success("创建成功，请保存 API Key", source);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新来源")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExternalSource source) {
        ExternalSource existing = externalSourceMapper.selectById(id);
        if (existing == null || existing.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        source.setId(id);
        // 如果没有传入新密钥，保留原密钥
        if (source.getApiKey() == null || source.getApiKey().isEmpty()) {
            source.setApiKey(existing.getApiKey());
        } else if (!source.getApiKey().equals(existing.getApiKey())) {
            // API Key 变更，清除旧缓存
            apiKeyAuthFilter.clearApiKeyCache(existing.getApiKey());
        }
        externalSourceMapper.updateById(source);
        // 清除新 API Key 的缓存（如果之前被标记为无效）
        apiKeyAuthFilter.clearApiKeyCache(source.getApiKey());
        return Result.success("更新成功", null);
    }

    @PostMapping("/{id}/regenerate-key")
    @Operation(summary = "重新生成API Key")
    public Result<String> regenerateApiKey(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        // 清除旧缓存
        apiKeyAuthFilter.clearApiKeyCache(source.getApiKey());
        // 生成新 Key
        String newApiKey = apiKeyAuthFilter.generateApiKey();
        source.setApiKey(newApiKey);
        externalSourceMapper.updateById(source);
        log.info("重新生成API Key: sourceId={}, sourceCode={}", id, source.getSourceCode());
        return Result.success("API Key 已重新生成，请保存", newApiKey);
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换启用状态")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null) {
            return Result.error("404", "来源不存在");
        }
        source.setEnabled(enabled);
        externalSourceMapper.updateById(source);
        return Result.success(enabled ? "已启用" : "已禁用", null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    public Result<Void> test(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        
        String apiUrl = source.getApiUrl();
        LocalDateTime now = LocalDateTime.now();
        
        // 如果没有配置API URL，直接标记为成功（仅接收档案场景）
        if (apiUrl == null || apiUrl.isEmpty()) {
            source.setLastSyncAt(now);
            source.setLastSyncStatus("SUCCESS");
            source.setLastSyncMessage("无需测试（仅接收模式）");
            externalSourceMapper.updateById(source);
            return Result.success("配置有效（仅接收模式）", null);
        }
        
        try {
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
                return Result.error("500", "连接返回状态: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("测试来源连接失败: id={}, error={}", id, e.getMessage());
            source.setLastSyncAt(now);
            source.setLastSyncStatus("FAILED");
            source.setLastSyncMessage(e.getMessage());
            externalSourceMapper.updateById(source);
            return Result.error("500", "连接失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除来源")
    public Result<Void> delete(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source != null) {
            source.setDeleted(true);
            externalSourceMapper.updateById(source);
        }
        return Result.success("删除成功", null);
    }
}
