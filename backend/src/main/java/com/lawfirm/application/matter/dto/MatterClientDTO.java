package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 项目-客户关联 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterClientDTO extends BaseDTO {

  /** 项目ID. */
  private Long matterId;

  /** 客户ID. */
  private Long clientId;

  /** 客户名称. */
  private String clientName;

  /** 客户类型. */
  private String clientType;

  /**
   * 客户角色：PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人,
   * APPELLANT-上诉人, APPELLEE-被上诉人, EXECUTION_APPLICANT-申请执行人, EXECUTION_RESPONDENT-被执行人,
   * SUSPECT-犯罪嫌疑人, DEFENDANT_CRIMINAL-被告人, RETRIAL_APPLICANT-再审申请人, RETRIAL_RESPONDENT-再审被申请人.
   */
  private String clientRole;

  /** 客户角色名称. */
  private String clientRoleName;

  /** 是否主要客户. */
  private Boolean isPrimary;

  /**
   * 获取客户角色名称.
   *
   * @param clientRole 客户角色
   * @return 客户角色名称
   */
  public static String getClientRoleName(final String clientRole) {
    if (clientRole == null) {
      return "";
    }
    return switch (clientRole) {
        // 一审/普通诉讼
      case "PLAINTIFF" -> "原告";
      case "DEFENDANT" -> "被告";
      case "THIRD_PARTY" -> "第三人";
        // 二审
      case "APPELLANT" -> "上诉人";
      case "APPELLEE" -> "被上诉人";
        // 仲裁
      case "APPLICANT" -> "申请人";
      case "RESPONDENT" -> "被申请人";
        // 执行
      case "EXECUTION_APPLICANT" -> "申请执行人";
      case "EXECUTION_RESPONDENT" -> "被执行人";
        // 刑事
      case "SUSPECT" -> "犯罪嫌疑人";
      case "DEFENDANT_CRIMINAL" -> "被告人";
        // 再审
      case "RETRIAL_APPLICANT" -> "再审申请人";
      case "RETRIAL_RESPONDENT" -> "再审被申请人";
      default -> clientRole;
    };
  }
}
