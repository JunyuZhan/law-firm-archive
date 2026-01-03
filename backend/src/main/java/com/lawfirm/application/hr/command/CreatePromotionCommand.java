package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建晋升申请命令
 */
@Data
public class CreatePromotionCommand {
    
    @NotNull(message = "目标职级不能为空")
    private Long targetLevelId;
    
    private String applyReason;
    
    private String achievements;
    
    private String selfEvaluation;
    
    private List<String> attachments;
}
