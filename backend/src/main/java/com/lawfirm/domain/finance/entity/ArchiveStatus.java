package com.lawfirm.domain.finance.entity;

/**
 * 归档状态枚举
 */
public enum ArchiveStatus {
    /**
     * 未归档
     */
    NOT_ARCHIVED("未归档"),
    
    /**
     * 已归档
     */
    ARCHIVED("已归档"),
    
    /**
     * 已销毁
     */
    DESTROYED("已销毁");

    private final String description;

    ArchiveStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
