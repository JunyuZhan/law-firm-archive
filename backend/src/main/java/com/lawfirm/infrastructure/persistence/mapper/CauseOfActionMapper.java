package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.CauseOfAction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 案由/罪名 Mapper */
@Mapper
public interface CauseOfActionMapper extends BaseMapper<CauseOfAction> {

  /**
   * 根据代码和类型获取案由.
   *
   * @param code 代码
   * @param causeType 案由类型
   * @return 案由信息
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE code = #{code} "
          + "AND cause_type = #{causeType} AND is_active = TRUE LIMIT 1")
  CauseOfAction findByCodeAndType(@Param("code") String code, @Param("causeType") String causeType);

  /**
   * 根据代码获取案由名称.
   *
   * @param code 代码
   * @param causeType 案由类型
   * @return 案由名称
   */
  @Select(
      "SELECT name FROM sys_cause_of_action WHERE code = #{code} "
          + "AND cause_type = #{causeType} AND is_active = TRUE LIMIT 1")
  String findNameByCode(@Param("code") String code, @Param("causeType") String causeType);

  /**
   * 按类型获取所有案由.
   *
   * @param causeType 案由类型
   * @return 案由列表
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} AND is_active = TRUE ORDER BY sort_order")
  List<CauseOfAction> findAllByType(@Param("causeType") String causeType);

  /**
   * 按类型和层级获取案由.
   *
   * @param causeType 案由类型
   * @param level 层级
   * @return 案由列表
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} "
          + "AND level = #{level} AND is_active = TRUE ORDER BY sort_order")
  List<CauseOfAction> findByTypeAndLevel(
      @Param("causeType") String causeType, @Param("level") Integer level);

  /**
   * 获取某分类下的所有案由.
   *
   * @param causeType 案由类型
   * @param categoryCode 分类代码
   * @return 案由列表
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} "
          + "AND category_code = #{categoryCode} AND is_active = TRUE "
          + "ORDER BY sort_order")
  List<CauseOfAction> findByCategory(
      @Param("causeType") String causeType, @Param("categoryCode") String categoryCode);

  /**
   * 搜索案由（按名称模糊匹配）.
   *
   * @param causeType 案由类型
   * @param keyword 关键词
   * @return 案由列表
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} "
          + "AND name LIKE CONCAT('%', #{keyword}, '%') AND is_active = TRUE "
          + "ORDER BY sort_order LIMIT 50")
  List<CauseOfAction> searchByName(
      @Param("causeType") String causeType, @Param("keyword") String keyword);

  /**
   * 根据父代码获取子案由.
   *
   * @param parentCode 父代码
   * @param causeType 案由类型
   * @return 子案由列表
   */
  @Select(
      "SELECT * FROM sys_cause_of_action WHERE parent_code = #{parentCode} "
          + "AND cause_type = #{causeType} AND is_active = TRUE "
          + "ORDER BY sort_order")
  List<CauseOfAction> findByParentCode(
      @Param("parentCode") String parentCode, @Param("causeType") String causeType);

  /**
   * 检查代码是否存在（同一类型下）.
   *
   * @param code 代码
   * @param causeType 案由类型
   * @return 是否存在
   */
  @Select(
      "SELECT COUNT(*) > 0 FROM sys_cause_of_action WHERE code = #{code} AND cause_type = #{causeType}")
  boolean existsByCodeAndType(@Param("code") String code, @Param("causeType") String causeType);

  /**
   * 获取最大排序号（按类型和分类）.
   *
   * @param causeType 案由类型
   * @param categoryCode 分类代码
   * @return 最大排序号
   */
  @Select(
      "SELECT COALESCE(MAX(sort_order), 0) FROM sys_cause_of_action "
          + "WHERE cause_type = #{causeType} AND category_code = #{categoryCode}")
  Integer getMaxSortOrder(
      @Param("causeType") String causeType, @Param("categoryCode") String categoryCode);
}
