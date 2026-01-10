package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.security.AesEncryptionService;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.repository.ExternalIntegrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 外部系统集成应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIntegrationAppService {

    private final ExternalIntegrationRepository integrationRepository;
    private final ExternalIntegrationMapper integrationMapper;
    private final AesEncryptionService encryptionService;

    // ✅ 连接超时时间可配置
    @Value("${law-firm.integration.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${law-firm.integration.read-timeout-ms:5000}")
    private int readTimeoutMs;

    /**
     * 分页查询
     */
    public PageResult<ExternalIntegrationDTO> listIntegrations(ExternalIntegrationQueryDTO query) {
        IPage<ExternalIntegration> page = integrationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getIntegrationType(),
                query.getEnabled(),
                query.getKeyword()
        );
        
        List<ExternalIntegrationDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取所有集成配置
     */
    public List<ExternalIntegrationDTO> listAllIntegrations() {
        return integrationMapper.selectAllIntegrations().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定类型的启用集成
     */
    public List<ExternalIntegrationDTO> listEnabledByType(String type) {
        return integrationMapper.selectEnabledByType(type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取集成配置
     */
    public ExternalIntegrationDTO getIntegrationById(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        return toDTO(integration);
    }

    /**
     * 根据编码获取集成配置
     */
    public ExternalIntegrationDTO getIntegrationByCode(String code) {
        ExternalIntegration integration = integrationMapper.selectByCode(code);
        if (integration == null) {
            throw new BusinessException("集成配置不存在: " + code);
        }
        return toDTO(integration);
    }

    /**
     * 更新集成配置
     */
    @Transactional
    public void updateIntegration(UpdateExternalIntegrationCommand command) {
        // ✅ 权限验证：只有管理员才能修改集成配置
        if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能修改集成配置");
        }

        ExternalIntegration integration = integrationRepository.getByIdOrThrow(command.getId(), "集成配置不存在");

        // 记录是否修改了敏感信息
        boolean sensitiveChanged = false;

        if (StringUtils.hasText(command.getApiUrl())) {
            integration.setApiUrl(command.getApiUrl());
        }
        if (StringUtils.hasText(command.getApiKey())) {
            // ✅ 问题531已修复：使用AES加密存储API密钥
            String encryptedKey = encryptionService.encrypt(command.getApiKey());
            integration.setApiKey(encryptedKey);
            sensitiveChanged = true;
        }
        if (StringUtils.hasText(command.getApiSecret())) {
            // ✅ 问题531已修复：使用AES加密存储API密钥
            String encryptedSecret = encryptionService.encrypt(command.getApiSecret());
            integration.setApiSecret(encryptedSecret);
            sensitiveChanged = true;
        }
        if (StringUtils.hasText(command.getAuthType())) {
            integration.setAuthType(command.getAuthType());
        }
        if (command.getExtraConfig() != null) {
            integration.setExtraConfig(command.getExtraConfig());
        }
        if (StringUtils.hasText(command.getDescription())) {
            integration.setDescription(command.getDescription());
        }

        integrationRepository.updateById(integration);

        // ✅ 敏感信息修改时记录审计日志
        if (sensitiveChanged) {
            log.warn("【敏感操作】集成配置敏感信息已修改: integrationCode={}, operator={}, operatorName={}",
                    integration.getIntegrationCode(), SecurityUtils.getUserId(), SecurityUtils.getUsername());
        } else {
            log.info("集成配置更新成功: {}", integration.getIntegrationCode());
        }
    }

    /**
     * 启用集成
     */
    @Transactional
    public void enableIntegration(Long id) {
        // ✅ 权限验证：只有管理员才能启用集成
        if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能启用集成");
        }

        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");

        // 检查必要配置是否完整
        if (!StringUtils.hasText(integration.getApiUrl())) {
            throw new BusinessException("请先配置API地址");
        }
        if (!StringUtils.hasText(integration.getApiKey()) && !StringUtils.hasText(integration.getApiSecret())) {
            throw new BusinessException("请先配置API密钥");
        }

        integrationMapper.updateEnabled(id, true);
        log.info("集成已启用: integrationCode={}, operator={}", integration.getIntegrationCode(), SecurityUtils.getUserId());
    }

    /**
     * 禁用集成
     */
    @Transactional
    public void disableIntegration(Long id) {
        // ✅ 权限验证：只有管理员才能禁用集成
        if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能禁用集成");
        }

        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        integrationMapper.updateEnabled(id, false);
        log.info("集成已禁用: integrationCode={}, operator={}", integration.getIntegrationCode(), SecurityUtils.getUserId());
    }

    /**
     * 测试连接
     * 注意：不使用@Transactional，因为网络请求可能耗时较长
     */
    public ExternalIntegrationDTO testConnection(Long id) {
        // ✅ 权限验证：只有管理员才能测试连接
        if (!SecurityUtils.hasAnyRole("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能测试连接");
        }

        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");

        if (!StringUtils.hasText(integration.getApiUrl())) {
            updateTestResultInTransaction(id, ExternalIntegration.TEST_FAILED, "API地址未配置");
            throw new BusinessException("API地址未配置");
        }

        String result;
        String message;

        try {
            // 简单的连通性测试（去除可能的空格）
            String apiUrl = integration.getApiUrl().trim();
            URL url = URI.create(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // ✅ 使用可配置的超时时间
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 500) {
                result = ExternalIntegration.TEST_SUCCESS;
                message = "连接成功，响应码: " + responseCode;
            } else {
                result = ExternalIntegration.TEST_FAILED;
                message = "连接失败，响应码: " + responseCode;
            }
            connection.disconnect();
        } catch (Exception e) {
            result = ExternalIntegration.TEST_FAILED;
            message = "连接异常: " + e.getMessage();
            log.error("测试连接失败: {}", integration.getIntegrationCode(), e);
        }

        // ✅ 单独的事务更新测试结果
        updateTestResultInTransaction(id, result, message);

        // 重新获取更新后的记录
        return getIntegrationById(id);
    }

    /**
     * 在单独事务中更新测试结果（避免网络请求占用事务）
     */
    @Transactional
    public void updateTestResultInTransaction(Long id, String result, String message) {
        integrationMapper.updateTestResult(id, result, message);
    }

    // ============ 通用集成查询方法（消除代码重复 - 问题551）============

    /**
     * 根据类型获取第一个启用的集成配置（通用方法）
     * @param type 集成类型
     * @return 集成配置，如果没有启用的则返回null
     */
    public ExternalIntegration getFirstEnabledIntegrationByType(String type) {
        List<ExternalIntegration> list = integrationMapper.selectEnabledByType(type);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据类型获取所有启用的集成配置列表（通用方法）
     * @param type 集成类型
     * @return 集成配置列表
     */
    public List<ExternalIntegration> getAllEnabledIntegrationsByType(String type) {
        return integrationMapper.selectEnabledByType(type);
    }

    /**
     * 根据ID获取指定类型的集成配置（通用方法）
     * @param id 集成ID
     * @param expectedType 期望的类型
     * @param typeName 类型名称（用于错误提示）
     * @return 集成配置
     */
    public ExternalIntegration getIntegrationByIdAndType(Long id, String expectedType, String typeName) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, typeName + "集成配置不存在");
        if (!expectedType.equals(integration.getIntegrationType())) {
            throw new BusinessException("指定的集成配置不是" + typeName + "类型");
        }
        return integration;
    }

    // ============ 解密API密钥方法（问题531修复）============

    /**
     * 获取解密后的API密钥
     * 供需要实际使用密钥调用外部API时使用
     * 
     * @param integrationId 集成配置ID
     * @return 解密后的API密钥
     */
    public String getDecryptedApiKey(Long integrationId) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(integrationId, "集成配置不存在");
        
        if (!StringUtils.hasText(integration.getApiKey())) {
            return null;
        }
        
        try {
            return encryptionService.decrypt(integration.getApiKey());
        } catch (Exception e) {
            // 兼容历史未加密数据
            log.warn("API密钥解密失败，可能是未加密的历史数据: integrationId={}", integrationId);
            return integration.getApiKey();
        }
    }

    /**
     * 获取解密后的API密钥Secret
     * 供需要实际使用密钥调用外部API时使用
     * 
     * @param integrationId 集成配置ID
     * @return 解密后的API密钥Secret
     */
    public String getDecryptedApiSecret(Long integrationId) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(integrationId, "集成配置不存在");
        
        if (!StringUtils.hasText(integration.getApiSecret())) {
            return null;
        }
        
        try {
            return encryptionService.decrypt(integration.getApiSecret());
        } catch (Exception e) {
            // 兼容历史未加密数据
            log.warn("API密钥Secret解密失败，可能是未加密的历史数据: integrationId={}", integrationId);
            return integration.getApiSecret();
        }
    }

    /**
     * 获取解密后的完整集成配置
     * 包含解密后的API密钥，用于实际调用外部API
     * 
     * @param id 集成配置ID
     * @return 包含解密密钥的集成实体
     */
    public ExternalIntegration getIntegrationWithDecryptedKeys(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        
        // 解密API密钥
        if (StringUtils.hasText(integration.getApiKey())) {
            try {
                integration.setApiKey(encryptionService.decrypt(integration.getApiKey()));
            } catch (Exception e) {
                log.warn("API密钥解密失败，使用原值: integrationId={}", id);
            }
        }
        
        if (StringUtils.hasText(integration.getApiSecret())) {
            try {
                integration.setApiSecret(encryptionService.decrypt(integration.getApiSecret()));
            } catch (Exception e) {
                log.warn("API密钥Secret解密失败，使用原值: integrationId={}", id);
            }
        }
        
        return integration;
    }

    // ============ AI集成便捷方法 ============

    /**
     * 获取AI集成配置（供业务使用）
     * 返回第一个启用的 AI 集成
     */
    public ExternalIntegration getEnabledAIIntegration() {
        return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_AI);
    }

    /**
     * 获取所有启用的 AI 集成列表
     */
    public List<ExternalIntegration> getAllEnabledAIIntegrations() {
        return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_AI);
    }

    /**
     * 根据 ID 获取 AI 集成配置
     */
    public ExternalIntegration getAIIntegrationById(Long id) {
        return getIntegrationByIdAndType(id, ExternalIntegration.TYPE_AI, "AI");
        }

    // ============ 档案系统集成便捷方法 ============

    /**
     * 获取档案系统集成配置（供业务使用）
     */
    public ExternalIntegration getEnabledArchiveIntegration() {
        return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_ARCHIVE);
    }

    /**
     * 获取所有启用的档案系统集成列表
     */
    public List<ExternalIntegration> getAllEnabledArchiveIntegrations() {
        return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_ARCHIVE);
    }

    /**
     * 根据ID获取档案系统集成配置
     */
    public ExternalIntegration getArchiveIntegrationById(Long id) {
        return getIntegrationByIdAndType(id, ExternalIntegration.TYPE_ARCHIVE, "档案系统");
        }

    // ============ OCR集成便捷方法 ============

    /**
     * 获取OCR集成配置
     */
    public ExternalIntegration getEnabledOCRIntegration() {
        return getFirstEnabledIntegrationByType(ExternalIntegration.TYPE_OCR);
    }

    /**
     * 获取所有启用的OCR集成列表
     */
    public List<ExternalIntegration> getAllEnabledOCRIntegrations() {
        return getAllEnabledIntegrationsByType(ExternalIntegration.TYPE_OCR);
    }

    /**
     * 转换为DTO（脱敏处理）
     */
    private ExternalIntegrationDTO toDTO(ExternalIntegration entity) {
        ExternalIntegrationDTO dto = new ExternalIntegrationDTO();
        dto.setId(entity.getId());
        dto.setIntegrationCode(entity.getIntegrationCode());
        dto.setIntegrationName(entity.getIntegrationName());
        dto.setIntegrationType(entity.getIntegrationType());
        dto.setDescription(entity.getDescription());
        dto.setApiUrl(entity.getApiUrl());
        
        // API密钥脱敏显示
        if (StringUtils.hasText(entity.getApiKey())) {
            String key = entity.getApiKey();
            if (key.length() > 8) {
                dto.setApiKey(key.substring(0, 4) + "****" + key.substring(key.length() - 4));
            } else {
                dto.setApiKey("****");
            }
        }
        dto.setHasApiSecret(StringUtils.hasText(entity.getApiSecret()));
        
        dto.setAuthType(entity.getAuthType());
        dto.setExtraConfig(entity.getExtraConfig());
        dto.setEnabled(entity.getEnabled());
        dto.setLastTestTime(entity.getLastTestTime());
        dto.setLastTestResult(entity.getLastTestResult());
        dto.setLastTestMessage(entity.getLastTestMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
}

