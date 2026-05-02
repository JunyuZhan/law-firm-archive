package com.archivesystem.service;

import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.entity.CallbackRecord;
import com.archivesystem.repository.CallbackRecordMapper;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.security.OutboundUrlValidator;
import com.archivesystem.service.impl.CallbackServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.archivesystem.entity.ExternalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate mockRestTemplate;
    private OutboundUrlValidator outboundUrlValidator;
    private ExternalSourceMapper externalSourceMapper;
    private CallbackRecordMapper callbackRecordMapper;

    private CallbackServiceImpl callbackService;

    private CallbackMessage testMessage;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        outboundUrlValidator = mock(OutboundUrlValidator.class);
        externalSourceMapper = mock(ExternalSourceMapper.class);
        callbackRecordMapper = mock(CallbackRecordMapper.class);
        ExternalSource source = new ExternalSource();
        source.setSourceType("LAW_FIRM");
        source.setApiKey("test-callback-secret");
        source.setEnabled(true);
        lenient().when(externalSourceMapper.selectBySourceCode("LAW_FIRM")).thenReturn(null);
        lenient().when(externalSourceMapper.selectBySourceType("LAW_FIRM")).thenReturn(source);
        callbackService = new CallbackServiceImpl(
                objectMapper, externalSourceMapper, callbackRecordMapper, outboundUrlValidator, mockRestTemplate);
        
        testMessage = CallbackMessage.builder()
                .archiveId(1L)
                .archiveNo("ARC-20260213-0001")
                .sourceType("LAW_FIRM")
                .sourceId("123")
                .callbackUrl("http://example.com/callback")
                .status("SUCCESS")
                .successCount(5)
                .failedCount(0)
                .totalCount(5)
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSendCallback_EmptyUrl() {
        testMessage.setCallbackUrl(null);
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result); // 空URL跳过回调，返回成功
    }

    @Test
    void testSendCallback_EmptyUrlString() {
        testMessage.setCallbackUrl("");
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result);
    }

    @Test
    void testSendCallback_Success() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result);
        verify(mockRestTemplate).exchange(eq("http://example.com/callback"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSendCallback_ShouldIncludeNonceHeader() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        boolean result = callbackService.sendCallback(testMessage);

        assertTrue(result);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).exchange(eq("http://example.com/callback"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        HttpEntity<?> entity = entityCaptor.getValue();
        assertNotNull(entity.getHeaders().getFirst("X-Callback-Timestamp"));
        assertNotNull(entity.getHeaders().getFirst("X-Callback-Signature"));
        assertNotNull(entity.getHeaders().getFirst("X-Callback-Nonce"));
        assertTrue(entity.getBody() instanceof java.util.Map<?, ?>);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entity.getBody();
        assertFalse(body.containsKey("timestamp"));
    }

    @Test
    void testSendCallback_Non2xxStatus() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertFalse(result);
    }

    @Test
    void testSendCallback_Exception() {
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("网络错误"));
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertFalse(result);
    }

    @Test
    void testLogFailedCallback() {
        // 只验证不抛出异常
        assertDoesNotThrow(() -> 
            callbackService.logFailedCallback(testMessage, "网络超时"));
    }

    @Test
    void testLogFailedCallback_ShouldHideInternalErrorDetails() {
        ArgumentCaptor<CallbackRecord> recordCaptor = ArgumentCaptor.forClass(CallbackRecord.class);

        callbackService.logFailedCallback(testMessage, "connect timeout to http://callback.example.com/internal");

        verify(callbackRecordMapper).insert(recordCaptor.capture());
        assertEquals("回调失败，请联系系统管理员查看系统日志", recordCaptor.getValue().getErrorMessage());
    }

    @Test
    void testLogFailedCallback_WithNullFields() {
        CallbackMessage emptyMessage = CallbackMessage.builder()
                .archiveId(null)
                .archiveNo(null)
                .callbackUrl(null)
                .build();
        
        assertDoesNotThrow(() -> 
            callbackService.logFailedCallback(emptyMessage, "测试错误"));
    }

    @Test
    void testSendCallback_WithErrorMessage() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        
        testMessage.setStatus("FAILED");
        testMessage.setErrorMessage("部分文件处理失败: /srv/archive/internal");
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).exchange(eq("http://example.com/callback"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("档案处理失败，请联系系统管理员查看系统日志", body.get("message"));
        assertEquals("档案处理失败，请联系系统管理员查看系统日志", body.get("errorMessage"));
    }

    @Test
    void testSendCallback_WithNullCompletedAt() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        
        testMessage.setCompletedAt(null);
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result);
    }

    @Test
    void testSendCallback_MissingSourceSecret_ReturnsFalse() {
        when(externalSourceMapper.selectBySourceCode("LAW_FIRM")).thenReturn(null);
        when(externalSourceMapper.selectBySourceType("LAW_FIRM")).thenReturn(null);

        boolean result = callbackService.sendCallback(testMessage);

        assertFalse(result);
        verify(mockRestTemplate, never())
                .exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }
}
