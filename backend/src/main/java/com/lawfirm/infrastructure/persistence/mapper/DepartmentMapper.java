package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.Department;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 部门 Mapper */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

  /**
   * 查询子部门列表.
   *
   * @param parentId 父部门ID
   * @return 子部门列表
   */
  @Select(
      "SELECT * FROM sys_department WHERE parent_id = #{parentId} AND deleted = false ORDER BY sort_order")
  List<Department> selectByParentId(@Param("parentId") Long parentId);

  /**
   * 查询所有部门（树形结构用）.
   *
   * @return 所有部门列表
   */
  @Select("SELECT * FROM sys_department WHERE deleted = false ORDER BY sort_order")
  List<Department> selectAll();

  /**
   * 查询直接子部门ID列表.
   *
   * @param parentId 父部门ID
   * @return 子部门ID列表
   */
  @Select("SELECT id FROM sys_department WHERE parent_id = #{parentId} AND deleted = false")
  List<Long> selectChildDeptIds(@Param("parentId") Long parentId);

  /**
   * 使用递归CTE一次性查询所有后代部门ID 避免递归Java调用导致的多次数据库查询.
   *
   * @param parentId 父部门ID
   * @return 所有后代部门ID列表
   */
  @Select(
      "WITH RECURSIVE dept_tree AS ("
          + "  SELECT id FROM sys_department WHERE parent_id = #{parentId} AND deleted = false"
          + "  UNION ALL"
          + "  SELECT d.id FROM sys_department d"
          + "  INNER JOIN dept_tree dt ON d.parent_id = dt.id"
          + "  WHERE d.deleted = false"
          + ") SELECT id FROM dept_tree")
  List<Long> selectAllDescendantDeptIds(@Param("parentId") Long parentId);
}
