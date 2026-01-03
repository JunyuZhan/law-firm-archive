package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import com.lawfirm.common.constant.CompensationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 职位
     */
    private String position;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 律师执业证号
     */
    private String lawyerLicenseNo;

    /**
     * 入职日期
     */
    private LocalDate joinDate;

    /**
     * 薪酬模式：COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制
     * 重要：此字段决定是否参与项目提成分配
     */
    private String compensationType;

    /**
     * 是否可作为案源人
     */
    private Boolean canBeOriginator;

    /**
     * 状态：ACTIVE, INACTIVE, LOCKED
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    // ========== 业务方法 ==========

    /**
     * 是否有提成资格（提成制或混合制）
     */
    public boolean isCommissionEligible() {
        CompensationType type = CompensationType.fromCode(compensationType);
        return type.isCommissionEligible();
    }

    /**
     * 是否授薪制
     */
    public boolean isSalaried() {
        return CompensationType.SALARIED.getCode().equals(compensationType);
    }

    /**
     * 是否活跃状态
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}

