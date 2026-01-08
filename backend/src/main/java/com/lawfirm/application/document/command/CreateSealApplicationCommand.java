package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建用印申请命令
 */
@Data
public class CreateSealApplicationCommand {

    /**
     * 印章ID
     */
    @NotNull(message = "印章不能为空")
    private Long sealId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 用印文件名称
     */
    @NotBlank(message = "用印文件名称不能为空")
    private String documentName;

    /**
     * 文件类型
     */
    private String documentType;

    /**
     * 份数
     */
    private Integer copies;

    /**
     * 用印目的
     */
    private String usePurpose;

    /**
     * 预计用印日期
     */
    private LocalDate expectedUseDate;

    /**
     * 审批人ID（必填，由申请人选择）
     */
    @NotNull(message = "审批人不能为空")
    private Long approverId;
}
