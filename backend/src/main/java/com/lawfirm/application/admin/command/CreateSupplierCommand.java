package com.lawfirm.application.admin.command;

import lombok.Data;

/** 创建供应商命令 */
@Data
public class CreateSupplierCommand {

  /** 供应商名称 */
  private String name;

  /** 供应商类型 */
  private String supplierType;

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

  /** 备注 */
  private String remarks;
}
