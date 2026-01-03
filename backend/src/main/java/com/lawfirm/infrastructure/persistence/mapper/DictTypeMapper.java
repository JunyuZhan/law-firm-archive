package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.DictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据字典类型Mapper
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

    /**
     * 查询所有启用的字典类型
     */
    @Select("SELECT * FROM sys_dict_type WHERE status = 'ENABLED' AND deleted = false ORDER BY id")
    List<DictType> selectEnabledTypes();

    /**
     * 根据编码查询
     */
    @Select("SELECT * FROM sys_dict_type WHERE code = #{code} AND deleted = false")
    DictType selectByCode(@Param("code") String code);
}
