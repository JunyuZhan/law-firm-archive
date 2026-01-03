package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 利冲检查项实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_conflict_check_item")
public class ConflictCheckItem extends BaseEntity {

    /**
     * 所属检查ID
     */
    private Long checkId;

    /**
     * 当事人名称
     */
    private String partyName;

    /**
     * 当事人类型：CLIENT-委托人, OPPOSING-对方当事人, RELATED-关联方
     */
    private String partyType;

    /**
     * 证件号码
     */
    private String idNumber;

    /**
     * 是否存在冲突
     */
    private Boolean hasConflict;

    /**
     * 冲突详情
     */
    private String conflictDetail;

    /**
     * 关联案件ID
     */
    private Long relatedMatterId;

    /**
     * 关联客户ID
     */
    private Long relatedClientId;
}

