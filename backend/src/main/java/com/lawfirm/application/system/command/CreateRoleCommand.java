package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建角色命令
 */
@Data
public class CreateRoleCommand {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;

    private String dataScope = "SELF";

    private Integer sortOrder = 0;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;
}
