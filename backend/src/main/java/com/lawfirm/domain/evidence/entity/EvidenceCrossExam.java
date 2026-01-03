package com.lawfirm.domain.evidence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 质证记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("evidence_cross_exam")
public class EvidenceCrossExam implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 证据ID
     */
    private Long evidenceId;

    /**
     * 质证方：OUR_SIDE-我方, OPPOSITE-对方, COURT-法院
     */
    private String examParty;

    /**
     * 真实性意见
     */
    private String authenticityOpinion;
    private String authenticityReason;

    /**
     * 合法性意见
     */
    private String legalityOpinion;
    private String legalityReason;

    /**
     * 关联性意见
     */
    private String relevanceOpinion;
    private String relevanceReason;

    /**
     * 综合意见
     */
    private String overallOpinion;

    /**
     * 法院认定意见
     */
    private String courtOpinion;

    /**
     * 法院是否采纳
     */
    private Boolean courtAccepted;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
