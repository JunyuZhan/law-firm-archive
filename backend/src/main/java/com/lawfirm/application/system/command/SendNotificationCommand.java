package com.lawfirm.application.system.command;

import lombok.Data;

import java.util.List;

/**
 * 发送通知命令
 */
@Data
public class SendNotificationCommand {
    /** 通知标题 */
    private String title;
    /** 通知内容 */
    private String content;
    /** 类型 */
    private String type;
    /** 接收者ID列表 */
    private List<Long> receiverIds;
    /** 关联业务类型 */
    private String businessType;
    /** 关联业务ID */
    private Long businessId;
}
