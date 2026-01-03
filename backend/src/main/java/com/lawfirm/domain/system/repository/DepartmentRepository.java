package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门仓储
 */
@Repository
public class DepartmentRepository extends AbstractRepository<DepartmentMapper, Department> {

    /**
     * 查询子部门列表
     */
    public List<Department> findByParentId(Long parentId) {
        return baseMapper.selectByParentId(parentId);
    }

    /**
     * 查询所有部门
     */
    public List<Department> findAll() {
        return baseMapper.selectAll();
    }

    /**
     * 检查是否有子部门
     */
    public boolean hasChildren(Long parentId) {
        List<Department> children = baseMapper.selectByParentId(parentId);
        return children != null && !children.isEmpty();
    }
}
