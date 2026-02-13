package com.lawfirm.domain.ai.repository;

import com.lawfirm.domain.ai.entity.AiPricingConfig;
import java.util.List;

/** AI定价配置仓储接口 */
public interface AiPricingConfigRepository {

  /**
   * 保存定价配置
   *
   * @param config 定价配置
   */
  void save(AiPricingConfig config);

  /**
   * 更新定价配置
   *
   * @param config 定价配置
   */
  void update(AiPricingConfig config);

  /**
   * 根据ID查询
   *
   * @param id 配置ID
   * @return 定价配置
   */
  AiPricingConfig findById(Long id);

  /**
   * 根据集成编码和模型名称查询
   *
   * @param integrationCode 集成编码
   * @param modelName 模型名称（可为null，表示查询默认定价）
   * @return 定价配置
   */
  AiPricingConfig findByCodeAndModel(String integrationCode, String modelName);

  /**
   * 根据集成编码查询所有定价配置
   *
   * @param integrationCode 集成编码
   * @return 定价配置列表
   */
  List<AiPricingConfig> findByIntegrationCode(String integrationCode);

  /**
   * 查询所有启用的定价配置
   *
   * @return 启用的定价配置列表
   */
  List<AiPricingConfig> findAllEnabled();

  /**
   * 查询所有定价配置（包括禁用的）
   *
   * @return 定价配置列表
   */
  List<AiPricingConfig> findAll();

  /**
   * 删除定价配置（逻辑删除）
   *
   * @param id 配置ID
   */
  void deleteById(Long id);
}
