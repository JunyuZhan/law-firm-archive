package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 部门仓储。
 *
 * <p>提供部门数据的持久化操作。
 */
@Repository
public class DepartmentRepository extends AbstractRepository<DepartmentMapper, Department> {

  /**
   * 查询子部门列表。
   *
   * @param parentId 父部门ID
   * @return 子部门列表
   */
  public List<Department> findByParentId(final Long parentId) {
    return baseMapper.selectByParentId(parentId);
  }

  /**
   * 查询所有部门。
   *
   * @return 所有部门列表
   */
  public List<Department> findAll() {
    return baseMapper.selectAll();
  }

  /**
   * 检查是否有子部门。
   *
   * @param parentId 父部门ID
   * @return 是否有子部门
   */
  public boolean hasChildren(final Long parentId) {
    List<Department> children = baseMapper.selectByParentId(parentId);
    return children != null && !children.isEmpty();
  }
}
