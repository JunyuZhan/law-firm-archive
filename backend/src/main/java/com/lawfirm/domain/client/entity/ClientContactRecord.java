package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户联系记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_client_contact_record")
public class ClientContactRecord extends BaseEntity {

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 联系人ID（可选）
     */
    private Long contactId;

    /**
     * 联系人姓名（如果未指定contactId）
     */
    private String contactPerson;

    /**
     * 联系方式：PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他
     */
    private String contactMethod;

    /**
     * 联系时间
     */
    private LocalDateTime contactDate;

    /**
     * 联系时长（分钟）
     */
    private Integer contactDuration;

    /**
     * 联系地点
     */
    private String contactLocation;

    /**
     * 联系内容
     */
    private String contactContent;

    /**
     * 联系结果
     */
    private String contactResult;

    /**
     * 下次跟进日期
     */
    private LocalDate nextFollowUpDate;

    /**
     * 是否设置提醒
     */
    private Boolean followUpReminder;

    /**
     * 记录人ID
     */
    private Long createdBy;
}

