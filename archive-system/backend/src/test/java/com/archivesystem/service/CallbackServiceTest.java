package com.archivesystem.service;

import com.archivesystem.mq.CallbackMessage;
import com.archivesystem.service.impl.CallbackServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate mockRestTemplate;

    private CallbackServiceImpl callbackService;

    private CallbackMessage testMessage;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        callbackService = new CallbackServiceImpl(objectMapper, null);
        ReflectionTestUtils.setField(callbackService, "restTemplate", mockRestTemplate);
        
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
        
        testMessage.setStatus("PARTIAL");
        testMessage.setErrorMessage("部分文件处理失败");
        
        boolean result = callbackService.sendCallback(testMessage);
        
        assertTrue(result);
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
}
