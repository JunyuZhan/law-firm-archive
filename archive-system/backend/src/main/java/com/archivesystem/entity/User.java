package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 部门 */
    private String department;

    /** 角色：ADMIN-管理员, ARCHIVIST-档案员, USER-普通用户 */
    private String role;

    /** 状态：ACTIVE-正常, DISABLED-禁用 */
    private String status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    // ===== 角色常量 =====
    
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_ARCHIVIST = "ARCHIVIST";
    public static final String ROLE_USER = "USER";

    // ===== 状态常量 =====
    
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
}
