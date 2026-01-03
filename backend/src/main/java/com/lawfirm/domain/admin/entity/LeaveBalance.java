package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 假期余额实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("leave_balance")
public class LeaveBalance extends BaseEntity {

    /** 员工ID */
    private Long userId;

    /** 请假类型ID */
    private Long leaveTypeId;

    /** 年度 */
    private Integer year;

    /** 总天数 */
    private BigDecimal totalDays;

    /** 已用天数 */
    private BigDecimal usedDays;

    /** 剩余天数 */
    private BigDecimal remainingDays;
}
