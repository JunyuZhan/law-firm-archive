package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 案源跟进记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead_follow_up")
public class LeadFollowUp extends BaseEntity {

    /**
     * 案源ID
     */
    private Long leadId;

    /**
     * 跟进方式：PHONE-电话, EMAIL-邮件, VISIT-拜访, MEETING-会议, OTHER-其他
     */
    private String followType;

    /**
     * 跟进内容
     */
    private String followContent;

    /**
     * 跟进结果：POSITIVE-积极, NEUTRAL-中性, NEGATIVE-消极
     */
    private String followResult;

    /**
     * 下次跟进时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 下次跟进计划
     */
    private String nextFollowPlan;

    /**
     * 跟进人ID
     */
    private Long followUserId;
}

