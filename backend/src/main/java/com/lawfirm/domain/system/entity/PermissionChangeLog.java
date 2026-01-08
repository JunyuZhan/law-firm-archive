package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 权限变更历史实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission_change_log")
public class PermissionChangeLog extends BaseEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色代码
     */
    private String roleCode;

    /**
     * 变更类型：ADD-新增权限, REMOVE-移除权限
     */
    private String changeType;

    /**
     * 权限代码
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 变更人ID
     */
    private Long changedBy;

    /**
     * 变更时间
     */
    private LocalDateTime changedAt;
}

