package com.lawfirm.domain.ai.repository;

import com.lawfirm.domain.ai.entity.AiPricingConfig;

import java.util.List;

/**
 * AI定价配置仓储接口
 */
public interface AiPricingConfigRepository {

    /**
     * 保存定价配置
     */
    void save(AiPricingConfig config);

    /**
     * 更新定价配置
     */
    void update(AiPricingConfig config);

    /**
     * 根据ID查询
     */
    AiPricingConfig findById(Long id);

    /**
     * 根据集成编码和模型名称查询
     * @param integrationCode 集成编码
     * @param modelName 模型名称（可为null，表示查询默认定价）
     */
    AiPricingConfig findByCodeAndModel(String integrationCode, String modelName);

    /**
     * 根据集成编码查询所有定价配置
     */
    List<AiPricingConfig> findByIntegrationCode(String integrationCode);

    /**
     * 查询所有启用的定价配置
     */
    List<AiPricingConfig> findAllEnabled();

    /**
     * 查询所有定价配置（包括禁用的）
     */
    List<AiPricingConfig> findAll();

    /**
     * 删除定价配置（逻辑删除）
     */
    void deleteById(Long id);
}
