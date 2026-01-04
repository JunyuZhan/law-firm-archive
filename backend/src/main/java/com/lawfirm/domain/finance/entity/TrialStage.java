package com.lawfirm.domain.finance.entity;

/**
 * 审理阶段枚举
 */
public enum TrialStage {
    /**
     * 一审
     */
    FIRST_INSTANCE("一审"),
    
    /**
     * 二审
     */
    SECOND_INSTANCE("二审"),
    
    /**
     * 再审
     */
    RETRIAL("再审"),
    
    /**
     * 执行
     */
    EXECUTION("执行"),
    
    /**
     * 非诉
     */
    NON_LITIGATION("非诉");

    private final String description;

    TrialStage(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
