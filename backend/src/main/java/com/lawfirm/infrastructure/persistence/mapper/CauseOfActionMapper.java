package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.CauseOfAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 案由/罪名 Mapper
 */
@Mapper
public interface CauseOfActionMapper extends BaseMapper<CauseOfAction> {

    /**
     * 根据代码和类型获取案由
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE code = #{code} AND cause_type = #{causeType} AND is_active = TRUE LIMIT 1")
    CauseOfAction findByCodeAndType(@Param("code") String code, @Param("causeType") String causeType);

    /**
     * 根据代码获取案由名称
     */
    @Select("SELECT name FROM sys_cause_of_action WHERE code = #{code} AND cause_type = #{causeType} AND is_active = TRUE LIMIT 1")
    String findNameByCode(@Param("code") String code, @Param("causeType") String causeType);

    /**
     * 按类型获取所有案由
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} AND is_active = TRUE ORDER BY sort_order")
    List<CauseOfAction> findAllByType(@Param("causeType") String causeType);

    /**
     * 按类型和层级获取案由
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} AND level = #{level} AND is_active = TRUE ORDER BY sort_order")
    List<CauseOfAction> findByTypeAndLevel(@Param("causeType") String causeType, @Param("level") Integer level);

    /**
     * 获取某分类下的所有案由
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} AND category_code = #{categoryCode} AND is_active = TRUE ORDER BY sort_order")
    List<CauseOfAction> findByCategory(@Param("causeType") String causeType, @Param("categoryCode") String categoryCode);

    /**
     * 搜索案由（按名称模糊匹配）
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE cause_type = #{causeType} AND name LIKE CONCAT('%', #{keyword}, '%') AND is_active = TRUE ORDER BY sort_order LIMIT 50")
    List<CauseOfAction> searchByName(@Param("causeType") String causeType, @Param("keyword") String keyword);

    /**
     * 根据父代码获取子案由
     */
    @Select("SELECT * FROM sys_cause_of_action WHERE parent_code = #{parentCode} AND cause_type = #{causeType} AND is_active = TRUE ORDER BY sort_order")
    List<CauseOfAction> findByParentCode(@Param("parentCode") String parentCode, @Param("causeType") String causeType);

    /**
     * 检查代码是否存在（同一类型下）
     */
    @Select("SELECT COUNT(*) > 0 FROM sys_cause_of_action WHERE code = #{code} AND cause_type = #{causeType}")
    boolean existsByCodeAndType(@Param("code") String code, @Param("causeType") String causeType);

    /**
     * 获取最大排序号（按类型和分类）
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM sys_cause_of_action WHERE cause_type = #{causeType} AND category_code = #{categoryCode}")
    Integer getMaxSortOrder(@Param("causeType") String causeType, @Param("categoryCode") String categoryCode);
}
