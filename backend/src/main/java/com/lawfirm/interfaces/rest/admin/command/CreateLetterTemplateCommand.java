package com.lawfirm.interfaces.rest.admin.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新出函模板命令
 */
@Data
public class CreateLetterTemplateCommand {
    
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    @NotBlank(message = "函件类型不能为空")
    private String letterType;
    
    @NotBlank(message = "模板内容不能为空")
    private String content;
    
    private String description;
}
