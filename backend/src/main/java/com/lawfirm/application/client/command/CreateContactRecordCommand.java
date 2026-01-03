package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建联系记录命令
 */
@Data
public class CreateContactRecordCommand {

    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    private Long contactId; // 联系人ID（可选）

    private String contactPerson; // 联系人姓名（如果未指定contactId）

    @NotBlank(message = "联系方式不能为空")
    private String contactMethod; // PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他

    @NotNull(message = "联系时间不能为空")
    private LocalDateTime contactDate;

    private Integer contactDuration; // 联系时长（分钟）
    private String contactLocation; // 联系地点
    private String contactContent; // 联系内容
    private String contactResult; // 联系结果
    private LocalDate nextFollowUpDate; // 下次跟进日期
    private Boolean followUpReminder; // 是否设置提醒
}

