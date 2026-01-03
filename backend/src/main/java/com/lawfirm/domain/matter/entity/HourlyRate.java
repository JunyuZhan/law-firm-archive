package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 小时费率实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hourly_rate")
public class HourlyRate extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 费率
     */
    private BigDecimal rate;

    /**
     * 币种
     */
    @lombok.Builder.Default
    private String currency = "CNY";

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expiryDate;

    /**
     * 状态
     */
    @lombok.Builder.Default
    private String status = "ACTIVE";
}
