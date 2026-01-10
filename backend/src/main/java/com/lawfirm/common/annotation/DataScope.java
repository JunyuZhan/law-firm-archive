package com.lawfirm.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于标记需要进行数据权限过滤的Mapper方法
 * 
 * 使用示例：
 * <pre>
 * // 在Mapper接口方法上使用
 * @DataScope(deptAlias = "m", userAlias = "m")
 * List<Matter> selectMatterList(@Param("query") MatterQuery query);
 * 
 * // 或者在Service方法上使用（需要配合DataScopeHelper）
 * @DataScope(deptAlias = "d", userAlias = "u", deptField = "department_id", userField = "created_by")
 * public PageResult<ClientDTO> listClients(ClientQuery query) { ... }
 * </pre>
 * 
 * 数据权限范围说明：
 * - ALL: 全部数据权限，不添加过滤条件
 * - DEPT_AND_CHILD: 本部门及下级部门数据权限
 * - DEPT: 本部门数据权限
 * - SELF: 仅本人数据权限
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    
    /**
     * 部门表的别名
     * 用于生成 SQL 条件：{deptAlias}.{deptField} IN (...)
     */
    String deptAlias() default "";
    
    /**
     * 用户表的别名
     * 用于生成 SQL 条件：{userAlias}.{userField} = ?
     */
    String userAlias() default "";
    
    /**
     * 部门字段名
     * 默认为 department_id
     */
    String deptField() default "department_id";
    
    /**
     * 用户字段名（创建人字段）
     * 默认为 created_by
     */
    String userField() default "created_by";
    
    /**
     * 是否启用数据权限
     * 设为 false 可临时禁用
     */
    boolean enabled() default true;
}
