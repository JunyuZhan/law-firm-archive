package com.lawfirm.common.constant;

import java.util.Map;
import java.util.HashMap;

/**
 * 项目/案件相关常量
 * 统一管理项目大类、案件类型、状态等的中文名称映射
 */
public class MatterConstants {

    // ============ 项目状态常量 ============
    /** 草稿 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 待审批 */
    public static final String STATUS_PENDING = "PENDING";
    /** 进行中 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 已暂停 */
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    /** 待审批结案 */
    public static final String STATUS_PENDING_CLOSE = "PENDING_CLOSE";
    /** 已结案 */
    public static final String STATUS_CLOSED = "CLOSED";
    /** 已归档 */
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /**
     * 项目大类名称映射
     */
    public static final Map<String, String> MATTER_TYPE_NAME_MAP = new HashMap<>();
    
    /**
     * 案件类型名称映射
     */
    public static final Map<String, String> CASE_TYPE_NAME_MAP = new HashMap<>();
    
    /**
     * 项目状态名称映射
     */
    public static final Map<String, String> MATTER_STATUS_NAME_MAP = new HashMap<>();
    
    /**
     * 代理阶段名称映射
     */
    public static final Map<String, String> LITIGATION_STAGE_NAME_MAP = new HashMap<>();

    static {
        // 初始化项目大类名称
        MATTER_TYPE_NAME_MAP.put("LITIGATION", "诉讼案件");
        MATTER_TYPE_NAME_MAP.put("NON_LITIGATION", "非诉项目");
        
        // 初始化案件类型名称（统一使用完整名称）
        CASE_TYPE_NAME_MAP.put("CIVIL", "民事案件");
        CASE_TYPE_NAME_MAP.put("CRIMINAL", "刑事案件");
        CASE_TYPE_NAME_MAP.put("ADMINISTRATIVE", "行政案件");
        CASE_TYPE_NAME_MAP.put("BANKRUPTCY", "破产案件");
        CASE_TYPE_NAME_MAP.put("IP", "知识产权案件");
        CASE_TYPE_NAME_MAP.put("ARBITRATION", "仲裁案件");
        CASE_TYPE_NAME_MAP.put("COMMERCIAL_ARBITRATION", "商事仲裁");
        CASE_TYPE_NAME_MAP.put("LABOR_ARBITRATION", "劳动仲裁");
        CASE_TYPE_NAME_MAP.put("ENFORCEMENT", "执行案件");
        CASE_TYPE_NAME_MAP.put("LEGAL_COUNSEL", "法律顾问");
        CASE_TYPE_NAME_MAP.put("SPECIAL_SERVICE", "专项服务");
        CASE_TYPE_NAME_MAP.put("DUE_DILIGENCE", "尽职调查");
        CASE_TYPE_NAME_MAP.put("CONTRACT_REVIEW", "合同审查");
        CASE_TYPE_NAME_MAP.put("LEGAL_OPINION", "法律意见");
        
        // 初始化项目状态名称
        MATTER_STATUS_NAME_MAP.put("DRAFT", "草稿");
        MATTER_STATUS_NAME_MAP.put("PENDING", "待审批");
        MATTER_STATUS_NAME_MAP.put("ACTIVE", "进行中");
        MATTER_STATUS_NAME_MAP.put("SUSPENDED", "已暂停");
        MATTER_STATUS_NAME_MAP.put("PENDING_CLOSE", "待审批结案");
        MATTER_STATUS_NAME_MAP.put("CLOSED", "已结案");
        MATTER_STATUS_NAME_MAP.put("ARCHIVED", "已归档");
        
        // 初始化代理阶段名称（通用）
        LITIGATION_STAGE_NAME_MAP.put("FIRST_INSTANCE", "一审");
        LITIGATION_STAGE_NAME_MAP.put("SECOND_INSTANCE", "二审");
        LITIGATION_STAGE_NAME_MAP.put("RETRIAL", "再审");
        LITIGATION_STAGE_NAME_MAP.put("EXECUTION", "执行");
        LITIGATION_STAGE_NAME_MAP.put("ARBITRATION", "仲裁阶段");
        // 刑事案件特有阶段
        LITIGATION_STAGE_NAME_MAP.put("INVESTIGATION", "侦查阶段");
        LITIGATION_STAGE_NAME_MAP.put("PROSECUTION_REVIEW", "审查起诉");
        LITIGATION_STAGE_NAME_MAP.put("DEATH_PENALTY_REVIEW", "死刑复核");
        // 行政案件特有阶段
        LITIGATION_STAGE_NAME_MAP.put("ADMINISTRATIVE_RECONSIDERATION", "行政复议");
        // 执行案件特有阶段
        LITIGATION_STAGE_NAME_MAP.put("EXECUTION_OBJECTION", "执行异议");
        LITIGATION_STAGE_NAME_MAP.put("EXECUTION_REVIEW", "执行复议");
        // 非诉
        LITIGATION_STAGE_NAME_MAP.put("NON_LITIGATION", "非诉服务");
    }

    /**
     * 获取项目大类名称
     * @param matterType 项目大类代码
     * @return 项目大类名称，如果不存在则返回原值
     */
    public static String getMatterTypeName(String matterType) {
        if (matterType == null) {
            return null;
        }
        return MATTER_TYPE_NAME_MAP.getOrDefault(matterType, matterType);
    }

    /**
     * 获取案件类型名称
     * @param caseType 案件类型代码
     * @return 案件类型名称，如果不存在则返回原值
     */
    public static String getCaseTypeName(String caseType) {
        if (caseType == null) {
            return null;
        }
        return CASE_TYPE_NAME_MAP.getOrDefault(caseType, caseType);
    }

    /**
     * 获取项目状态名称
     * @param status 状态代码
     * @return 状态名称，如果不存在则返回原值
     */
    public static String getMatterStatusName(String status) {
        if (status == null) {
            return null;
        }
        return MATTER_STATUS_NAME_MAP.getOrDefault(status, status);
    }
    
    /**
     * 获取代理阶段名称
     * @param litigationStage 代理阶段代码
     * @return 代理阶段名称，如果不存在则返回原值
     */
    public static String getLitigationStageName(String litigationStage) {
        if (litigationStage == null) {
            return null;
        }
        return LITIGATION_STAGE_NAME_MAP.getOrDefault(litigationStage, litigationStage);
    }
}

