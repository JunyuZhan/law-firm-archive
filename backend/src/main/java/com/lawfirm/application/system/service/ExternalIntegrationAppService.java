package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.repository.ExternalIntegrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
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
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(command.getId(), "集成配置不存在");
        
        if (StringUtils.hasText(command.getApiUrl())) {
            integration.setApiUrl(command.getApiUrl());
        }
        if (StringUtils.hasText(command.getApiKey())) {
            integration.setApiKey(command.getApiKey());
        }
        if (StringUtils.hasText(command.getApiSecret())) {
            // TODO: 实际应用中应加密存储
            integration.setApiSecret(command.getApiSecret());
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
        log.info("集成配置更新成功: {}", integration.getIntegrationCode());
    }

    /**
     * 启用集成
     */
    @Transactional
    public void enableIntegration(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        
        // 检查必要配置是否完整
        if (!StringUtils.hasText(integration.getApiUrl())) {
            throw new BusinessException("请先配置API地址");
        }
        if (!StringUtils.hasText(integration.getApiKey()) && !StringUtils.hasText(integration.getApiSecret())) {
            throw new BusinessException("请先配置API密钥");
        }
        
        integrationMapper.updateEnabled(id, true);
        log.info("集成已启用: {}", integration.getIntegrationCode());
    }

    /**
     * 禁用集成
     */
    @Transactional
    public void disableIntegration(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        integrationMapper.updateEnabled(id, false);
        log.info("集成已禁用: {}", integration.getIntegrationCode());
    }

    /**
     * 测试连接
     */
    @Transactional
    public ExternalIntegrationDTO testConnection(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "集成配置不存在");
        
        if (!StringUtils.hasText(integration.getApiUrl())) {
            integrationMapper.updateTestResult(id, ExternalIntegration.TEST_FAILED, "API地址未配置");
            throw new BusinessException("API地址未配置");
        }
        
        String result;
        String message;
        
        try {
            // 简单的连通性测试（去除可能的空格）
            String apiUrl = integration.getApiUrl().trim();
            URL url = URI.create(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
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
        
        integrationMapper.updateTestResult(id, result, message);
        
        // 重新获取更新后的记录
        return getIntegrationById(id);
    }

    /**
     * 获取AI集成配置（供业务使用）
     * 返回第一个启用的 AI 集成
     */
    public ExternalIntegration getEnabledAIIntegration() {
        List<ExternalIntegration> list = integrationMapper.selectEnabledByType(ExternalIntegration.TYPE_AI);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 获取所有启用的 AI 集成列表
     */
    public List<ExternalIntegration> getAllEnabledAIIntegrations() {
        return integrationMapper.selectEnabledByType(ExternalIntegration.TYPE_AI);
    }

    /**
     * 根据 ID 获取 AI 集成配置
     */
    public ExternalIntegration getAIIntegrationById(Long id) {
        ExternalIntegration integration = integrationRepository.getByIdOrThrow(id, "AI 集成配置不存在");
        if (!ExternalIntegration.TYPE_AI.equals(integration.getIntegrationType())) {
            throw new BusinessException("指定的集成配置不是 AI 类型");
        }
        return integration;
    }

    /**
     * 获取档案系统集成配置（供业务使用）
     */
    public ExternalIntegration getEnabledArchiveIntegration() {
        List<ExternalIntegration> list = integrationMapper.selectEnabledByType(ExternalIntegration.TYPE_ARCHIVE);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
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

