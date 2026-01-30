package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 职级通道 DTO */
@Data
public class CareerLevelDTO {
  /** 职级ID */
  private Long id;

  /** 职级编码 */
  private String levelCode;

  /** 职级名称 */
  private String levelName;

  /** 职级顺序 */
  private Integer levelOrder;

  /** 通道类别 */
  private String category;

  /** 通道类别名称 */
  private String categoryName;

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

  /** 薪资范围 */
  private String salaryRange;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
