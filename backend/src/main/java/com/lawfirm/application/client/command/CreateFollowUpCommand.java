package com.lawfirm.application.client.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建跟进记录命令
 */
@Data
public class CreateFollowUpCommand {

    @NotNull(message = "案源ID不能为空")
    private Long leadId;

    @NotBlank(message = "跟进方式不能为空")
    private String followType;  // PHONE, EMAIL, VISIT, MEETING, OTHER

    @NotBlank(message = "跟进内容不能为空")
    private String followContent;

    private String followResult;  // POSITIVE, NEUTRAL, NEGATIVE
    private LocalDateTime nextFollowTime;
    private String nextFollowPlan;
}

