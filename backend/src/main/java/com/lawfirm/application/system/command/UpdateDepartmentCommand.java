package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新部门命令
 */
@Data
public class UpdateDepartmentCommand {

    @NotNull(message = "部门ID不能为空")
    private Long id;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private Long leaderId;

    private String status;
}
