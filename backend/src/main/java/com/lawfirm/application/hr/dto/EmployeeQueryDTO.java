package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工档案查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeQueryDTO extends PageQuery {

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 工作状态
     */
    private String workStatus;

    /**
     * 职位
     */
    private String position;
}

