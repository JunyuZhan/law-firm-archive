package com.lawfirm.domain.finance.entity;

/** 合同参与人角色枚举. */
public enum ContractParticipantRole {
  /** 承办律师. */
  LEAD("承办律师"),

  /** 协办律师. */
  CO_COUNSEL("协办律师"),

  /** 案源人. */
  ORIGINATOR("案源人"),

  /** 律师助理. */
  PARALEGAL("律师助理");

  /** 角色描述. */
  private final String description;

  ContractParticipantRole(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
