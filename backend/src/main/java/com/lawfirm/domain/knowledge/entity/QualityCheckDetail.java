package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 质量检查明细实体（M10-031）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("quality_check_detail")
public class QualityCheckDetail extends BaseEntity {

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 标准ID
     */
    private Long standardId;

    /**
     * 检查结果：PASS-通过, FAIL-不通过, PARTIAL-部分通过
     */
    private String checkResult;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 满分
     */
    private BigDecimal maxScore;

    /**
     * 发现的问题
     */
    private String findings;

    /**
     * 改进建议
     */
    private String suggestions;

    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";
    public static final String RESULT_PARTIAL = "PARTIAL";
}

