package com.lawfirm.infrastructure.persistence.interceptor;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.lawfirm.common.annotation.DataScope;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 数据权限SQL拦截器
 *
 * <p>自动在SQL查询中添加数据权限过滤条件： - ALL: 不添加条件（全部数据） - DEPT_AND_CHILD: AND dept_id IN (当前部门及下级部门ID列表) -
 * DEPT: AND dept_id = 当前部门ID - SELF: AND created_by = 当前用户ID
 *
 * <p>使用方式： 1. 在Mapper方法上添加 @DataScope 注解 2. 配置 deptAlias 和 userAlias 指定表别名
 */
@Slf4j
@Component
public class DataScopeInterceptor implements InnerInterceptor {

  /** 部门仓储（使用@Lazy延迟加载，避免循环依赖） */
  private DepartmentRepository departmentRepository;

  /**
   * 设置部门仓储
   *
   * @param departmentRepository 部门仓储
   */
  @Autowired
  @Lazy
  public void setDepartmentRepository(final DepartmentRepository departmentRepository) {
    this.departmentRepository = departmentRepository;
  }

  /** 缓存方法上的 @DataScope 注解. */
  private final Map<String, DataScope> dataScopeCache = new ConcurrentHashMap<>();

  /** 记录已检查但没有 @DataScope 注解的方法（避免重复反射查找）. */
  private final Set<String> noDataScopeMethods = ConcurrentHashMap.newKeySet();

  /** 缓存部门及下级部门ID（避免频繁查询） key: 部门ID, value: 部门及下级部门ID列表. */
  private final Map<Long, List<Long>> deptChildrenCache = new ConcurrentHashMap<>();

  /**
   * 查询前拦截，添加数据权限过滤条件
   *
   * @param executor 执行器
   * @param ms MappedStatement
   * @param parameter 参数
   * @param rowBounds 分页参数
   * @param resultHandler 结果处理器
   * @param boundSql BoundSql
   * @throws SQLException SQL异常
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void beforeQuery(
      final Executor executor,
      final MappedStatement ms,
      final Object parameter,
      final RowBounds rowBounds,
      final ResultHandler resultHandler,
      final BoundSql boundSql)
      throws SQLException {
    // 检查是否需要忽略
    if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
      return;
    }

    // 获取 @DataScope 注解
    DataScope dataScope = getDataScopeAnnotation(ms);
    if (dataScope == null || !dataScope.enabled()) {
      return;
    }

    // 获取当前用户数据权限范围
    String scopeType;
    Long userId;
    Long deptId;

    try {
      scopeType = SecurityUtils.getDataScope();
      userId = SecurityUtils.getUserId();
      deptId = SecurityUtils.getDepartmentId();
    } catch (Exception e) {
      // 未登录或获取用户信息失败，跳过数据权限过滤
      log.debug("获取用户信息失败，跳过数据权限过滤: {}", e.getMessage());
      return;
    }

    // ALL 权限不需要过滤
    if ("ALL".equals(scopeType)) {
      log.debug("用户拥有ALL权限，跳过数据权限过滤");
      return;
    }

    // 构建数据权限SQL条件
    String dataScopeSql = buildDataScopeSql(dataScope, scopeType, userId, deptId);
    if (!StringUtils.hasText(dataScopeSql)) {
      return;
    }

    // 修改原SQL
    String originalSql = boundSql.getSql();
    String newSql = appendDataScopeCondition(originalSql, dataScopeSql);

    if (newSql != null && !newSql.equals(originalSql)) {
      // 使用反射修改 BoundSql 中的 SQL
      PluginUtils.mpBoundSql(boundSql).sql(newSql);
      log.debug("数据权限SQL拦截 - 原SQL: {}", originalSql);
      log.debug("数据权限SQL拦截 - 新SQL: {}", newSql);
    }
  }

  /**
   * 获取方法上的 @DataScope 注解
   *
   * @param ms MappedStatement
   * @return DataScope注解，如果不存在则返回null
   */
  private DataScope getDataScopeAnnotation(final MappedStatement ms) {
    String id = ms.getId();

    // 使用 get() 而非 containsKey()+get() 避免竞态
    DataScope cached = dataScopeCache.get(id);
    if (cached != null) {
      return cached;
    }

    // 检查是否已确认没有注解
    if (noDataScopeMethods.contains(id)) {
      return null;
    }

    DataScope dataScope = null;
    try {
      // 解析 Mapper 类和方法
      int lastDotIndex = id.lastIndexOf(".");
      if (lastDotIndex < 0) {
        log.debug("无法解析 Mapper ID: {}（不包含点号）", id);
        return invocation.proceed();
      }
      String className = id.substring(0, lastDotIndex);
      String methodName = id.substring(lastDotIndex + 1);

      Class<?> mapperClass = Class.forName(className);

      // 先检查类上的注解
      dataScope = mapperClass.getAnnotation(DataScope.class);

      // 再检查方法上的注解（方法注解优先级更高）
      for (Method method : mapperClass.getMethods()) {
        if (method.getName().equals(methodName)) {
          DataScope methodAnnotation = method.getAnnotation(DataScope.class);
          if (methodAnnotation != null) {
            dataScope = methodAnnotation;
            break;
          }
        }
      }
    } catch (Exception e) {
      log.debug("获取DataScope注解失败: {}", e.getMessage());
    }

    // 缓存结果：使用 putIfAbsent 避免覆盖并发写入的值
    if (dataScope != null) {
      DataScope existing = dataScopeCache.putIfAbsent(id, dataScope);
      return existing != null ? existing : dataScope;
    } else {
      noDataScopeMethods.add(id);
    }
    return dataScope;
  }

