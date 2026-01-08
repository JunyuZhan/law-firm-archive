package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 提交评审命令
 */
@Data
public class SubmitReviewCommand {
    
    @NotNull(message = "晋升申请ID不能为空")
    private Long applicationId;
    
    /**
     * 评审人角色：DIRECT_MANAGER-直属上级, HR-人力资源, TEAM_LEADER-团队负责人, COMMITTEE-评审委员会
     */
    @NotBlank(message = "评审人角色不能为空")
    private String reviewerRole;
    
    /**
     * 评分明细
     */
    private Map<String, Object> scoreDetails;
    
    /**
     * 总分
     */
    private BigDecimal totalScore;
    
    /**
     * 评审意见：APPROVE-同意, REJECT-不同意, ABSTAIN-弃权
     */
    @NotBlank(message = "评审意见不能为空")
    private String reviewOpinion;
    
    /**
     * 评审评语
     */
    private String reviewComment;
}
