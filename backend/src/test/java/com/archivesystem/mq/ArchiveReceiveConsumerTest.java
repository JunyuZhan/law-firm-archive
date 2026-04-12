package com.archivesystem.mq;

import com.archivesystem.config.MetricsConfig;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.PushRecordMapper;
import com.archivesystem.service.FileStorageService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchiveReceiveConsumerTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private PushRecordMapper pushRecordMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MetricsConfig metricsConfig;

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
    private ArchiveReceiveConsumer archiveReceiveConsumer;

    private ArchiveReceiveMessage testMessage;
    private Archive testArchive;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId(1L);
        testArchive.setArchiveNo("ARC-20260213-0001");
        testArchive.setStatus(Archive.STATUS_PROCESSING);

        ArchiveReceiveMessage.FileInfo fileInfo = ArchiveReceiveMessage.FileInfo.builder()
                .fileName("test.pdf")
                .downloadUrl("http://example.com/test.pdf")
                .fileCategory("DOCUMENT")
                .sortOrder(1)
                .build();

        testMessage = ArchiveReceiveMessage.builder()
                .messageId("msg-001")
                .archiveId(1L)
                .archiveNo("ARC-20260213-0001")
                .sourceType("LAW_FIRM")
                .sourceId("123")
                .callbackUrl("http://callback.example.com")
                .files(Arrays.asList(fileInfo))
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(pushRecordMapper.insert(any())).thenReturn(1);
        when(pushRecordMapper.updateById(any())).thenReturn(1);
    }

    @Test
    void testHandleArchiveReceive_Success() throws Exception {
        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setFileSize(1024L);
        PushRecord pushRecord = new PushRecord();
        pushRecord.setId(10L);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(digitalFile);
        when(pushRecordMapper.selectOne(any())).thenReturn(pushRecord);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(archiveMapper, times(2)).updateById(any(Archive.class));
        verify(fileStorageService).downloadAndStore(1L, "http://example.com/test.pdf", "test.pdf", "DOCUMENT", 1);
        verify(channel).basicAck(1L, false);
        verify(metricsConfig).recordArchiveReceiveSuccess();
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
        verify(pushRecordMapper, atLeastOnce()).updateById(argThat(record ->
                PushRecord.FILE_STATUS_CALLBACK_SENT.equals(record.getFileStatus())));
    }

    @Test
    void testHandleArchiveReceive_ArchiveNotFound() throws Exception {
        when(archiveMapper.selectById(1L)).thenReturn(null);
        doNothing().when(channel).basicReject(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(channel).basicReject(1L, false);
        verify(metricsConfig).recordArchiveReceiveFailed();
        verify(fileStorageService, never()).downloadAndStore(any(), any(), any(), any(), anyInt());
    }

    @Test
    void testHandleArchiveReceive_NoFiles() throws Exception {
        testMessage.setFiles(null);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(fileStorageService, never()).downloadAndStore(any(), any(), any(), any(), anyInt());
        verify(channel).basicAck(1L, false);
    }

    @Test
    void testHandleArchiveReceive_EmptyFiles() throws Exception {
        testMessage.setFiles(Collections.emptyList());

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(fileStorageService, never()).downloadAndStore(any(), any(), any(), any(), anyInt());
        verify(channel).basicAck(1L, false);
    }

    @Test
    void testHandleArchiveReceive_FileDownloadFailed() throws Exception {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("下载失败"));
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(channel).basicAck(1L, false);
        // updateById is called twice: once for PROCESSING, once for final status
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
    }

    @Test
    void testHandleArchiveReceive_PartialSuccess() throws Exception {
        ArchiveReceiveMessage.FileInfo fileInfo2 = ArchiveReceiveMessage.FileInfo.builder()
                .fileName("test2.pdf")
                .downloadUrl("http://example.com/test2.pdf")
                .sortOrder(2)
                .build();
        testMessage.setFiles(Arrays.asList(testMessage.getFiles().get(0), fileInfo2));

        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setFileSize(1024L);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(eq(1L), eq("http://example.com/test.pdf"), anyString(), any(), anyInt()))
                .thenReturn(digitalFile);
        when(fileStorageService.downloadAndStore(eq(1L), eq("http://example.com/test2.pdf"), anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("下载失败"));
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(channel).basicAck(1L, false);
        // updateById is called twice: once for PROCESSING, once for PARTIAL status
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
    }

    @Test
    void testHandleArchiveReceive_FinalStateWithPendingCallback_ShouldDispatchCallback() throws Exception {
        Archive completedArchive = new Archive();
        completedArchive.setId(1L);
        completedArchive.setArchiveNo("ARC-20260213-0001");
        completedArchive.setStatus(Archive.STATUS_RECEIVED);

        PushRecord pushRecord = new PushRecord();
        pushRecord.setId(10L);
        pushRecord.setCallbackUrl("http://callback.example.com");
        pushRecord.setFileStatus(PushRecord.FILE_STATUS_CALLBACK_PENDING);
        pushRecord.setSuccessFiles(1);
        pushRecord.setFailedFiles(0);
        pushRecord.setTotalFiles(1);
        pushRecord.setPushStatus(PushRecord.STATUS_SUCCESS);

        when(archiveMapper.selectById(1L)).thenReturn(completedArchive);
        when(pushRecordMapper.selectOne(any())).thenReturn(pushRecord);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
        verify(channel).basicAck(1L, false);
        verify(fileStorageService, never()).downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt());
        verify(pushRecordMapper, atLeastOnce()).updateById(argThat(record ->
                PushRecord.FILE_STATUS_CALLBACK_SENT.equals(record.getFileStatus())));
    }

    @Test
    void testHandleArchiveReceive_NoCallback() throws Exception {
        testMessage.setCallbackUrl(null);

        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setFileSize(1024L);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(digitalFile);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
    }

    @Test
    void testHandleArchiveReceive_EmptyCallbackUrl() throws Exception {
        testMessage.setCallbackUrl("");

        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setFileSize(1024L);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(digitalFile);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CallbackMessage.class));
    }

    @Test
    void testHandleArchiveReceive_ExceptionWithRetry() throws Exception {
        testMessage.setRetryCount(0);
        testMessage.setMaxRetries(3);

        when(archiveMapper.selectById(1L)).thenThrow(new RuntimeException("数据库异常"));
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(metricsConfig).recordArchiveReceiveFailed();
        verify(rabbitTemplate).convertAndSend(
                eq("archive.exchange"),
                eq("archive.receive"),
                same(testMessage));
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    void testHandleArchiveReceive_ExceptionMaxRetriesExceeded() throws Exception {
        testMessage.setRetryCount(3);
        testMessage.setMaxRetries(3);
        testMessage.setCallbackUrl("http://callback.example.com");

        // Use thenReturn for first call (for archive not found check in error handler), 
        // then throw exception for normal flow
        when(archiveMapper.selectById(1L))
                .thenThrow(new RuntimeException("数据库异常"))
                .thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        doNothing().when(channel).basicReject(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(metricsConfig).recordArchiveReceiveFailed();
        verify(channel).basicReject(1L, false);
    }

    @Test
    void testHandleArchiveReceive_ExceptionWithRetry_RequeueCurrentMessageWhenRepublishFails() throws Exception {
        testMessage.setRetryCount(0);
        testMessage.setMaxRetries(3);

        when(archiveMapper.selectById(1L)).thenThrow(new RuntimeException("数据库异常"));
        doThrow(new AmqpRejectAndDontRequeueException("重投失败"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(ArchiveReceiveMessage.class));

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(metricsConfig).recordArchiveReceiveFailed();
        verify(channel).basicNack(1L, false, true);
        verify(channel, never()).basicAck(1L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    void testHandleArchiveReceive_FileSizeNull() throws Exception {
        DigitalFile digitalFile = new DigitalFile();
        digitalFile.setId(1L);
        digitalFile.setFileSize(null);

        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(digitalFile);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        // updateById is called twice: once for PROCESSING, once for final status
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
    }

    @Test
    void testHandleArchiveReceive_DownloadReturnsNull() throws Exception {
        when(archiveMapper.selectById(1L)).thenReturn(testArchive);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);
        when(fileStorageService.downloadAndStore(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(null);
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        archiveReceiveConsumer.handleArchiveReceive(testMessage, amqpMessage, channel);

        verify(channel).basicAck(1L, false);
        // updateById is called twice: once for PROCESSING, once for final status
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
    }
}