  /**
   * 验证标识符是否安全（仅允许字母、数字、下划线）
   *
   * @param identifier SQL 标识符（表别名或字段名）
   * @return 验证通过返回 true，否则返回 false
   */
  private boolean isValidIdentifier(final String identifier) {
    if (!StringUtils.hasText(identifier)) {
      return false;
    }
    // 只允许字母、数字和下划线，且必须以字母或下划线开头
    return identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
  }

  /**
   * 构建安全的列名
   *
   * @param alias 表别名
   * @param field 字段名
   * @return 安全的列名，验证失败返回 null
   */
  private String buildSafeColumn(final String alias, final String field) {
    if (!isValidIdentifier(field)) {
      log.warn("字段名校验失败: {}", field);
      return null;
    }
    if (StringUtils.hasText(alias)) {
      if (!isValidIdentifier(alias)) {
        log.warn("表别名校验失败: {}", alias);
        return null;
      }
      return alias + "." + field;
    }
    return field;
  }

  /**
   * 构建数据权限SQL条件
   *
   * @param dataScope DataScope注解
   * @param scopeType 权限范围类型
   * @param userId 用户ID
   * @param deptId 部门ID
   * @return SQL条件字符串
   */
  private String buildDataScopeSql(
      final DataScope dataScope, final String scopeType, final Long userId, final Long deptId) {
    StringBuilder sql = new StringBuilder();

    switch (scopeType) {
      case "DEPT_AND_CHILD":
        // 部门及下级部门
        if (deptId != null && StringUtils.hasText(dataScope.deptAlias())) {
          List<Long> deptIds = getDeptAndChildrenIds(deptId);
          if (!deptIds.isEmpty()) {
            String column = buildSafeColumn(dataScope.deptAlias(), dataScope.deptField());
            if (column != null) {
              sql.append(column)
                  .append(" IN (")
                  .append(deptIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                  .append(")");
            }
          }
        }
        break;

      case "DEPT":
        // 本部门
        if (deptId != null && StringUtils.hasText(dataScope.deptAlias())) {
          String column = buildSafeColumn(dataScope.deptAlias(), dataScope.deptField());
          if (column != null) {
            sql.append(column).append(" = ").append(deptId);
          }
        }
        break;

      case "SELF":
      default:
        // 仅本人
        if (userId != null && StringUtils.hasText(dataScope.userAlias())) {
          String column = buildSafeColumn(dataScope.userAlias(), dataScope.userField());
          if (column != null) {
            sql.append(column).append(" = ").append(userId);
          }
        }
        break;
    }

    return sql.toString();
  }

  /**
   * 获取部门及下级部门ID列表
   *
   * @param deptId 部门ID
   * @return 部门及下级部门ID列表
   */
  private List<Long> getDeptAndChildrenIds(final Long deptId) {
    // 从缓存获取
    List<Long> cached = deptChildrenCache.get(deptId);
    if (cached != null) {
      // 返回不可变副本，防止调用方修改缓存内容
      return java.util.Collections.unmodifiableList(cached);
    }

    // 查询部门及下级部门
    List<Long> deptIds = new ArrayList<>();
    deptIds.add(deptId);

    try {
      // 递归查询下级部门
      collectChildDeptIds(deptId, deptIds);
    } catch (Exception e) {
      log.warn("查询下级部门失败: {}", e.getMessage());
    }

    // 缓存结果（5分钟后过期，这里简化处理不设过期）
    deptChildrenCache.put(deptId, deptIds);
    // 返回不可变副本
    return java.util.Collections.unmodifiableList(deptIds);
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
              .select(com.lawfirm.domain.system.entity.Department::getId)
              .eq(com.lawfirm.domain.system.entity.Department::getParentId, parentId)
              .eq(com.lawfirm.domain.system.entity.Department::getDeleted, false)
              .list()
              .stream()
              .map(com.lawfirm.domain.system.entity.Department::getId)
              .collect(Collectors.toList());

      for (Long childId : childIds) {
        if (!result.contains(childId)) {
          result.add(childId);
          collectChildDeptIds(childId, result);
        }
      }
    } catch (Exception e) {
      log.debug("递归查询下级部门失败: {}", e.getMessage());
    }
  }

