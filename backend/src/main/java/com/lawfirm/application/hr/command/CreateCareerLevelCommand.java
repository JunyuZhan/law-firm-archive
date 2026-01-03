package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建职级命令
 */
@Data
public class CreateCareerLevelCommand {
    
    @NotBlank(message = "职级编码不能为空")
    private String levelCode;
    
    @NotBlank(message = "职级名称不能为空")
    private String levelName;
    
    @NotNull(message = "职级顺序不能为空")
    private Integer levelOrder;
    
    @NotBlank(message = "通道类别不能为空")
    private String category;
    
    private String description;
    
    private Integer minWorkYears;
    private Integer minMatterCount;
    private BigDecimal minRevenue;
    private List<String> requiredCertificates;
    private String otherRequirements;
    
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
}
