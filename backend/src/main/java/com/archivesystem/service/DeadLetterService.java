package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.DeadLetterRecord;

import java.util.Map;

/**
 * 死信消息服务接口.
 */
public interface DeadLetterService {

    /**
     * 保存死信消息记录.
     *
     * @param record 死信记录
     * @return 保存后的记录
     */
    DeadLetterRecord save(DeadLetterRecord record);

    /**
     * 分页查询死信消息.
     *
     * @param status   状态过滤
     * @param queueName 队列名过滤
     * @param page     页码
     * @param size     每页大小
     * @return 分页结果
     */
    PageResult<DeadLetterRecord> getList(String status, String queueName, int page, int size);

    /**
     * 获取死信消息详情.
     *
     * @param id 记录ID
     * @return 死信记录
     */
    DeadLetterRecord getById(Long id);

    /**
     * 重试处理死信消息.
     *
     * @param id 记录ID
     * @return 是否成功
     */
    boolean retry(Long id);

    /**
     * 批量重试.
     *
     * @param ids 记录ID列表
     * @return 成功数量
     */
    int batchRetry(Long[] ids);

    /**
     * 忽略死信消息.
     *
     * @param id     记录ID
     * @param remark 处理备注
     */
    void ignore(Long id, String remark);

    /**
     * 批量忽略.
     *
     * @param ids    记录ID列表
     * @param remark 处理备注
     * @return 成功数量
     */
    int batchIgnore(Long[] ids, String remark);

    /**
     * 获取统计信息.
     *
     * @return 各状态的消息数量
     */
    Map<String, Integer> getStatistics();

    /**
     * 自动重试待处理的消息.
     * 用于定时任务调用.
     *
     * @param limit 每次处理的最大数量
     * @return 处理结果
     */
    int autoRetry(int limit);
}
