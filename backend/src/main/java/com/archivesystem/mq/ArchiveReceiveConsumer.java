package com.archivesystem.mq;

import com.archivesystem.config.MetricsConfig;
import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.repository.PushRecordMapper;
import com.archivesystem.service.FileStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 档案接收消息消费者
 * 异步处理档案文件的下载和存储
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveReceiveConsumer {

    private final ArchiveMapper archiveMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final PushRecordMapper pushRecordMapper;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;
    private final MetricsConfig metricsConfig;
    private final StringRedisTemplate stringRedisTemplate;

    // 消息幂等性检查的 Redis Key 前缀
    private static final String MESSAGE_PROCESSED_PREFIX = "mq:processed:archive:";
    // 消息处理标记过期时间（7天）
    private static final long MESSAGE_PROCESSED_TTL_DAYS = 7;

    /**
     * 消费档案接收消息
     */
    @RabbitListener(queues = RabbitMQConfig.ARCHIVE_RECEIVE_QUEUE)
    public void handleArchiveReceive(ArchiveReceiveMessage message, Message amqpMessage, Channel channel) {
        // 消息对象 null 检查
        if (message == null || amqpMessage == null || amqpMessage.getMessageProperties() == null) {
            log.error("消息对象为空或格式异常");
            try {
                if (amqpMessage != null && amqpMessage.getMessageProperties() != null) {
                    channel.basicReject(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                }
            } catch (Exception e) {
                log.error("拒绝异常消息失败", e);
            }
            return;
        }
        
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId();
        Long archiveId = message.getArchiveId();
        
        // 关键字段 null 检查
        if (messageId == null || archiveId == null) {
            log.error("消息关键字段为空: messageId={}, archiveId={}", messageId, archiveId);
            try {
                channel.basicReject(deliveryTag, false);
            } catch (Exception e) {
                log.error("拒绝异常消息失败", e);
            }
            return;
        }
        
        log.info("开始处理档案接收消息: messageId={}, archiveId={}", messageId, archiveId);
        metricsConfig.recordArchiveReceiveStart();
        
        String processedKey = MESSAGE_PROCESSED_PREFIX + messageId;
        
        try {
            // 检查档案是否存在
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive == null) {
                log.error("档案不存在: archiveId={}", archiveId);
                channel.basicReject(deliveryTag, false);
                metricsConfig.recordArchiveReceiveFailed();
                return;
            }
            
            // 基于档案状态的幂等性检查（比消息幂等更可靠）
            // 只有最终状态（RECEIVED、FAILED、PARTIAL）才跳过处理
            if (Archive.STATUS_RECEIVED.equals(archive.getStatus()) || 
                Archive.STATUS_FAILED.equals(archive.getStatus()) ||
                Archive.STATUS_PARTIAL.equals(archive.getStatus())) {
                if (tryDispatchPendingCompletionCallback(message, archive, deliveryTag, channel)) {
                    metricsConfig.recordArchiveReceiveSuccess();
                    return;
                }
                log.info("档案已处理完成，跳过: archiveId={}, status={}", archiveId, archive.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 消息幂等性检查（防止短时间内重复处理）
            // 注意：如果档案仍为 PROCESSING，说明之前可能处理中断，允许重新处理
            Boolean isNewMessage = stringRedisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "processing", MESSAGE_PROCESSED_TTL_DAYS, TimeUnit.DAYS);
            
            if (Boolean.FALSE.equals(isNewMessage)) {
                // 消息已处理过，但档案仍为 PROCESSING 状态，说明之前处理可能中断
                if (Archive.STATUS_PROCESSING.equals(archive.getStatus())) {
                    log.warn("检测到之前处理可能中断，重新处理: messageId={}, archiveId={}", messageId, archiveId);
                    // 删除旧的幂等标记，重新设置
                    stringRedisTemplate.delete(processedKey);
                    stringRedisTemplate.opsForValue().set(processedKey, "reprocessing", MESSAGE_PROCESSED_TTL_DAYS, TimeUnit.DAYS);
                } else {
                    log.warn("消息已处理过，跳过: messageId={}", messageId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }
            
            // 更新状态为处理中
            archive.setStatus(Archive.STATUS_PROCESSING);
            archiveMapper.updateById(archive);
            
            // 处理文件下载
            int successCount = 0;
            int failedCount = 0;
            long totalSize = 0;
            List<String> failedFiles = new ArrayList<>();
            
            if (message.getFiles() != null && !message.getFiles().isEmpty()) {
                for (ArchiveReceiveMessage.FileInfo fileInfo : message.getFiles()) {
                    // 跳过空的文件信息
                    if (fileInfo == null || fileInfo.getDownloadUrl() == null) {
                        log.warn("文件信息为空或缺少下载URL，跳过");
                        failedCount++;
                        failedFiles.add("空文件信息");
                        continue;
                    }
                    
                    try {
                        DigitalFile digitalFile = fileStorageService.downloadAndStore(
                                archiveId,
                                fileInfo.getDownloadUrl(),
                                fileInfo.getFileName(),
                                fileInfo.getFileCategory(),
                                fileInfo.getSortOrder()
                        );
                        
                        if (digitalFile != null) {
                            if (fileInfo.getVolumeNo() != null) {
                                digitalFile.setVolumeNo(fileInfo.getVolumeNo());
                            }
                            if (fileInfo.getSectionType() != null) {
                                digitalFile.setSectionType(fileInfo.getSectionType());
                            }
                            if (fileInfo.getDocumentNo() != null) {
                                digitalFile.setDocumentNo(fileInfo.getDocumentNo());
                            }
                            if (fileInfo.getPageStart() != null) {
                                digitalFile.setPageStart(fileInfo.getPageStart());
                            }
                            if (fileInfo.getPageEnd() != null) {
                                digitalFile.setPageEnd(fileInfo.getPageEnd());
                            }
                            if (fileInfo.getVersionLabel() != null) {
                                digitalFile.setVersionLabel(fileInfo.getVersionLabel());
                            }
                            if (fileInfo.getFileSourceType() != null) {
                                digitalFile.setFileSourceType(fileInfo.getFileSourceType());
                            }
                            if (fileInfo.getScanBatchNo() != null) {
                                digitalFile.setScanBatchNo(fileInfo.getScanBatchNo());
                            }
                            if (fileInfo.getScanOperator() != null) {
                                digitalFile.setScanOperator(fileInfo.getScanOperator());
                            }
                            if (fileInfo.getScanTime() != null) {
                                digitalFile.setScanTime(fileInfo.getScanTime());
                            }
                            if (fileInfo.getScanCheckStatus() != null) {
                                digitalFile.setScanCheckStatus(fileInfo.getScanCheckStatus());
                            }
                            if (fileInfo.getScanCheckBy() != null) {
                                digitalFile.setScanCheckBy(fileInfo.getScanCheckBy());
                            }
                            if (fileInfo.getScanCheckTime() != null) {
                                digitalFile.setScanCheckTime(fileInfo.getScanCheckTime());
                            }
                            if (fileInfo.getVolumeNo() != null
                                    || fileInfo.getSectionType() != null
                                    || fileInfo.getDocumentNo() != null
                                    || fileInfo.getPageStart() != null
                                    || fileInfo.getPageEnd() != null
                                    || fileInfo.getVersionLabel() != null
                                    || fileInfo.getFileSourceType() != null
                                    || fileInfo.getScanBatchNo() != null
                                    || fileInfo.getScanOperator() != null
                                    || fileInfo.getScanTime() != null
                                    || fileInfo.getScanCheckStatus() != null
                                    || fileInfo.getScanCheckBy() != null
                                    || fileInfo.getScanCheckTime() != null) {
                                digitalFileMapper.updateById(digitalFile);
                            }
                            successCount++;
                            totalSize += digitalFile.getFileSize() != null ? digitalFile.getFileSize() : 0;
                            log.info("文件处理成功: fileName={}", fileInfo.getFileName());
                        }
                    } catch (Exception e) {
                        failedCount++;
                        failedFiles.add(fileInfo.getFileName());
                        log.error("文件处理失败: fileName={}, error={}", fileInfo.getFileName(), e.getMessage());
                    }
                }
            }
            
            // 确定最终状态
            String finalStatus;
            if (failedCount == 0) {
                finalStatus = Archive.STATUS_RECEIVED;
            } else if (successCount > 0) {
                finalStatus = Archive.STATUS_PARTIAL;
            } else {
                finalStatus = Archive.STATUS_FAILED;
            }
            
            // 更新档案状态和统计
            archive.setFileCount(successCount);
            archive.setTotalFileSize(totalSize);
            archive.setHasElectronic(successCount > 0);
            archive.setStatus(finalStatus);
            archiveMapper.updateById(archive);
            
            // 同步更新推送记录状态
            updatePushRecord(message.getSourceType(), message.getSourceId(), 
                    finalStatus, successCount, failedCount, 
                    failedFiles.isEmpty() ? null : String.join(", ", failedFiles));

            markCompletionCallbackPending(message);
            
            // 发送回调通知
            if (message.getCallbackUrl() != null && !message.getCallbackUrl().isEmpty()) {
                sendCallbackMessage(message, successCount, failedCount, 
                        message.getFiles() != null ? message.getFiles().size() : 0,
                        failedFiles.isEmpty() ? null : String.join(", ", failedFiles));
            }
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            metricsConfig.recordArchiveReceiveSuccess();
            log.info("档案接收处理完成: archiveId={}, successCount={}, failedCount={}", 
                    archiveId, successCount, failedCount);
            
        } catch (Exception e) {
            log.error("档案接收处理异常: messageId={}, archiveId={}", messageId, archiveId, e);
            metricsConfig.recordArchiveReceiveFailed();
            
            // 删除幂等标记，允许重试
            try {
                stringRedisTemplate.delete(processedKey);
            } catch (Exception redisEx) {
                log.warn("删除幂等标记失败: {}", redisEx.getMessage());
            }
            
            try {
                // 检查是否需要重试
                if (message.getRetryCount() < message.getMaxRetries()) {
                    message.setRetryCount(message.getRetryCount() + 1);
                    log.info("重试处理: messageId={}, retryCount={}", messageId, message.getRetryCount());

                    requeueForRetry(message, channel, deliveryTag);
                } else {
                    // 超过最大重试次数，进入死信队列
                    log.error("超过最大重试次数，消息进入死信队列: messageId={}", messageId);
                    channel.basicReject(deliveryTag, false);
                    
                    // 更新档案状态为失败
                    Archive archive = archiveMapper.selectById(archiveId);
                    if (archive != null) {
                        archive.setStatus(Archive.STATUS_FAILED);
                        archiveMapper.updateById(archive);
                    }
                    
                    // 发送失败回调
                    if (message.getCallbackUrl() != null) {
                        sendFailureCallback(message, e.getMessage());
                    }
                }
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }

    private void requeueForRetry(ArchiveReceiveMessage message, Channel channel, long deliveryTag)
            throws IOException {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARCHIVE_EXCHANGE,
                    RabbitMQConfig.ARCHIVE_RECEIVE_ROUTING_KEY,
                    message
            );
            channel.basicAck(deliveryTag, false);
        } catch (AmqpException e) {
            log.error("档案接收消息重投失败，原消息将重新入队: messageId={}, retryCount={}",
                    message.getMessageId(), message.getRetryCount(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private boolean tryDispatchPendingCompletionCallback(
            ArchiveReceiveMessage message, Archive archive, long deliveryTag, Channel channel) throws IOException {
        PushRecord pushRecord = selectLatestPushRecord(message.getSourceType(), message.getSourceId());
        if (pushRecord == null
                || !PushRecord.FILE_STATUS_CALLBACK_PENDING.equals(pushRecord.getFileStatus())
                || pushRecord.getCallbackUrl() == null
                || pushRecord.getCallbackUrl().isEmpty()) {
            return false;
        }

        int successCount = pushRecord.getSuccessFiles() != null ? pushRecord.getSuccessFiles() : 0;
        int failedCount = pushRecord.getFailedFiles() != null ? pushRecord.getFailedFiles() : 0;
        int totalCount = pushRecord.getTotalFiles() != null ? pushRecord.getTotalFiles() : successCount + failedCount;

        sendCallbackMessage(
                message,
                successCount,
                failedCount,
                totalCount,
                pushRecord.getErrorMessage());
        channel.basicAck(deliveryTag, false);
        log.info("档案已完成但补发完成回调成功: archiveId={}, status={}", archive.getId(), archive.getStatus());
        return true;
    }
    
    /**
     * 发送回调消息到回调队列
     */
    private void sendCallbackMessage(ArchiveReceiveMessage original, int successCount, 
            int failedCount, int totalCount, String errorMessage) {
        String status;
        if (failedCount == 0) {
            status = CallbackMessage.STATUS_SUCCESS;
        } else if (successCount > 0) {
            status = CallbackMessage.STATUS_PARTIAL;
        } else {
            status = CallbackMessage.STATUS_FAILED;
        }
        
        CallbackMessage callback = CallbackMessage.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .archiveId(original.getArchiveId())
                .archiveNo(original.getArchiveNo())
                .sourceType(original.getSourceType())
                .sourceId(original.getSourceId())
                .sourceNo(original.getSourceNo())
                .callbackUrl(original.getCallbackUrl())
                .status(status)
                .successCount(successCount)
                .failedCount(failedCount)
                .totalCount(totalCount)
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ARCHIVE_EXCHANGE,
                RabbitMQConfig.ARCHIVE_CALLBACK_ROUTING_KEY,
                callback
        );
        markCompletionCallbackSent(original);
        
        log.info("已发送回调消息: archiveId={}, status={}", original.getArchiveId(), status);
    }
    
    /**
     * 发送失败回调
     */
    private void sendFailureCallback(ArchiveReceiveMessage original, String errorMessage) {
        CallbackMessage callback = CallbackMessage.builder()
                .messageId(java.util.UUID.randomUUID().toString())
                .archiveId(original.getArchiveId())
                .archiveNo(original.getArchiveNo())
                .sourceType(original.getSourceType())
                .sourceId(original.getSourceId())
                .sourceNo(original.getSourceNo())
                .callbackUrl(original.getCallbackUrl())
                .status(CallbackMessage.STATUS_FAILED)
                .successCount(0)
                .failedCount(original.getFiles() != null ? original.getFiles().size() : 0)
                .totalCount(original.getFiles() != null ? original.getFiles().size() : 0)
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ARCHIVE_EXCHANGE,
                RabbitMQConfig.ARCHIVE_CALLBACK_ROUTING_KEY,
                callback
        );
        
        // 同步更新推送记录状态为失败
        updatePushRecord(original.getSourceType(), original.getSourceId(),
                PushRecord.STATUS_FAILED, 0, 
                original.getFiles() != null ? original.getFiles().size() : 0,
                errorMessage);
    }

    private void markCompletionCallbackPending(ArchiveReceiveMessage message) {
        if (message.getCallbackUrl() == null || message.getCallbackUrl().isEmpty()) {
            return;
        }
        updatePushRecordFileStatus(message.getSourceType(), message.getSourceId(), PushRecord.FILE_STATUS_CALLBACK_PENDING);
    }

    private void markCompletionCallbackSent(ArchiveReceiveMessage message) {
        updatePushRecordFileStatus(message.getSourceType(), message.getSourceId(), PushRecord.FILE_STATUS_CALLBACK_SENT);
    }

    private void updatePushRecordFileStatus(String sourceType, String sourceId, String fileStatus) {
        try {
            PushRecord pushRecord = selectLatestPushRecord(sourceType, sourceId);
            if (pushRecord != null) {
                pushRecord.setFileStatus(fileStatus);
                pushRecordMapper.updateById(pushRecord);
            }
        } catch (Exception e) {
            log.error("更新推送记录文件状态失败: sourceId={}, fileStatus={}, error={}",
                    sourceId, fileStatus, e.getMessage());
        }
    }

    private PushRecord selectLatestPushRecord(String sourceType, String sourceId) {
        return pushRecordMapper.selectOne(
                new LambdaQueryWrapper<PushRecord>()
                        .eq(PushRecord::getSourceType, sourceType)
                        .eq(PushRecord::getSourceId, sourceId)
                        .orderByDesc(PushRecord::getCreatedAt)
                        .last("LIMIT 1"));
    }
    
    /**
     * 更新推送记录状态
     */
    private void updatePushRecord(String sourceType, String sourceId, String status,
                                   int successCount, int failedCount, String errorMessage) {
        try {
            PushRecord pushRecord = selectLatestPushRecord(sourceType, sourceId);
            
            if (pushRecord != null) {
                pushRecord.setPushStatus(status);
                pushRecord.setSuccessFiles(successCount);
                pushRecord.setFailedFiles(failedCount);
                pushRecord.setProcessedAt(LocalDateTime.now());
                if (errorMessage != null) {
                    pushRecord.setErrorMessage(errorMessage);
                }
                pushRecordMapper.updateById(pushRecord);
                log.info("推送记录状态已更新: sourceId={}, status={}", sourceId, status);
            }
        } catch (Exception e) {
            log.error("更新推送记录状态失败: sourceId={}, error={}", sourceId, e.getMessage());
        }
    }
}
