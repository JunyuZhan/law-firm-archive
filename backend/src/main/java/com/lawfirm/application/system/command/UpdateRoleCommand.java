package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新角色命令
 */
@Data
public class UpdateRoleCommand {

    @NotNull(message = "角色ID不能为空")
    private Long id;

    private String roleName;

    private String description;

    private String dataScope;

    private Integer sortOrder;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;
}
