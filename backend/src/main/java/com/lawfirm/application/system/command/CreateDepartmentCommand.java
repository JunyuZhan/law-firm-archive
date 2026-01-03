package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建部门命令
 */
@Data
public class CreateDepartmentCommand {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    /**
     * 父部门ID，为空表示顶级部门
     */
    private Long parentId;

    private Integer sortOrder = 0;

    /**
     * 部门负责人ID
     */
    private Long leaderId;
}
