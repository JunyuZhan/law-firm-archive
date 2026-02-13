package com.archivesystem.mq;

import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.service.CallbackService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 回调消息消费者
 * 负责将处理结果通知给外部系统
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackConsumer {

    private final CallbackService callbackService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 消费回调消息
     */
    @RabbitListener(queues = RabbitMQConfig.ARCHIVE_CALLBACK_QUEUE)
    public void handleCallback(CallbackMessage message, Message amqpMessage, Channel channel) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId();
        
        log.info("开始处理回调消息: messageId={}, archiveId={}, callbackUrl={}", 
                messageId, message.getArchiveId(), message.getCallbackUrl());
        
        try {
            // 执行回调
            boolean success = callbackService.sendCallback(message);
            
            if (success) {
                channel.basicAck(deliveryTag, false);
                log.info("回调成功: messageId={}, archiveId={}", messageId, message.getArchiveId());
            } else {
                // 回调失败，检查是否需要重试
                handleRetry(message, channel, deliveryTag, "回调请求失败");
            }
            
        } catch (Exception e) {
            log.error("回调处理异常: messageId={}", messageId, e);
            try {
                handleRetry(message, channel, deliveryTag, e.getMessage());
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }
    
    /**
     * 处理重试逻辑
     */
    private void handleRetry(CallbackMessage message, Channel channel, 
            long deliveryTag, String errorMessage) throws IOException {
        if (message.getRetryCount() < message.getMaxRetries()) {
            message.setRetryCount(message.getRetryCount() + 1);
            log.info("回调重试: messageId={}, retryCount={}", message.getMessageId(), message.getRetryCount());
            
            // 重新发送消息（带延迟）
            channel.basicAck(deliveryTag, false);
            
            // 延迟后重新发送
            try {
                Thread.sleep(calculateRetryDelay(message.getRetryCount()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARCHIVE_EXCHANGE,
                    RabbitMQConfig.ARCHIVE_CALLBACK_ROUTING_KEY,
                    message
            );
        } else {
            // 超过最大重试次数
            log.error("回调超过最大重试次数，放弃: messageId={}, archiveId={}", 
                    message.getMessageId(), message.getArchiveId());
            channel.basicAck(deliveryTag, false);
            
            // 记录失败日志（可扩展为持久化到数据库）
            callbackService.logFailedCallback(message, errorMessage);
        }
    }
    
    /**
     * 计算重试延迟（指数退避）
     */
    private long calculateRetryDelay(int retryCount) {
        // 1秒、2秒、4秒...
        return (long) Math.pow(2, retryCount - 1) * 1000;
    }
}
