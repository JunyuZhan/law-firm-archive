package com.lawfirm.domain.finance.entity;

/** 付款计划状态枚举. */
public enum PaymentScheduleStatus {
  /** 待收. */
  PENDING("待收"),

  /** 部分收款. */
  PARTIAL("部分收款"),

  /** 已收清. */
  PAID("已收清"),

  /** 已取消. */
  CANCELLED("已取消");

  /** 状态描述. */
  private final String description;

  PaymentScheduleStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
