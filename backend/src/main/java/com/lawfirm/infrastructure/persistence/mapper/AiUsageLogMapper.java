package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.application.ai.dto.AiUsageLogDTO;
import com.lawfirm.application.ai.dto.AiUsageQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI使用记录Mapper
 */
@Mapper
public interface AiUsageLogMapper extends AiUsageLogRepository {

    // ==================== queryPage 和 getSummary 需要在 default 方法中实现 ====================

    /**
     * 分页查询（default 方法实现）
     */
    @Override
    default PageResult<AiUsageLogDTO> queryPage(AiUsageQueryDTO query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<AiUsageLogDTO> list = selectPageList(query, offset, query.getPageSize());
        long total = selectPageCount(query);
        return PageResult.of(list, total, query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取统计摘要（调用已有方法）
     */
    @Override
    default Map<String, Object> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        return getUserMonthlySummary(userId, startDate, endDate);
    }

    /**
     * 分页查询列表
     */
    @Select("""
        <script>
        SELECT id, user_id, user_name, department_id, department_name,
               integration_code, integration_name, model_name,
               request_type, business_type, business_id,
               prompt_tokens, completion_tokens, total_tokens,
               total_cost, user_cost, charge_ratio,
               success, error_message, duration_ms, created_at
        FROM ai_usage_log 
        WHERE 1=1
        <if test="query.userId != null"> AND user_id = #{query.userId} </if>
        <if test="query.integrationCode != null"> AND integration_code = #{query.integrationCode} </if>
        <if test="query.modelName != null"> AND model_name = #{query.modelName} </if>
        <if test="query.requestType != null"> AND request_type = #{query.requestType} </if>
        <if test="query.businessType != null"> AND business_type = #{query.businessType} </if>
        <if test="query.success != null"> AND success = #{query.success} </if>
        <if test="query.createdAtFrom != null"> AND created_at &gt;= #{query.createdAtFrom} </if>
        <if test="query.createdAtTo != null"> AND created_at &lt;= #{query.createdAtTo} </if>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<AiUsageLogDTO> selectPageList(@Param("query") AiUsageQueryDTO query,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 分页查询总数
     */
    @Select("""
        <script>
        SELECT COUNT(*) FROM ai_usage_log 
        WHERE 1=1
        <if test="query.userId != null"> AND user_id = #{query.userId} </if>
        <if test="query.integrationCode != null"> AND integration_code = #{query.integrationCode} </if>
        <if test="query.modelName != null"> AND model_name = #{query.modelName} </if>
        <if test="query.requestType != null"> AND request_type = #{query.requestType} </if>
        <if test="query.businessType != null"> AND business_type = #{query.businessType} </if>
        <if test="query.success != null"> AND success = #{query.success} </if>
        <if test="query.createdAtFrom != null"> AND created_at &gt;= #{query.createdAtFrom} </if>
        <if test="query.createdAtTo != null"> AND created_at &lt;= #{query.createdAtTo} </if>
        </script>
        """)
    long selectPageCount(@Param("query") AiUsageQueryDTO query);

    @Override
    @Insert("""
        INSERT INTO ai_usage_log (
            user_id, user_name, department_id, department_name,
            integration_id, integration_code, integration_name, model_name,
            request_type, business_type, business_id,
            prompt_tokens, completion_tokens, total_tokens,
            prompt_price, completion_price, total_cost, user_cost, charge_ratio,
            success, error_message, duration_ms, created_at
        ) VALUES (
            #{userId}, #{userName}, #{departmentId}, #{departmentName},
            #{integrationId}, #{integrationCode}, #{integrationName}, #{modelName},
            #{requestType}, #{businessType}, #{businessId},
            #{promptTokens}, #{completionTokens}, #{totalTokens},
            #{promptPrice}, #{completionPrice}, #{totalCost}, #{userCost}, #{chargeRatio},
            #{success}, #{errorMessage}, #{durationMs}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(AiUsageLog usageLog);

    @Override
    @Select("SELECT * FROM ai_usage_log WHERE id = #{id}")
    AiUsageLog findById(Long id);

    @Override
    @Select("""
        SELECT * FROM ai_usage_log 
        WHERE user_id = #{userId} 
        ORDER BY created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<AiUsageLog> findByUserId(@Param("userId") Long userId, 
                                   @Param("offset") int offset, 
                                   @Param("limit") int limit);

    @Override
    @Select("SELECT COUNT(*) FROM ai_usage_log WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    @Override
    @Select("""
        SELECT * FROM ai_usage_log 
        WHERE user_id = #{userId} 
        AND created_at >= #{startTime} AND created_at < #{endTime}
        ORDER BY created_at DESC
        """)
    List<AiUsageLog> findByUserIdAndTimeRange(@Param("userId") Long userId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    @Override
    @Select("""
        SELECT 
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log 
        WHERE user_id = #{userId} 
        AND created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        """)
    Map<String, Object> getUserMonthlySummary(@Param("userId") Long userId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Override
    @Select("""
        SELECT 
            integration_code,
            integration_name,
            model_name,
            COUNT(*) as call_count,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log 
        WHERE user_id = #{userId} 
        AND created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY integration_code, integration_name, model_name
        ORDER BY total_cost DESC
        """)
    List<Map<String, Object>> getUsageByModel(@Param("userId") Long userId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Override
    @Select("""
        SELECT 
            user_id,
            user_name,
            department_id,
            department_name,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log 
        WHERE created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY user_id, user_name, department_id, department_name
        ORDER BY total_cost DESC
        """)
    List<Map<String, Object>> getAllUsersMonthlySummary(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Override
    @Select("""
        SELECT 
            department_id,
            department_name,
            COUNT(DISTINCT user_id) as user_count,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log 
        WHERE created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        AND department_id IS NOT NULL
        GROUP BY department_id, department_name
        ORDER BY total_cost DESC
        """)
    List<Map<String, Object>> getDepartmentSummary(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Override
    @Select("""
        SELECT 
            user_id,
            user_name,
            department_id,
            department_name,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost,
            MAX(charge_ratio) as charge_ratio
        FROM ai_usage_log 
        WHERE EXTRACT(YEAR FROM created_at) = #{year}
        AND EXTRACT(MONTH FROM created_at) = #{month}
        GROUP BY user_id, user_name, department_id, department_name
        HAVING SUM(user_cost) > 0
        ORDER BY user_cost DESC
        """)
    List<Map<String, Object>> getUsersForBilling(@Param("year") int year, @Param("month") int month);

    // ==================== 新增方法 ====================

    @Override
    @Select("""
        SELECT 
            DATE(created_at) as date,
            COUNT(*) as call_count,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log 
        WHERE user_id = #{userId}
        AND created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY DATE(created_at)
        ORDER BY date
        """)
    List<Map<String, Object>> getUsageTrend(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Override
    default List<com.lawfirm.application.ai.dto.AiUsageSummaryDTO> getAllUsersSummary(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rawData = getAllUsersMonthlySummary(startDate, endDate);
        return rawData.stream()
                .map(row -> com.lawfirm.application.ai.dto.AiUsageSummaryDTO.builder()
                        .userId(row.get("user_id") != null ? ((Number) row.get("user_id")).longValue() : null)
                        .userName((String) row.get("user_name"))
                        .departmentId(row.get("department_id") != null ? ((Number) row.get("department_id")).longValue() : null)
                        .departmentName((String) row.get("department_name"))
                        .totalCalls(row.get("total_calls") != null ? ((Number) row.get("total_calls")).intValue() : 0)
                        .totalTokens(row.get("total_tokens") != null ? ((Number) row.get("total_tokens")).longValue() : 0L)
                        .promptTokens(row.get("prompt_tokens") != null ? ((Number) row.get("prompt_tokens")).longValue() : 0L)
                        .completionTokens(row.get("completion_tokens") != null ? ((Number) row.get("completion_tokens")).longValue() : 0L)
                        .totalCost(row.get("total_cost") != null ? new java.math.BigDecimal(row.get("total_cost").toString()) : java.math.BigDecimal.ZERO)
                        .userCost(row.get("user_cost") != null ? new java.math.BigDecimal(row.get("user_cost").toString()) : java.math.BigDecimal.ZERO)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Select("""
        SELECT DISTINCT user_id, user_name
        FROM ai_usage_log 
        WHERE created_at >= #{startDate}::timestamp 
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        """)
    List<Map<String, Object>> getActiveUsersInPeriod(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
}
