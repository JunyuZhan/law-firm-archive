package com.lawfirm.application.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 供应商DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {

  /** 供应商ID */
  private Long id;

  /** 供应商编号 */
  private String supplierNo;

  /** 供应商名称 */
  private String name;

  /** 供应商类型 */
  private String supplierType;

  /** 供应商类型名称 */
  private String supplierTypeName;

  /** 联系人 */
  private String contactPerson;

  /** 联系电话 */
  private String contactPhone;

  /** 联系邮箱 */
  private String contactEmail;

  /** 地址 */
  private String address;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 开户银行 */
  private String bankName;

  /** 银行账号 */
  private String bankAccount;

  /** 供应范围 */
  private String supplyScope;

  /** 评级 */
  private String rating;

  /** 评级名称 */
  private String ratingName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
