package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Employee;
import com.lawfirm.infrastructure.persistence.mapper.EmployeeMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 员工档案 Repository. */
@Repository
public class EmployeeRepository extends AbstractRepository<EmployeeMapper, Employee> {

  /**
   * 根据用户ID查询员工档案.
   *
   * @param userId 用户ID
   * @return 员工档案
   */
  public Optional<Employee> findByUserId(final Long userId) {
    Employee employee = baseMapper.selectByUserId(userId);
    return Optional.ofNullable(employee);
  }

  /**
   * 根据工号查询.
   *
   * @param employeeNo 工号
   * @return 员工档案
   */
  public Optional<Employee> findByEmployeeNo(final String employeeNo) {
    Employee employee = lambdaQuery().eq(Employee::getEmployeeNo, employeeNo).one();
    return Optional.ofNullable(employee);
  }

  /**
   * 统计指定职级的员工数量 问题297修复：用于删除职级前检查关联.
   *
   * @param levelCode 职级编码
   * @return 员工数量
   */
  public long countByLevel(final String levelCode) {
    return lambdaQuery().eq(Employee::getLevel, levelCode).eq(Employee::getDeleted, false).count();
  }
}
