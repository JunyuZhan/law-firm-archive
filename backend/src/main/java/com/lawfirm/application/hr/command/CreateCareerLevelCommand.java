package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 创建职级命令 */
@Data
public class CreateCareerLevelCommand {

  /** 职级编码 */
  @NotBlank(message = "职级编码不能为空")
  private String levelCode;

  /** 职级名称 */
  @NotBlank(message = "职级名称不能为空")
  private String levelName;

  /** 职级顺序 */
  @NotNull(message = "职级顺序不能为空")
  private Integer levelOrder;

  /** 通道类别 */
  @NotBlank(message = "通道类别不能为空")
  private String category;

  /** 描述 */
  private String description;

  /** 最低工作年限 */
  private Integer minWorkYears;

  /** 最低案件数量 */
  private Integer minMatterCount;

  /** 最低收入 */
  private BigDecimal minRevenue;

  /** 必需证书列表 */
  private List<String> requiredCertificates;

  /** 其他要求 */
  private String otherRequirements;

  /** 最低薪资 */
  private BigDecimal salaryMin;

  /** 最高薪资 */
  private BigDecimal salaryMax;
}
