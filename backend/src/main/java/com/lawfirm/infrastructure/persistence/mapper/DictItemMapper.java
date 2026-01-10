package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据字典项Mapper
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {

    /**
     * 根据字典类型ID查询
     */
    @Select("SELECT * FROM sys_dict_item WHERE dict_type_id = #{dictTypeId} AND status = 'ENABLED' AND deleted = false ORDER BY sort_order")
    List<DictItem> selectByTypeId(@Param("dictTypeId") Long dictTypeId);

    /**
     * 根据字典编码查询
     */
    @Select("SELECT i.* FROM sys_dict_item i " +
            "JOIN sys_dict_type t ON i.dict_type_id = t.id " +
            "WHERE t.code = #{typeCode} AND i.status = 'ENABLED' AND i.deleted = false AND t.deleted = false " +
            "ORDER BY i.sort_order")
    List<DictItem> selectByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 统计字典类型下的字典项数量
     * 问题489修复：用于删除前检查关联
     */
    @Select("SELECT COUNT(*) FROM sys_dict_item WHERE dict_type_id = #{typeId} AND deleted = false")
    long countByTypeId(@Param("typeId") Long typeId);
}
