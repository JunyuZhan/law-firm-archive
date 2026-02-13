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
 * 对应数据库表: sys_user
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

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 部门 */
    private String department;

    /** 用户类型（三员分立） */
    @Builder.Default
    private String userType = TYPE_USER;

    /** 状态 */
    @Builder.Default
    private String status = STATUS_ACTIVE;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    // ===== 用户类型常量（三员分立） =====
    
    public static final String TYPE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String TYPE_SECURITY_ADMIN = "SECURITY_ADMIN";
    public static final String TYPE_AUDIT_ADMIN = "AUDIT_ADMIN";
    public static final String TYPE_ARCHIVIST = "ARCHIVIST";
    public static final String TYPE_USER = "USER";

    // ===== 状态常量 =====
    
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
}
