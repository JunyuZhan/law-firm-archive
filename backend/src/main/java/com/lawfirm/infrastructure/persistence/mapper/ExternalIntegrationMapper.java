package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 外部系统集成Mapper
 */
@Mapper
public interface ExternalIntegrationMapper extends BaseMapper<ExternalIntegration> {

    /**
     * 分页查询
     */
    @Select("<script>" +
            "SELECT * FROM sys_external_integration WHERE deleted = false " +
            "<if test='integrationType != null and integrationType != \"\"'>" +
            "  AND integration_type = #{integrationType} " +
            "</if>" +
            "<if test='enabled != null'>" +
            "  AND enabled = #{enabled} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (integration_name LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR integration_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY integration_type, id" +
            "</script>")
    @Results(id = "integrationResult", value = {
            @Result(column = "extra_config", property = "extraConfig", 
                    typeHandler = com.lawfirm.infrastructure.persistence.typehandler.PostgresJsonTypeHandler.class)
    })
    IPage<ExternalIntegration> selectPage(
            Page<ExternalIntegration> page,
            @Param("integrationType") String integrationType,
            @Param("enabled") Boolean enabled,
            @Param("keyword") String keyword
    );

    /**
     * 查询所有集成配置
     */
    @Select("SELECT * FROM sys_external_integration WHERE deleted = false ORDER BY integration_type, id")
    @ResultMap("integrationResult")
    List<ExternalIntegration> selectAllIntegrations();

    /**
     * 根据编码查询
     */
    @Select("SELECT * FROM sys_external_integration WHERE integration_code = #{code} AND deleted = false")
    @ResultMap("integrationResult")
    ExternalIntegration selectByCode(@Param("code") String code);

    /**
     * 根据类型查询启用的集成
     */
    @Select("SELECT * FROM sys_external_integration WHERE integration_type = #{type} AND enabled = true AND deleted = false")
    @ResultMap("integrationResult")
    List<ExternalIntegration> selectEnabledByType(@Param("type") String type);

    /**
     * 根据类型查询集成配置（不限制启用状态）
     */
    @Select("SELECT * FROM sys_external_integration WHERE integration_type = #{type} AND deleted = false LIMIT 1")
    @ResultMap("integrationResult")
    ExternalIntegration selectByType(@Param("type") String type);

    /**
     * 更新测试结果
     */
    @Update("UPDATE sys_external_integration SET " +
            "last_test_time = NOW(), " +
            "last_test_result = #{result}, " +
            "last_test_message = #{message}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateTestResult(@Param("id") Long id, @Param("result") String result, @Param("message") String message);

    /**
     * 启用/禁用
     */
    @Update("UPDATE sys_external_integration SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
}

