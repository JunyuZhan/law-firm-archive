package com.lawfirm.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据权限辅助工具类
 *
 * <p>用于在Service层手动添加数据权限过滤条件 适用于复杂查询场景，或者不方便使用 @DataScope 注解的情况
 *
 * <p>使用示例：
 *
 * <pre>
 * // 方式1：使用 LambdaQueryWrapper
 * LambdaQueryWrapper<Matter> wrapper = new LambdaQueryWrapper<>();
 * dataScopeHelper.applyDataScope(wrapper, Matter::getDepartmentId, Matter::getCreatedBy);
 *
 * // 方式2：使用 QueryWrapper（指定字段名）
 * QueryWrapper<Matter> wrapper = new QueryWrapper<>();
 * dataScopeHelper.applyDataScope(wrapper, "department_id", "created_by");
 *
 * // 方式3：获取可访问的部门ID列表
 * List<Long> deptIds = dataScopeHelper.getAccessibleDeptIds();
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

  /** 部门仓储接口. */
  private final DepartmentRepository departmentRepository;

  /**
   * 应用数据权限过滤（LambdaQueryWrapper）
   *
   * @param wrapper 查询条件包装器
   * @param deptField 部门字段的getter方法引用
   * @param userField 用户字段的getter方法引用（创建人）
   * @param <T> 实体类型
   */
  public <T> void applyDataScope(
      final LambdaQueryWrapper<T> wrapper,
      final SFunction<T, Long> deptField,
      final SFunction<T, Long> userField) {
    String dataScope = getDataScope();
    Long userId = getUserId();
    Long deptId = getDeptId();

    switch (dataScope) {
      case "ALL":
        // 全部数据权限，不添加过滤条件
        log.debug("数据权限: ALL - 不添加过滤条件");
        break;

      case "DEPT_AND_CHILD":
        // 部门及下级部门
        if (deptId != null && deptField != null) {
          List<Long> deptIds = getDeptAndChildrenIds(deptId);
          wrapper.in(deptField, deptIds);
          log.debug("数据权限: DEPT_AND_CHILD - 部门ID列表: {}", deptIds);
        }
        break;

      case "DEPT":
        // 本部门
        if (deptId != null && deptField != null) {
          wrapper.eq(deptField, deptId);
          log.debug("数据权限: DEPT - 部门ID: {}", deptId);
        }
        break;

      case "SELF":
      default:
        // 仅本人
        if (userId != null && userField != null) {
          wrapper.eq(userField, userId);
          log.debug("数据权限: SELF - 用户ID: {}", userId);
        }
        break;
    }
  }

  /**
   * 应用数据权限过滤（QueryWrapper，指定字段名）
   *
   * @param wrapper 查询条件包装器
   * @param deptFieldName 部门字段名（如 "department_id"）
   * @param userFieldName 用户字段名（如 "created_by"）
   * @param <T> 实体类型
   */
  public <T> void applyDataScope(
      final QueryWrapper<T> wrapper, final String deptFieldName, final String userFieldName) {
    String dataScope = getDataScope();
    Long userId = getUserId();
    Long deptId = getDeptId();

    switch (dataScope) {
      case "ALL":
        log.debug("数据权限: ALL - 不添加过滤条件");
        break;

      case "DEPT_AND_CHILD":
        if (deptId != null && deptFieldName != null) {
          List<Long> deptIds = getDeptAndChildrenIds(deptId);
          wrapper.in(deptFieldName, deptIds);
          log.debug("数据权限: DEPT_AND_CHILD - 部门ID列表: {}", deptIds);
        }
        break;

      case "DEPT":
        if (deptId != null && deptFieldName != null) {
          wrapper.eq(deptFieldName, deptId);
          log.debug("数据权限: DEPT - 部门ID: {}", deptId);
        }
        break;

      case "SELF":
      default:
        if (userId != null && userFieldName != null) {
          wrapper.eq(userFieldName, userId);
          log.debug("数据权限: SELF - 用户ID: {}", userId);
        }
        break;
    }
  }

  /**
   * 应用数据权限过滤（仅按部门过滤）
   *
   * @param wrapper 查询条件包装器
   * @param deptField 部门字段的getter方法引用
   * @param <T> 实体类型
   */
  public <T> void applyDeptDataScope(
      final LambdaQueryWrapper<T> wrapper, final SFunction<T, Long> deptField) {
    String dataScope = getDataScope();
    Long deptId = getDeptId();

    if ("ALL".equals(dataScope) || deptField == null) {
      return;
    }

    if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
      List<Long> deptIds = getDeptAndChildrenIds(deptId);
      wrapper.in(deptField, deptIds);
    } else if ("DEPT".equals(dataScope) && deptId != null) {
      wrapper.eq(deptField, deptId);
    }
    // SELF 权限时不按部门过滤，由调用方决定是否按用户过滤
  }

  /**
   * 应用数据权限过滤（仅按用户过滤）
   *
   * @param wrapper 查询条件包装器
   * @param userField 用户字段的getter方法引用
   * @param <T> 实体类型
   */
  public <T> void applyUserDataScope(
      final LambdaQueryWrapper<T> wrapper, final SFunction<T, Long> userField) {
    String dataScope = getDataScope();
    Long userId = getUserId();

    if ("ALL".equals(dataScope) || userField == null) {
      return;
    }

    if ("SELF".equals(dataScope) && userId != null) {
      wrapper.eq(userField, userId);
    }
    // DEPT 和 DEPT_AND_CHILD 权限时不按用户过滤
  }

  /**
   * 获取当前用户可访问的部门ID列表
   *
   * @return 部门ID列表，ALL权限返回null表示可访问所有
   */
  public List<Long> getAccessibleDeptIds() {
    String dataScope = getDataScope();
    Long deptId = getDeptId();

    if ("ALL".equals(dataScope)) {
      return null; // null 表示可访问所有部门
    }

    if (deptId == null) {
      return Collections.emptyList();
    }

    if ("DEPT_AND_CHILD".equals(dataScope)) {
      return getDeptAndChildrenIds(deptId);
    } else if ("DEPT".equals(dataScope)) {
      return Collections.singletonList(deptId);
    }

    // SELF 权限返回空列表（不按部门过滤）
    return Collections.emptyList();
  }

  /**
   * 判断当前用户是否有权限访问指定部门的数据
   *
   * @param targetDeptId 目标部门ID
   * @return 是否有权限
   */
  public boolean canAccessDept(final Long targetDeptId) {
    if (targetDeptId == null) {
      return true;
    }

    String dataScope = getDataScope();
    if ("ALL".equals(dataScope)) {
      return true;
    }

    Long deptId = getDeptId();
    if (deptId == null) {
      return false;
    }

    if ("DEPT_AND_CHILD".equals(dataScope)) {
      List<Long> deptIds = getDeptAndChildrenIds(deptId);
      return deptIds.contains(targetDeptId);
    } else if ("DEPT".equals(dataScope)) {
      return deptId.equals(targetDeptId);
    }

    // SELF 权限不按部门判断
    return true;
  }

  /**
   * 判断当前用户是否有权限访问指定用户创建的数据
   *
   * @param createdBy 创建人ID
   * @return 是否有权限
   */
  public boolean canAccessUserData(final Long createdBy) {
    if (createdBy == null) {
      return true;
    }

    String dataScope = getDataScope();
    if (!"SELF".equals(dataScope)) {
      return true; // 非SELF权限不按用户判断
    }

    Long userId = getUserId();
    return userId != null && userId.equals(createdBy);
  }

  /**
   * 获取部门及下级部门ID列表
   *
   * @param deptId 部门ID
   * @return 部门ID列表（包含自身及所有下级部门）
   */
  public List<Long> getDeptAndChildrenIds(final Long deptId) {
    if (deptId == null) {
      return Collections.emptyList();
    }

    List<Long> result = new ArrayList<>();
    result.add(deptId);
    collectChildDeptIds(deptId, result);
    return result;
  }

  /**
   * 递归收集下级部门ID
   *
   * @param parentId 父部门ID
   * @param result 结果列表
   */
  private void collectChildDeptIds(final Long parentId, final List<Long> result) {
    try {
      List<Long> childIds =
          departmentRepository
              .lambdaQuery()
              .select(Department::getId)
              .eq(Department::getParentId, parentId)
              .eq(Department::getDeleted, false)
              .list()
              .stream()
              .map(Department::getId)
              .collect(Collectors.toList());

      for (Long childId : childIds) {
        if (!result.contains(childId)) {
          result.add(childId);
          collectChildDeptIds(childId, result);
        }
      }
    } catch (Exception e) {
      log.warn("递归查询下级部门失败: {}", e.getMessage());
    }
  }

  /**
   * 获取当前用户数据权限范围.
   *
   * @return 数据权限范围
   */
  private String getDataScope() {
    try {
      return SecurityUtils.getDataScope();
    } catch (Exception e) {
      return "SELF"; // 默认最小权限
    }
  }

  /**
   * 获取当前用户ID
   *
   * @return 用户ID
   */
  private Long getUserId() {
    try {
      return SecurityUtils.getUserId();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 获取当前用户部门ID.
   *
   * @return 部门ID
   */
  private Long getDeptId() {
    try {
      return SecurityUtils.getDepartmentId();
    } catch (Exception e) {
      return null;
    }
  }
}
