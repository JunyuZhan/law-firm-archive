package com.archivesystem.mq;

import com.archivesystem.config.MetricsConfig;
import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.service.FileStorageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 档案接收消息消费者
 * 异步处理档案文件的下载和存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveReceiveConsumer {

    private final ArchiveMapper archiveMapper;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;
    private final MetricsConfig metricsConfig;

    /**
     * 消费档案接收消息
     */
    @RabbitListener(queues = RabbitMQConfig.ARCHIVE_RECEIVE_QUEUE)
    public void handleArchiveReceive(ArchiveReceiveMessage message, Message amqpMessage, Channel channel) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId();
        Long archiveId = message.getArchiveId();
        
        log.info("开始处理档案接收消息: messageId={}, archiveId={}", messageId, archiveId);
        metricsConfig.recordArchiveReceiveStart();
        
        try {
            // 检查档案是否存在
            Archive archive = archiveMapper.selectById(archiveId);
            if (archive == null) {
                log.error("档案不存在: archiveId={}", archiveId);
                channel.basicReject(deliveryTag, false);
                metricsConfig.recordArchiveReceiveFailed();
                return;
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
                    try {
                        DigitalFile digitalFile = fileStorageService.downloadAndStore(
                                archiveId,
                                fileInfo.getDownloadUrl(),
                                fileInfo.getFileName(),
                                fileInfo.getFileCategory(),
                                fileInfo.getSortOrder()
                        );
                        
                        if (digitalFile != null) {
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
            
            // 更新档案状态和统计
            archive.setFileCount(successCount);
            archive.setTotalFileSize(totalSize);
            archive.setHasElectronic(successCount > 0);
            
            if (failedCount == 0) {
                archive.setStatus(Archive.STATUS_RECEIVED);
            } else if (successCount > 0) {
                archive.setStatus(Archive.STATUS_PARTIAL);
            } else {
                archive.setStatus(Archive.STATUS_FAILED);
            }
            
            archiveMapper.updateById(archive);
            
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
            
            try {
                // 检查是否需要重试
                if (message.getRetryCount() < message.getMaxRetries()) {
                    message.setRetryCount(message.getRetryCount() + 1);
                    log.info("重试处理: messageId={}, retryCount={}", messageId, message.getRetryCount());
                    
                    // 重新发送消息（带延迟）
                    channel.basicReject(deliveryTag, false);
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
    }
}
