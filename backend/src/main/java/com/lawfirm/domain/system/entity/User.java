package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import com.lawfirm.common.constant.CompensationType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 用户实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

  /** 用户名 */
  private String username;

  /** 密码（加密） */
  private String password;

  /** 真实姓名 */
  private String realName;

  /** 邮箱 */
  private String email;

  /** 手机号 */
  private String phone;

  /** 头像URL */
  private String avatarUrl;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称（非数据库字段，用于关联查询） */
  @TableField(exist = false)
  private String departmentName;

  /** 职位 */
  private String position;

  /** 工号 */
  private String employeeNo;

  /** 律师执业证号 */
  private String lawyerLicenseNo;

  /** 入职日期 */
  private LocalDate joinDate;

  /** 薪酬模式：COMMISSION-提成制, SALARIED-授薪制, HYBRID-混合制 重要：此字段决定是否参与项目提成分配 */
  private String compensationType;

  /** 是否可作为案源人 */
  private Boolean canBeOriginator;

  /** 状态：ACTIVE, INACTIVE, LOCKED */
  private String status;

  /** 最后登录时间 */
  private LocalDateTime lastLoginAt;

  /** 最后登录IP */
  private String lastLoginIp;

  /**
   * 获取真实姓名.
   *
   * @return 真实姓名
   */
  public String getRealName() {
    return realName;
  }

  /**
   * 获取律师执业证号.
   *
   * @return 律师执业证号
   */
  public String getLawyerLicenseNo() {
    return lawyerLicenseNo;
  }

  /**
   * 获取手机号.
   *
   * @return 手机号
   */
  public String getPhone() {
    return phone;
  }

  /**
   * 获取邮箱.
   *
   * @return 邮箱
   */
  public String getEmail() {
    return email;
  }

  // ========== 业务方法 ==========

  /**
   * 是否有提成资格（提成制或混合制）。
   *
   * @return 是否有提成资格
   */
  public boolean isCommissionEligible() {
    CompensationType type = CompensationType.fromCode(compensationType);
    return type.isCommissionEligible();
  }

  /**
   * 是否授薪制。
   *
   * @return 是否授薪制
   */
  public boolean isSalaried() {
    return CompensationType.SALARIED.getCode().equals(compensationType);
  }

  /**
   * 是否活跃状态。
   *
   * @return 是否活跃状态
   */
  public boolean isActive() {
    return "ACTIVE".equals(status);
  }
}
