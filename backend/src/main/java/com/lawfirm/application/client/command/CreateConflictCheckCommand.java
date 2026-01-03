package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建利冲检查命令
 */
@Data
public class CreateConflictCheckCommand {

    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    private Long clientId;

    private String remark;

    /**
     * 检查当事人列表
     */
    @NotEmpty(message = "检查当事人不能为空")
    private List<PartyCommand> parties;

    @Data
    public static class PartyCommand {
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
    }
}

