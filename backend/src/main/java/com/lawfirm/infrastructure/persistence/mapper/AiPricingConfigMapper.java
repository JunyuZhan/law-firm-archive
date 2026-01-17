package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.domain.ai.entity.AiPricingConfig;
import com.lawfirm.domain.ai.repository.AiPricingConfigRepository;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI定价配置Mapper
 */
@Mapper
public interface AiPricingConfigMapper extends AiPricingConfigRepository {

    @Override
    @Insert("""
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

    @Override
    @Update("""
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

    @Override
    @Select("SELECT * FROM ai_pricing_config WHERE id = #{id} AND deleted = FALSE")
    AiPricingConfig findById(Long id);

    @Override
    @Select("""
        SELECT * FROM ai_pricing_config 
        WHERE integration_code = #{integrationCode} 
        AND (model_name = #{modelName} OR (#{modelName} IS NULL AND model_name IS NULL))
        AND deleted = FALSE AND enabled = TRUE
        LIMIT 1
        """)
    AiPricingConfig findByCodeAndModel(@Param("integrationCode") String integrationCode,
                                        @Param("modelName") String modelName);

    @Override
    @Select("""
        SELECT * FROM ai_pricing_config 
        WHERE integration_code = #{integrationCode} AND deleted = FALSE
        ORDER BY model_name NULLS FIRST
        """)
    List<AiPricingConfig> findByIntegrationCode(String integrationCode);

    @Override
    @Select("SELECT * FROM ai_pricing_config WHERE deleted = FALSE AND enabled = TRUE ORDER BY integration_code, model_name")
    List<AiPricingConfig> findAllEnabled();

    @Override
    @Select("SELECT * FROM ai_pricing_config WHERE deleted = FALSE ORDER BY integration_code, model_name")
    List<AiPricingConfig> findAll();

    @Override
    @Update("UPDATE ai_pricing_config SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void deleteById(Long id);
}
