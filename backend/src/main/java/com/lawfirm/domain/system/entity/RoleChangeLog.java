package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 角色变更历史实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_change_log")
public class RoleChangeLog extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 旧角色ID列表（JSON格式）
     */
    private String oldRoleIds;

    /**
     * 旧角色代码列表（JSON格式）
     */
    private String oldRoleCodes;

    /**
     * 新角色ID列表（JSON格式）
     */
    private String newRoleIds;

    /**
     * 新角色代码列表（JSON格式）
     */
    private String newRoleCodes;

    /**
     * 变更类型：UPGRADE-权限扩大, DOWNGRADE-权限缩小, TRANSFER-跨部门/跨角色
     */
    private String changeType;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 待处理业务数量
     */
    private Integer pendingBusinessCount;

    /**
     * 变更人ID
     */
    private Long changedBy;

    /**
     * 变更时间
     */
    private LocalDateTime changedAt;
}

