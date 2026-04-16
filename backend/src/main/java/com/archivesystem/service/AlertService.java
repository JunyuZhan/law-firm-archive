package com.archivesystem.service;

import com.archivesystem.dto.alert.AlertMessage;

import java.util.List;

/**
 * 告警通知服务接口.
 * @author junyuzhan
 */
public interface AlertService {

    /**
     * 发送告警.
     *
     * @param message 告警消息
     * @return 是否成功
     */
    boolean send(AlertMessage message);

    /**
     * 发送告警到指定接收者.
     *
     * @param message   告警消息
     * @param receivers 接收者列表（邮箱或手机号）
     * @return 是否成功
     */
    boolean send(AlertMessage message, List<String> receivers);

    /**
     * 发送档案处理失败告警.
     *
     * @param archiveId  档案ID
     * @param archiveNo  档案号
     * @param reason     失败原因
     */
    void alertArchiveProcessFailed(Long archiveId, String archiveNo, String reason);

    /**
     * 发送档案即将过期告警.
     *
     * @param archiveId       档案ID
     * @param archiveNo       档案号
     * @param daysUntilExpire 距离过期天数
     */
    void alertArchiveExpiring(Long archiveId, String archiveNo, int daysUntilExpire);

    /**
     * 发送死信消息告警.
     *
     * @param recordId   死信记录ID
     * @param sourceType 来源类型
     * @param sourceId   来源ID
     * @param error      错误信息
     */
    void alertDeadLetter(Long recordId, String sourceType, String sourceId, String error);

    /**
     * 发送系统错误告警.
     *
     * @param error 错误信息
     */
    void alertSystemError(String error);

    /**
     * 发送系统事件邮件（如未处理异常），受「启用邮件」与「系统事件邮件」开关控制.
     *
     * @param title   标题
     * @param detail  正文（可含堆栈摘要）
     */
    void notifySystemEvent(String title, String detail);

    /**
     * 发送一封测试邮件，用于验证 SMTP 与收件人配置.
     *
     * @param overrideTo 非空时仅发往该地址；否则发往当前解析的管理员收件列表
     */
    void sendTestMail(String overrideTo);
}
