package com.lawfirm.common.constant;

/**
 * 律师薪酬模式枚举
 * 
 * 重要：项目角色（主办/协办）与薪酬模式是两个独立维度
 * - 项目角色决定谁负责案件
 * - 薪酬模式决定是否参与提成分配
 */
public enum CompensationType {
    
    /**
     * 提成制律师
     * - 收入主要来自案件提成
     * - 无固定工资或低底薪
     * - 参与项目提成分配
     */
    COMMISSION("COMMISSION", "提成制", "按案件收入提成"),
    
    /**
     * 授薪制律师
     * - 收入为固定月薪
     * - 不参与项目提成分配
     * - 可以担任主办/协办律师
     */
    SALARIED("SALARIED", "授薪制", "固定月薪"),
    
    /**
     * 混合制律师
     * - 有基本工资 + 案件提成
     * - 参与项目提成分配
     * - 常见于新律师过渡期
     */
    HYBRID("HYBRID", "混合制", "底薪+提成");

    private final String code;
    private final String name;
    private final String description;

    CompensationType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否有提成资格
     */
    public boolean isCommissionEligible() {
        return this == COMMISSION || this == HYBRID;
    }

    /**
     * 是否领取固定薪资
     */
    public boolean hasSalary() {
        return this == SALARIED || this == HYBRID;
    }

    public static CompensationType fromCode(String code) {
        for (CompensationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return COMMISSION; // 默认为提成制
    }
}

