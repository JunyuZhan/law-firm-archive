package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.entity.DeadLetterRecord;
import com.archivesystem.repository.DeadLetterRecordMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.DeadLetterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 死信消息服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {
    private static final String DEAD_LETTER_RETRY_FAILURE_PUBLIC_MESSAGE = "重试失败，请联系系统管理员查看系统日志";

    private final DeadLetterRecordMapper deadLetterRecordMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public DeadLetterRecord save(DeadLetterRecord record) {
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        if (record.getStatus() == null) {
            record.setStatus(DeadLetterRecord.STATUS_PENDING);
        }
        if (record.getRetryCount() == null) {
            record.setRetryCount(0);
        }
        if (record.getMaxRetries() == null) {
            record.setMaxRetries(3);
        }
        record.setUpdatedAt(LocalDateTime.now());
        deadLetterRecordMapper.insert(record);
        log.info("保存死信消息记录: id={}, queueName={}", record.getId(), record.getQueueName());
        return record;
    }

    @Override
    public PageResult<DeadLetterRecord> getList(String status, String queueName, int page, int size) {
        Page<DeadLetterRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DeadLetterRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isBlank()) {
            wrapper.eq(DeadLetterRecord::getStatus, status);
        }
        if (queueName != null && !queueName.isBlank()) {
            wrapper.eq(DeadLetterRecord::getQueueName, queueName);
        }
        wrapper.orderByDesc(DeadLetterRecord::getCreatedAt);
        
        Page<DeadLetterRecord> result = deadLetterRecordMapper.selectPage(pageParam, wrapper);
        return PageResult.of(page, size, result.getTotal(), result.getRecords());
    }

    @Override
    public DeadLetterRecord getById(Long id) {
        DeadLetterRecord record = deadLetterRecordMapper.selectById(id);
        if (record == null) {
            throw NotFoundException.of("死信记录", id);
        }
        return record;
    }

    @Override
    @Transactional
    public boolean retry(Long id) {
        DeadLetterRecord record = getById(id);
        
        if (!record.canRetry()) {
            throw new BusinessException("该消息状态不允许重试");
        }
        
        if (record.isMaxRetryExceeded()) {
            throw new BusinessException("已超过最大重试次数");
        }
        
        try {
            record.setStatus(DeadLetterRecord.STATUS_RETRYING);
            record.incrementRetryCount();
            record.setUpdatedAt(LocalDateTime.now());
            deadLetterRecordMapper.updateById(record);
            
            // 重新发送消息到原队列
            resendMessage(record);
            
            log.info("死信消息重试: id={}, retryCount={}", id, record.getRetryCount());
            return true;
            
        } catch (Exception e) {
            log.error("死信消息重试失败: id={}", id, e);
            record.setStatus(DeadLetterRecord.STATUS_FAILED);
            record.setErrorMessage(DEAD_LETTER_RETRY_FAILURE_PUBLIC_MESSAGE);
            record.setUpdatedAt(LocalDateTime.now());
            deadLetterRecordMapper.updateById(record);
            return false;
        }
    }

    @Override
    @Transactional
    public int batchRetry(Long[] ids) {
        int successCount = 0;
        for (Long id : ids) {
            try {
                if (retry(id)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("批量重试失败: id={}, error={}", id, e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    @Transactional
    public void ignore(Long id, String remark) {
        DeadLetterRecord record = getById(id);
        
        record.setStatus(DeadLetterRecord.STATUS_IGNORED);
        record.setProcessedBy(SecurityUtils.getCurrentUserId());
        record.setProcessedAt(LocalDateTime.now());
        record.setProcessRemark(remark);
        record.setUpdatedAt(LocalDateTime.now());
        deadLetterRecordMapper.updateById(record);
        
        log.info("死信消息已忽略: id={}, processedBy={}", id, record.getProcessedBy());
    }

    @Override
    @Transactional
    public int batchIgnore(Long[] ids, String remark) {
        int successCount = 0;
        for (Long id : ids) {
            try {
                ignore(id, remark);
                successCount++;
            } catch (Exception e) {
                log.warn("批量忽略失败: id={}, error={}", id, e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put(DeadLetterRecord.STATUS_PENDING, 0);
        stats.put(DeadLetterRecord.STATUS_RETRYING, 0);
        stats.put(DeadLetterRecord.STATUS_SUCCESS, 0);
        stats.put(DeadLetterRecord.STATUS_FAILED, 0);
        stats.put(DeadLetterRecord.STATUS_IGNORED, 0);
        
        List<DeadLetterRecordMapper.StatusCount> counts = deadLetterRecordMapper.countByStatus();
        for (DeadLetterRecordMapper.StatusCount sc : counts) {
            stats.put(sc.getStatus(), sc.getCount());
        }
        
        return stats;
    }

    @Override
    @Transactional
    public int autoRetry(int limit) {
        List<DeadLetterRecord> records = deadLetterRecordMapper.findRetryableMessages(limit);
        int successCount = 0;
        
        for (DeadLetterRecord record : records) {
            try {
                if (retry(record.getId())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("自动重试失败: id={}, error={}", record.getId(), e.getMessage());
            }
        }
        
        log.info("自动重试完成: total={}, success={}", records.size(), successCount);
        return successCount;
    }

    /**
     * 重新发送消息到原队列.
     */
    private void resendMessage(DeadLetterRecord record) {
        String queueName = record.getQueueName();
        String messageBody = record.getMessageBody();
        
        // 根据队列名确定路由键
        String routingKey = determineRoutingKey(queueName);
        
        // 发送消息
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ARCHIVE_EXCHANGE,
            routingKey,
            messageBody,
            message -> {
                message.getMessageProperties().setHeader("x-retry-from-dlq", true);
                message.getMessageProperties().setHeader("x-dlq-record-id", record.getId());
                return message;
            }
        );
        
        log.info("消息已重新发送: recordId={}, routingKey={}", record.getId(), routingKey);
    }

    /**
     * 根据队列名确定路由键.
     */
    private String determineRoutingKey(String queueName) {
        if (RabbitMQConfig.ARCHIVE_RECEIVE_QUEUE.equals(queueName)) {
            return RabbitMQConfig.ARCHIVE_RECEIVE_ROUTING_KEY;
        } else if (RabbitMQConfig.ARCHIVE_CALLBACK_QUEUE.equals(queueName)) {
            return RabbitMQConfig.ARCHIVE_CALLBACK_ROUTING_KEY;
        }
        return queueName;
    }
}
