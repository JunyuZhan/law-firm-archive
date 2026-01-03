package com.lawfirm.common.constant;

/**
 * 项目团队角色枚举
 * 
 * 重要：项目角色与薪酬模式是两个独立维度
 * - 项目角色决定在案件中的职责
 * - 无论提成制还是授薪制，都可以担任任何项目角色
 */
public enum TeamRole {
    
    /**
     * 主办律师
     * - 案件的第一负责人
     * - 对案件质量负全责
     * - 有权决定协办律师的分配比例
     * - 每个项目有且仅有一个
     */
    LEAD_LAWYER("LEAD_LAWYER", "主办律师"),
    
    /**
     * 协办律师
     * - 协助主办律师处理案件
     * - 执行具体任务
     * - 一个项目可有多个
     */
    ASSOCIATE("ASSOCIATE", "协办律师"),
    
    /**
     * 案源人
     * - 带来案件的人
     * - 可以是团队成员，也可以不是
     * - 主办律师可同时是案源人
     * - 协办律师也可同时是案源人
     */
    ORIGINATOR("ORIGINATOR", "案源人"),
    
    /**
     * 律师助理
     * - 辅助工作，非律师身份
     * - 通常不参与提成
     */
    PARALEGAL("PARALEGAL", "律师助理");

    private final String code;
    private final String name;

    TeamRole(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static TeamRole fromCode(String code) {
        for (TeamRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return ASSOCIATE; // 默认为协办
    }
}

