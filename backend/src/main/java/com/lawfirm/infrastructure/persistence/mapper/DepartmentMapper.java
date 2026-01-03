package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门 Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 查询子部门列表
     */
    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
    List<Department> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有部门（树形结构用）
     */
    @Select("SELECT * FROM sys_department WHERE deleted = false ORDER BY sort_order")
    List<Department> selectAll();
}
