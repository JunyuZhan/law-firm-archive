package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.common.base.BaseRepository;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.infrastructure.persistence.mapper.EmployeeMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 员工档案 Repository
 */
@Repository
public class EmployeeRepository extends AbstractRepository<EmployeeMapper, Employee> {

    /**
     * 根据用户ID查询员工档案
     */
    public Optional<Employee> findByUserId(Long userId) {
        Employee employee = baseMapper.selectByUserId(userId);
        return Optional.ofNullable(employee);
    }

    /**
     * 根据工号查询
     */
    public Optional<Employee> findByEmployeeNo(String employeeNo) {
        Employee employee = lambdaQuery()
                .eq(Employee::getEmployeeNo, employeeNo)
                .one();
        return Optional.ofNullable(employee);
    }

    /**
     * 统计指定职级的员工数量
     * 问题297修复：用于删除职级前检查关联
     */
    public long countByLevel(String levelCode) {
        return lambdaQuery()
                .eq(Employee::getLevel, levelCode)
                .eq(Employee::getDeleted, false)
                .count();
    }
}

