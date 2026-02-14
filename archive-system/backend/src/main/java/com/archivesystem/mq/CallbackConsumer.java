package com.archivesystem.mq;

import com.archivesystem.config.RabbitMQConfig;
import com.archivesystem.service.CallbackService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate stringRedisTemplate;

    // 回调消息幂等性检查的 Redis Key 前缀
    private static final String CALLBACK_PROCESSED_PREFIX = "mq:processed:callback:";
    // 回调处理标记过期时间（7天）
    private static final long CALLBACK_PROCESSED_TTL_DAYS = 7;

    /**
     * 消费回调消息
     */
    @RabbitListener(queues = RabbitMQConfig.ARCHIVE_CALLBACK_QUEUE)
    public void handleCallback(CallbackMessage message, Message amqpMessage, Channel channel) {
        // 消息对象 null 检查
        if (message == null || amqpMessage == null || amqpMessage.getMessageProperties() == null) {
            log.error("回调消息对象为空或格式异常");
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
        
        // 关键字段 null 检查
        if (messageId == null || message.getCallbackUrl() == null) {
            log.error("回调消息关键字段为空: messageId={}, callbackUrl={}", messageId, message.getCallbackUrl());
            try {
                channel.basicReject(deliveryTag, false);
            } catch (Exception e) {
                log.error("拒绝异常消息失败", e);
            }
            return;
        }
        
        log.info("开始处理回调消息: messageId={}, archiveId={}, callbackUrl={}", 
                messageId, message.getArchiveId(), message.getCallbackUrl());
        
        try {
            // 幂等性检查（仅对成功的回调进行幂等检查，失败重试时不检查）
            String processedKey = CALLBACK_PROCESSED_PREFIX + messageId;
            if (message.getRetryCount() == 0) {
                Boolean isNew = stringRedisTemplate.opsForValue()
                        .setIfAbsent(processedKey, "processing", CALLBACK_PROCESSED_TTL_DAYS, TimeUnit.DAYS);
                if (Boolean.FALSE.equals(isNew)) {
                    log.warn("回调消息已处理过，跳过: messageId={}", messageId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }
            
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
