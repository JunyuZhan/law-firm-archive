package com.lawfirm.application.hr.command;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提交评价命令
 */
@Data
public class SubmitEvaluationCommand {

    private Long taskId;
    private Long employeeId;
    private String evaluationType;
    private String comment;
    private String strengths;
    private String improvements;
    private List<ScoreItem> scores;

    @Data
    public static class ScoreItem {
        private Long indicatorId;
        private BigDecimal score;
        private String comment;
    }
}
