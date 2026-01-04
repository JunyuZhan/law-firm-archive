package com.lawfirm.domain.finance.entity;

/**
 * 利冲审查状态枚举
 */
public enum ConflictCheckStatus {
    /**
     * 待审查
     */
    PENDING("待审查"),
    
    /**
     * 已通过
     */
    PASSED("已通过"),
    
    /**
     * 未通过
     */
    FAILED("未通过"),
    
    /**
     * 无需审查
     */
    NOT_REQUIRED("无需审查");

    private final String description;

    ConflictCheckStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
