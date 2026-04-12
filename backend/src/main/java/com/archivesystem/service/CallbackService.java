package com.archivesystem.service;

import com.archivesystem.mq.CallbackMessage;

/**
 * 回调服务接口
 * 负责向外部系统发送档案处理结果通知
 * @author junyuzhan
 */
public interface CallbackService {

    /**
     * 发送回调通知
     * 
     * @param message 回调消息
     * @return 是否发送成功
     */
    boolean sendCallback(CallbackMessage message);

    /**
     * 记录失败的回调
     * 
     * @param message 回调消息
     * @param errorMessage 错误信息
     */
    void logFailedCallback(CallbackMessage message, String errorMessage);
}
