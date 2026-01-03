package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 更新联系记录命令
 */
@Data
public class UpdateContactRecordCommand {

    @NotNull(message = "联系记录ID不能为空")
    private Long id;

    private Long contactId;
    private String contactPerson;
    private String contactMethod;
    private LocalDateTime contactDate;
    private Integer contactDuration;
    private String contactLocation;
    private String contactContent;
    private String contactResult;
    private LocalDate nextFollowUpDate;
    private Boolean followUpReminder;
}

