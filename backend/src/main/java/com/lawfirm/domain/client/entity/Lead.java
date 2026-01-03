package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 案源线索实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class Lead extends BaseEntity {

    /**
     * 案源编号
     */
    private String leadNo;

    /**
     * 案源名称
     */
    private String leadName;

    /**
     * 案源类型：INDIVIDUAL-个人, ENTERPRISE-企业
     */
    private String leadType;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 来源详情
     */
    private String sourceDetail;

    /**
     * 状态：PENDING-待跟进, FOLLOWING-跟进中, CONVERTED-已转化, ABANDONED-已放弃
     */
    private String status;

    /**
     * 优先级：HIGH-高, NORMAL-中, LOW-低
     */
    private String priority;

    /**
     * 业务类型：LITIGATION-诉讼, NON_LITIGATION-非诉
     */
    private String businessType;

    /**
     * 预估金额
     */
    private BigDecimal estimatedAmount;

    /**
     * 案源描述
     */
    private String description;

    /**
     * 最后跟进时间
     */
    private LocalDateTime lastFollowTime;

    /**
     * 下次跟进时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 跟进次数
     */
    private Integer followCount;

    /**
     * 转化时间
     */
    private LocalDateTime convertedAt;

    /**
     * 转化后的客户ID
     */
    private Long convertedToClientId;

    /**
     * 转化后的项目ID
     */
    private Long convertedToMatterId;

    /**
     * 案源人ID
     */
    private Long originatorId;

    /**
     * 负责跟进人ID
     */
    private Long responsibleUserId;

    /**
     * 备注
     */
    private String remark;
}

