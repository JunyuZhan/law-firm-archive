package com.lawfirm.domain.finance.entity;

/**
 * 变更申请状态枚举
 *
 * <p>Requirements: 3.5
 */
public enum AmendmentStatus {
  /** 待审批 */
  PENDING,

  /** 已批准 */
  APPROVED,

  /** 已拒绝 */
  REJECTED
}
