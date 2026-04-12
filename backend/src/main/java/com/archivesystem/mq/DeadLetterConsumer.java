package com.archivesystem.mq;

import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DeadLetterRecord;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.DeadLetterRecordMapper;
import com.archivesystem.repository.PushRecordMapper;
import com.archivesystem.service.AlertService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 死信队列消费者
 * 处理多次重试后仍然失败的消息
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final ArchiveMapper archiveMapper;
    private final PushRecordMapper pushRecordMapper;
    private final DeadLetterRecordMapper deadLetterRecordMapper;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    /**
     * 消费死信消息
     */
    @RabbitListener(queues = RabbitMQConfig.ARCHIVE_RECEIVE_DLQ)
    public void handleDeadLetter(Message message, Channel channel) {
        // 消息对象 null 检查
        if (message == null || message.getMessageProperties() == null || message.getBody() == null) {
            log.error("死信消息对象为空或格式异常");
            return;
        }
        
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String messageId = message.getMessageProperties().getMessageId();
        
        log.error("死信消息到达: routing_key={}, message={}", 
                routingKey, 
                messageBody.substring(0, Math.min(messageBody.length(), 500)));
        
        // 检查是否来自DLQ重试（避免循环）
        Boolean isRetryFromDlq = message.getMessageProperties().getHeader("x-retry-from-dlq");
        Long dlqRecordId = message.getMessageProperties().getHeader("x-dlq-record-id");
        
        try {
            // 尝试解析消息并更新相关状态
            ArchiveReceiveMessage archiveMessage = parseMessage(messageBody);
            
            // 保存或更新死信记录
            DeadLetterRecord dlRecord = saveDeadLetterRecord(
                    messageId, routingKey, messageBody, archiveMessage, 
                    isRetryFromDlq, dlqRecordId);
            
            if (archiveMessage != null) {
                handleFailedArchiveMessage(archiveMessage);
            }
            
            // 确认消息（从死信队列中移除）
            channel.basicAck(deliveryTag, false);
            log.info("死信消息处理完成，已从队列移除，记录ID={}", dlRecord.getId());
            
        } catch (Exception e) {
            log.error("死信消息处理异常", e);
            try {
                // 即使处理失败也确认消息，避免无限循环
                // 失败的死信消息应该通过日志和告警机制人工介入
                channel.basicAck(deliveryTag, false);
            } catch (IOException ioException) {
                log.error("确认死信消息失败", ioException);
            }
        }
    }

    /**
     * 保存死信记录.
     */
    private DeadLetterRecord saveDeadLetterRecord(
            String messageId, String routingKey, String messageBody, 
            ArchiveReceiveMessage archiveMessage, Boolean isRetryFromDlq, Long dlqRecordId) {
        
        // 如果是重试失败的消息，更新原记录
        if (Boolean.TRUE.equals(isRetryFromDlq) && dlqRecordId != null) {
            DeadLetterRecord existingRecord = deadLetterRecordMapper.selectById(dlqRecordId);
            if (existingRecord != null) {
                existingRecord.setStatus(DeadLetterRecord.STATUS_FAILED);
                existingRecord.setErrorMessage("重试后仍然失败");
                existingRecord.setUpdatedAt(LocalDateTime.now());
                deadLetterRecordMapper.updateById(existingRecord);
                log.info("更新死信记录（重试失败）: id={}", dlqRecordId);
                return existingRecord;
            }
        }
        
        // 创建新的死信记录
        DeadLetterRecord record = new DeadLetterRecord();
        record.setMessageId(messageId);
        record.setQueueName(RabbitMQConfig.ARCHIVE_RECEIVE_QUEUE);
        record.setRoutingKey(routingKey);
        record.setMessageBody(messageBody);
        record.setStatus(DeadLetterRecord.STATUS_PENDING);
        record.setRetryCount(0);
        record.setMaxRetries(3);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        if (archiveMessage != null) {
            record.setArchiveId(archiveMessage.getArchiveId());
            record.setSourceType(archiveMessage.getSourceType());
            record.setSourceId(archiveMessage.getSourceId());
        }
        
        deadLetterRecordMapper.insert(record);
        log.info("保存死信记录: id={}, archiveId={}", record.getId(), record.getArchiveId());
        
        return record;
    }
    
    /**
     * 解析消息内容
     */
    private ArchiveReceiveMessage parseMessage(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, ArchiveReceiveMessage.class);
        } catch (Exception e) {
            log.warn("解析死信消息失败，可能是非标准格式: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理失败的档案接收消息
     */
    private void handleFailedArchiveMessage(ArchiveReceiveMessage message) {
        Long archiveId = message.getArchiveId();
        
        // 更新档案状态为失败
        if (archiveId != null) {
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive != null && !Archive.STATUS_RECEIVED.equals(archive.getStatus())) {
                archive.setStatus(Archive.STATUS_FAILED);
                archive.setRemarks(appendRemark(archive.getRemarks(), 
                        "消息处理失败，已进入死信队列。时间: " + LocalDateTime.now()));
                archiveMapper.updateById(archive);
                log.info("已将档案状态更新为失败: archiveId={}", archiveId);
            }
        }
        
        // 更新推送记录状态
        if (message.getSourceType() != null && message.getSourceId() != null) {
            LambdaQueryWrapper<PushRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PushRecord::getSourceType, message.getSourceType())
                   .eq(PushRecord::getSourceId, message.getSourceId());
            PushRecord pushRecord = pushRecordMapper.selectOne(wrapper);
            
            if (pushRecord != null && !PushRecord.STATUS_SUCCESS.equals(pushRecord.getPushStatus())) {
                pushRecord.setPushStatus(PushRecord.STATUS_FAILED);
                pushRecord.setErrorMessage("消息处理失败，已进入死信队列");
                pushRecord.setProcessedAt(LocalDateTime.now());
                pushRecordMapper.updateById(pushRecord);
                log.info("已将推送记录状态更新为失败: pushRecordId={}", pushRecord.getId());
            }
        }
        
        // TODO: 发送告警通知（邮件、短信、钉钉等）
        sendAlertNotification(message);
    }
    
    /**
     * 发送告警通知
     */
    private void sendAlertNotification(ArchiveReceiveMessage message) {
        log.error("【告警】档案处理失败，需人工介入处理！archiveId={}, sourceType={}, sourceId={}", 
                message.getArchiveId(), message.getSourceType(), message.getSourceId());
        
        alertService.alertDeadLetter(
                message.getArchiveId(),
                message.getSourceType(),
                message.getSourceId(),
                "消息处理失败，已进入死信队列"
        );
    }
    
    /**
     * 追加备注信息
     */
    private String appendRemark(String existingRemark, String newRemark) {
        if (existingRemark == null || existingRemark.isBlank()) {
            return newRemark;
        }
        return existingRemark + "\n" + newRemark;
    }
}
