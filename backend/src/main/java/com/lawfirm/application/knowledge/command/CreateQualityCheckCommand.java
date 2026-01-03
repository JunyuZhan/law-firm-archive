package com.lawfirm.application.knowledge.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建质量检查命令（M10-031）
 */
@Data
public class CreateQualityCheckCommand {
    private Long matterId;
    private LocalDate checkDate;
    private String checkType;
    private String checkSummary;
    private List<CheckDetailCommand> details;

    @Data
    public static class CheckDetailCommand {
        private Long standardId;
        private String checkResult;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String findings;
        private String suggestions;
    }
}

