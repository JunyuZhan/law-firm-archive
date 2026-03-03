package com.archivesystem.service;

import com.archivesystem.dto.alert.AlertMessage;

import java.util.List;

/**
 * 告警通知服务接口.
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
}
