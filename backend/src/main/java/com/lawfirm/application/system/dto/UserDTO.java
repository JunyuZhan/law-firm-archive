package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends BaseDTO {

    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Long departmentId;
    private String departmentName;
    private String position;
    private String employeeNo;
    private String lawyerLicenseNo;
    private LocalDate joinDate;
    private String compensationType;
    private String compensationTypeName;
    private Boolean canBeOriginator;
    private String status;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 角色编码列表
     */
    private List<String> roleCodes;

    /**
     * 权限编码列表
     */
    private List<String> permissions;
}