  /**
   * 在原SQL中追加数据权限条件
   *
   * @param originalSql 原始SQL
   * @param dataScopeSql 数据权限SQL条件
   * @return 修改后的SQL
   */
  private String appendDataScopeCondition(final String originalSql, final String dataScopeSql) {
    if (!StringUtils.hasText(dataScopeSql)) {
      return originalSql;
    }

    try {
      // 使用 JSqlParser 解析SQL
      Select select = (Select) CCJSqlParserUtil.parse(originalSql);
      PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

      // 构建新的WHERE条件
      Expression where = plainSelect.getWhere();
      Expression dataScopeExpression = CCJSqlParserUtil.parseCondExpression(dataScopeSql);

      if (where == null) {
        plainSelect.setWhere(dataScopeExpression);
      } else {
        // 将原条件和数据权限条件用 AND 连接
        AndExpression andExpression =
            new AndExpression(where, new Parenthesis(dataScopeExpression));
        plainSelect.setWhere(andExpression);
      }

      return select.toString();
    } catch (Exception e) {
      log.warn("解析SQL失败，使用字符串拼接方式: {}", e.getMessage());
      // 降级方案：字符串拼接
      return appendConditionByString(originalSql, dataScopeSql);
    }
  }

  /**
   * 字符串拼接方式追加条件（降级方案）
   *
   * @param originalSql 原始SQL
   * @param dataScopeSql 数据权限SQL条件
   * @return 修改后的SQL
   */
  private String appendConditionByString(final String originalSql, final String dataScopeSql) {
    String upperSql = originalSql.toUpperCase();

    // 查找 WHERE 关键字位置
    int whereIndex = upperSql.lastIndexOf(" WHERE ");
    int orderByIndex = upperSql.lastIndexOf(" ORDER BY ");
    int groupByIndex = upperSql.lastIndexOf(" GROUP BY ");
    int limitIndex = upperSql.lastIndexOf(" LIMIT ");

    // 找到插入位置
    int insertIndex = originalSql.length();
    if (orderByIndex > 0) {
      insertIndex = Math.min(insertIndex, orderByIndex);
    }
    if (groupByIndex > 0) {
      insertIndex = Math.min(insertIndex, groupByIndex);
    }
    if (limitIndex > 0) {
      insertIndex = Math.min(insertIndex, limitIndex);
    }

    StringBuilder newSql = new StringBuilder();
    if (whereIndex > 0) {
      // 已有 WHERE，追加 AND 条件
      newSql.append(originalSql, 0, insertIndex);
      newSql.append(" AND (").append(dataScopeSql).append(")");
      newSql.append(originalSql.substring(insertIndex));
    } else {
      // 没有 WHERE，添加 WHERE 条件
      newSql.append(originalSql, 0, insertIndex);
      newSql.append(" WHERE ").append(dataScopeSql);
      newSql.append(originalSql.substring(insertIndex));
    }

    return newSql.toString();
  }

  /** 清除部门缓存（部门变更时调用）. */
  public void clearDeptCache() {
    deptChildrenCache.clear();
    log.info("数据权限部门缓存已清除");
  }

  /**
   * 清除指定部门的缓存
   *
   * @param deptId 部门ID
   */
  public void clearDeptCache(final Long deptId) {
    deptChildrenCache.remove(deptId);
    log.debug("数据权限部门缓存已清除: deptId={}", deptId);
  }
}
