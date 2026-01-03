package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 职级通道实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_career_level")
public class CareerLevel extends BaseEntity {

    /**
     * 职级编码
     */
    private String levelCode;

    /**
     * 职级名称
     */
    private String levelName;

    /**
     * 职级顺序（数字越大级别越高）
     */
    private Integer levelOrder;

    /**
     * 通道类别：LAWYER-律师通道, ADMIN-行政通道, TECH-技术通道
     */
    private String category;

    /**
     * 职级描述
     */
    private String description;

    /**
     * 最低工作年限
     */
    private Integer minWorkYears;

    /**
     * 最低案件数量
     */
    private Integer minMatterCount;

    /**
     * 最低创收金额
     */
    private BigDecimal minRevenue;

    /**
     * 所需证书（JSON数组）
     */
    private String requiredCertificates;

    /**
     * 其他要求
     */
    private String otherRequirements;

    /**
     * 薪酬下限
     */
    private BigDecimal salaryMin;

    /**
     * 薪酬上限
     */
    private BigDecimal salaryMax;

    /**
     * 状态：ACTIVE-启用, INACTIVE-停用
     */
    private String status;
}
