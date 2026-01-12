package com.lawfirm.application.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 函件验证数据推送DTO
 * 用于将函件验证信息推送到客户服务系统
 * 
 * 推送后，客服系统独立存储和验证，律所系统不对外暴露任何验证接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterVerificationPushDTO {

    /**
     * 函件申请ID（内部ID，客服系统存储用于关联）
     */
    private Long letterId;

    /**
     * 函件申请编号（对外展示）
     */
    private String applicationNo;

    /**
     * 验证码（客服系统用于验证）
     */
    private String verificationCode;

    /**
     * 函件类型编码
     */
    private String letterType;

    /**
     * 函件类型名称
     */
    private String letterTypeName;

    /**
     * 接收单位
     */
    private String targetUnit;

    /**
     * 出函律师姓名（多人用逗号分隔）
     */
    private String lawyerNames;

    /**
     * 律师事务所名称
     */
    private String firmName;

    /**
     * 关联项目名称（脱敏）
     */
    private String matterName;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 打印时间
     */
    private LocalDateTime printedAt;

    /**
     * 验证有效期（默认1年）
     */
    private LocalDateTime validUntil;

    /**
     * 备注说明
     */
    private String remark;
}
