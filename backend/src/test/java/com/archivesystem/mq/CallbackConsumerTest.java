package com.archivesystem.mq;

import com.archivesystem.service.CallbackService;
import com.rabbitmq.client.Channel;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CallbackConsumerTest {

    @Mock
    private CallbackService callbackService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Channel channel;

    @Mock
    private Message amqpMessage;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private CallbackConsumer callbackConsumer;

    private CallbackMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = CallbackMessage.builder()
                .messageId("callback-001")
                .archiveId(1L)
                .archiveNo("ARC-20260213-0001")
                .sourceType("LAW_FIRM")
                .sourceId("123")
                .callbackUrl("http://callback.example.com/notify")
                .status(CallbackMessage.STATUS_SUCCESS)
                .successCount(5)
                .failedCount(0)
                .totalCount(5)
                .completedAt(LocalDateTime.now())
                .retryCount(1)
                .maxRetries(3)
                .build();

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    void testHandleCallback_Success() throws Exception {
        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(true);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
    }

    @Test
    void testHandleCallback_Failed_WithRetry() throws Exception {
        testMessage.setRetryCount(0);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(false);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void testHandleCallback_Failed_MaxRetriesExceeded() throws Exception {
        testMessage.setRetryCount(3);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(false);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
        verify(callbackService).logFailedCallback(eq(testMessage), anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
    }

    @Test
    void testHandleCallback_Exception_WithRetry() throws Exception {
        testMessage.setRetryCount(0);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class)))
                .thenThrow(new RuntimeException("网络异常"));
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void testHandleCallback_Exception_MaxRetriesExceeded() throws Exception {
        testMessage.setRetryCount(3);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class)))
                .thenThrow(new RuntimeException("网络异常"));
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
        verify(callbackService).logFailedCallback(eq(testMessage), eq("回调失败，请联系系统管理员查看系统日志"));
    }

    @Test
    void testHandleCallback_PartialStatus() throws Exception {
        testMessage.setStatus(CallbackMessage.STATUS_PARTIAL);
        testMessage.setSuccessCount(3);
        testMessage.setFailedCount(2);
        testMessage.setTotalCount(5);

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(true);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void testHandleCallback_FailedStatus() throws Exception {
        testMessage.setStatus(CallbackMessage.STATUS_FAILED);
        testMessage.setSuccessCount(0);
        testMessage.setFailedCount(5);
        testMessage.setTotalCount(5);
        testMessage.setErrorMessage("所有文件下载失败");

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(true);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void testHandleCallback_IncrementalRetry() throws Exception {
        // Test retry count incrementing
        testMessage.setRetryCount(1);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(false);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void testHandleCallback_RequeueWhenRepublishFails() throws Exception {
        testMessage.setRetryCount(0);
        testMessage.setMaxRetries(3);

        when(callbackService.sendCallback(any(CallbackMessage.class))).thenReturn(false);
        doThrow(new AmqpRejectAndDontRequeueException("重投失败"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));

        callbackConsumer.handleCallback(testMessage, amqpMessage, channel);

        verify(callbackService).sendCallback(testMessage);
        verify(channel, never()).basicAck(1L, false);
        verify(channel).basicNack(1L, false, true);
    }
}
