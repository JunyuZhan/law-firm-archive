package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 手动计算提成命令
 */
@Data
public class ManualCalculateCommissionCommand {

    @NotNull(message = "收款ID不能为空")
    private Long paymentId;

    /**
     * 参与人提成列表
     */
    private List<ParticipantCommission> participants;

    /**
     * 参与人提成信息
     */
    @Data
    public static class ParticipantCommission {
        /** 参与人ID（ContractParticipant的ID） */
        @NotNull(message = "参与人ID不能为空")
        private Long participantId;

        /** 用户ID */
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        /** 提成比例（%） */
        private BigDecimal commissionRate;

        /** 提成金额（财务可手动修改） */
        private BigDecimal commissionAmount;

        /** 备注 */
        private String remark;
    }
}

