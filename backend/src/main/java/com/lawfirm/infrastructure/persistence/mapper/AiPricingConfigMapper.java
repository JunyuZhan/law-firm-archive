package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.domain.ai.entity.AiPricingConfig;
import com.lawfirm.domain.ai.repository.AiPricingConfigRepository;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** AI定价配置Mapper */
@Mapper
public interface AiPricingConfigMapper extends AiPricingConfigRepository {

  /**
   * 保存AI定价配置.
   *
   * @param config AI定价配置实体
   */
  @Override
  @Insert(
      """
        INSERT INTO ai_pricing_config (
            integration_code, model_name, prompt_price, completion_price,
            per_call_price, pricing_mode, enabled, created_at, created_by
        ) VALUES (
            #{integrationCode}, #{modelName}, #{promptPrice}, #{completionPrice},
            #{perCallPrice}, #{pricingMode}, #{enabled}, CURRENT_TIMESTAMP, #{createdBy}
        )
        """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void save(AiPricingConfig config);

  /**
   * 更新AI定价配置.
   *
   * @param config AI定价配置实体
   */
  @Override
  @Update(
      """
        UPDATE ai_pricing_config SET
            integration_code = #{integrationCode},
            model_name = #{modelName},
            prompt_price = #{promptPrice},
            completion_price = #{completionPrice},
            per_call_price = #{perCallPrice},
            pricing_mode = #{pricingMode},
            enabled = #{enabled},
            updated_at = CURRENT_TIMESTAMP,
            updated_by = #{updatedBy}
        WHERE id = #{id} AND deleted = FALSE
        """)
  void update(AiPricingConfig config);

  /**
   * 根据ID查询AI定价配置.
   *
   * @param id 配置ID
   * @return AI定价配置实体
   */
  @Override
  @Select("SELECT * FROM ai_pricing_config WHERE id = #{id} AND deleted = FALSE")
  AiPricingConfig findById(Long id);

  /**
   * 根据集成代码和模型名称查询AI定价配置.
   *
   * @param integrationCode 集成代码
   * @param modelName 模型名称
   * @return AI定价配置实体
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_pricing_config
        WHERE integration_code = #{integrationCode}
        AND (model_name = #{modelName} OR (#{modelName} IS NULL AND model_name IS NULL))
        AND deleted = FALSE AND enabled = TRUE
        LIMIT 1
        """)
  AiPricingConfig findByCodeAndModel(
      @Param("integrationCode") String integrationCode, @Param("modelName") String modelName);

  /**
   * 根据集成代码查询AI定价配置列表.
   *
   * @param integrationCode 集成代码
   * @return AI定价配置列表
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_pricing_config
        WHERE integration_code = #{integrationCode} AND deleted = FALSE
        ORDER BY model_name NULLS FIRST
        """)
  List<AiPricingConfig> findByIntegrationCode(String integrationCode);

  /**
   * 查询所有启用的AI定价配置.
   *
   * @return AI定价配置列表
   */
  @Override
  @Select(
      "SELECT * FROM ai_pricing_config WHERE deleted = FALSE AND enabled = TRUE ORDER BY integration_code, model_name")
  List<AiPricingConfig> findAllEnabled();

  /**
   * 查询所有AI定价配置.
   *
   * @return AI定价配置列表
   */
  @Override
  @Select(
      "SELECT * FROM ai_pricing_config WHERE deleted = FALSE ORDER BY integration_code, model_name")
  List<AiPricingConfig> findAll();

  /**
   * 根据ID删除AI定价配置（逻辑删除）.
   *
   * @param id 配置ID
   */
  @Override
  @Update(
      "UPDATE ai_pricing_config SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  void deleteById(Long id);
}
