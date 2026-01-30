package com.lawfirm.application.contract.dto;

import lombok.Data;

/** 合同模板DTO */
@Data
public class ContractTemplateDTO {
  /** 模板ID */
  private Long id;

  /** 模板编号 */
  private String templateNo;

  /** 模板名称 */
  private String name;

  /** 模板类型 */
  private String templateType;

  /** 收费方式 */
  private String feeType;

  /** 模板内容 */
  private String content;

  /** 条款 */
  private String clauses;

  /** 描述 */
  private String description;

  /** 状态 */
  private String status;

  /** 排序 */
  private Integer sortOrder;

  /** 创建时间 */
  private String createdAt;

  /** 更新时间 */
  private String updatedAt;

  /**
   * 获取模板类型名称（合同类型名称）
   *
   * @return 模板类型名称
   */
  public String getTemplateTypeName() {
    if (templateType == null) {
      return null;
    }
    switch (templateType) {
      case "CIVIL_PROXY":
        return "民事代理";
      case "ADMINISTRATIVE_PROXY":
        return "行政代理";
      case "CRIMINAL_DEFENSE":
        return "刑事辩护";
      case "LEGAL_COUNSEL":
        return "法律顾问";
      case "NON_LITIGATION":
        return "非诉案件";
      case "CUSTOM":
        return "自定义模板";
      default:
        return templateType;
    }
  }

  /**
   * 获取收费方式名称
   *
   * @return 收费方式名称
   */
  public String getFeeTypeName() {
    if (feeType == null) {
      return null;
    }
    switch (feeType) {
      case "FIXED":
        return "固定收费";
      case "HOURLY":
        return "计时收费";
      case "CONTINGENCY":
        return "风险代理";
      case "MIXED":
        return "混合收费";
      default:
        return feeType;
    }
  }
}
