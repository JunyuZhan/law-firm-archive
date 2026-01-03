package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 员工档案实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class Employee extends BaseEntity {

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 性别：MALE-男, FEMALE-女
     */
    private String gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 国籍
     */
    private String nationality;

    /**
     * 籍贯
     */
    private String nativePlace;

    /**
     * 政治面貌
     */
    private String politicalStatus;

    /**
     * 学历
     */
    private String education;

    /**
     * 专业
     */
    private String major;

    /**
     * 毕业院校
     */
    private String graduationSchool;

    /**
     * 毕业日期
     */
    private LocalDate graduationDate;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 家庭住址
     */
    private String address;

    /**
     * 律师执业证号
     */
    private String lawyerLicenseNo;

    /**
     * 执业证发证日期
     */
    private LocalDate licenseIssueDate;

    /**
     * 执业证到期日期
     */
    private LocalDate licenseExpireDate;

    /**
     * 执业证状态：VALID-有效, EXPIRED-已过期, SUSPENDED-已暂停
     */
    private String licenseStatus;

    /**
     * 执业领域
     */
    private String practiceArea;

    /**
     * 执业年限
     */
    private Integer practiceYears;

    /**
     * 职位
     */
    private String position;

    /**
     * 职级
     */
    private String level;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 试用期结束日期
     */
    private LocalDate probationEndDate;

    /**
     * 转正日期
     */
    private LocalDate regularDate;

    /**
     * 离职日期
     */
    private LocalDate resignationDate;

    /**
     * 离职原因
     */
    private String resignationReason;

    /**
     * 工作状态：ACTIVE-在职, PROBATION-试用, RESIGNED-离职, RETIRED-退休
     */
    private String workStatus;

    /**
     * 备注
     */
    private String remark;
}

