package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 外部系统集成Mapper */
@Mapper
public interface ExternalIntegrationMapper extends BaseMapper<ExternalIntegration> {

  /**
   * 分页查询.
   *
   * @param page 分页对象
   * @param integrationType 集成类型
   * @param enabled 是否启用
   * @param keyword 关键词
   * @return 集成配置分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM sys_external_integration WHERE deleted = false "
          + "<if test='integrationType != null and integrationType != \"\"'>"
          + "  AND integration_type = #{integrationType} "
          + "</if>"
          + "<if test='enabled != null'>"
          + "  AND enabled = #{enabled} "
          + "</if>"
          + "<if test='keyword != null and keyword != \"\"'>"
          + "  AND (integration_name LIKE CONCAT('%', #{keyword}, '%') "
          + "       OR integration_code LIKE CONCAT('%', #{keyword}, '%')) "
          + "</if>"
          + "ORDER BY integration_type, id"
          + "</script>")
  @Results(
      id = "integrationResult",
      value = {
        @Result(
            column = "extra_config",
            property = "extraConfig",
            typeHandler =
                com.lawfirm.infrastructure.persistence.typehandler.PostgresJsonTypeHandler.class)
      })
  IPage<ExternalIntegration> selectPage(
      Page<ExternalIntegration> page,
      @Param("integrationType") String integrationType,
      @Param("enabled") Boolean enabled,
      @Param("keyword") String keyword);

  /**
   * 查询所有集成配置。
   *
   * @return 集成配置列表
   */
  @Select(
      "SELECT * FROM sys_external_integration WHERE deleted = false ORDER BY integration_type, id")
  @ResultMap("integrationResult")
  List<ExternalIntegration> selectAllIntegrations();

  /**
   * 根据编码查询.
   *
   * @param code 集成编码
   * @return 集成配置
   */
  @Select(
      "SELECT * FROM sys_external_integration WHERE integration_code = #{code} AND deleted = false")
  @ResultMap("integrationResult")
  ExternalIntegration selectByCode(@Param("code") String code);

  /**
   * 根据类型查询启用的集成.
   *
   * @param type 集成类型
   * @return 集成配置列表
   */
  @Select(
      "SELECT * FROM sys_external_integration WHERE integration_type = #{type} AND enabled = true AND deleted = false")
  @ResultMap("integrationResult")
  List<ExternalIntegration> selectEnabledByType(@Param("type") String type);

  /**
   * 根据类型查询集成配置（不限制启用状态）.
   *
   * @param type 集成类型
   * @return 集成配置
   */
  @Select(
      "SELECT * FROM sys_external_integration WHERE integration_type = #{type} AND deleted = false LIMIT 1")
  @ResultMap("integrationResult")
  ExternalIntegration selectByType(@Param("type") String type);

  /**
   * 更新测试结果.
   *
   * @param id 集成ID
   * @param result 测试结果
   * @param message 测试消息
   * @return 更新行数
   */
  @Update(
      "UPDATE sys_external_integration SET "
          + "last_test_time = NOW(), "
          + "last_test_result = #{result}, "
          + "last_test_message = #{message}, "
          + "updated_at = NOW() "
          + "WHERE id = #{id}")
  int updateTestResult(
      @Param("id") Long id, @Param("result") String result, @Param("message") String message);

  /**
   * 启用/禁用.
   *
   * @param id 集成ID
   * @param enabled 是否启用
   * @return 更新行数
   */
  @Update(
      "UPDATE sys_external_integration SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
  int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
}
