package com.archivesystem.service;

import com.archivesystem.entity.DeadLetterRecord;
import com.archivesystem.repository.DeadLetterRecordMapper;
import com.archivesystem.service.impl.DeadLetterServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterServiceTest {

    @Mock
    private DeadLetterRecordMapper deadLetterRecordMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DeadLetterServiceImpl deadLetterService;

    @Test
    void testRetry_ShouldHideInternalErrorDetails() {
        DeadLetterRecord record = new DeadLetterRecord();
        record.setId(1L);
        record.setQueueName("archive.receive.queue");
        record.setMessageBody("{\"archiveId\":1}");
        record.setStatus(DeadLetterRecord.STATUS_PENDING);
        record.setRetryCount(0);
        record.setMaxRetries(3);
        when(deadLetterRecordMapper.selectById(1L)).thenReturn(record);
        doThrow(new AmqpException("connect timeout to mq.internal:5672") {
        }).when(rabbitTemplate).convertAndSend(
                eq("archive.exchange"),
                eq("archive.receive"),
                any(String.class),
                any(MessagePostProcessor.class));

        boolean result = deadLetterService.retry(1L);

        assertFalse(result);
        assertTrue(DeadLetterRecord.STATUS_FAILED.equals(record.getStatus()));
        assertTrue("重试失败，请联系系统管理员查看系统日志".equals(record.getErrorMessage()));
        verify(deadLetterRecordMapper, atLeastOnce()).updateById(argThat(updated ->
                DeadLetterRecord.STATUS_FAILED.equals(updated.getStatus())
                        && "重试失败，请联系系统管理员查看系统日志".equals(updated.getErrorMessage())));
    }
}
