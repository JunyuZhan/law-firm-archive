package com.archivesystem.service;

import com.archivesystem.entity.AccessLog;
import com.archivesystem.repository.AccessLogMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.AccessLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessLogServiceTest {

    @Mock
    private AccessLogMapper accessLogMapper;

    @InjectMocks
    private AccessLogServiceImpl accessLogService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testLogAccess_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logAccess(100L, 200L, AccessLog.TYPE_VIEW, "192.168.1.1"));

            verify(accessLogMapper).insert(argThat(log -> 
                log.getArchiveId().equals(100L) &&
                log.getFileId().equals(200L) &&
                log.getAccessType().equals(AccessLog.TYPE_VIEW) &&
                log.getAccessIp().equals("192.168.1.1")));
        }
    }

    @Test
    void testLogAccess_WithNullFileId() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logAccess(100L, null, AccessLog.TYPE_VIEW, "192.168.1.1"));

            verify(accessLogMapper).insert(any(AccessLog.class));
        }
    }

    @Test
    void testLogAccess_ExceptionHandled() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenThrow(new RuntimeException("无登录用户"));

            // 即使出错也不应该抛出异常（异步方法静默失败）
            assertDoesNotThrow(() -> 
                accessLogService.logAccess(100L, 200L, AccessLog.TYPE_VIEW, "192.168.1.1"));
        }
    }

    @Test
    void testLogDownload() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logDownload(100L, 200L, "192.168.1.1"));

            verify(accessLogMapper).insert(argThat(log -> 
                log.getAccessType().equals(AccessLog.TYPE_DOWNLOAD)));
        }
    }

    @Test
    void testLogPreview() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logPreview(100L, 200L, "192.168.1.1"));

            verify(accessLogMapper).insert(argThat(log -> 
                log.getAccessType().equals(AccessLog.TYPE_PREVIEW)));
        }
    }

    @Test
    void testLogSearch_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logSearch("测试关键词", 10, 150L, "192.168.1.1"));

            verify(accessLogMapper).insert(argThat(log -> 
                log.getAccessType().equals(AccessLog.TYPE_SEARCH) &&
                log.getSearchKeyword().equals("测试关键词") &&
                log.getSearchResultCount().equals(10) &&
                log.getDuration().equals(150L)));
        }
    }

    @Test
    void testLogSearch_ZeroResults() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                accessLogService.logSearch("不存在的内容", 0, 50L, "192.168.1.1"));

            verify(accessLogMapper).insert(argThat(log -> 
                log.getSearchResultCount().equals(0)));
        }
    }

    @Test
    void testLogSearch_ExceptionHandled() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenThrow(new RuntimeException("无登录用户"));

            // 即使出错也不应该抛出异常
            assertDoesNotThrow(() -> 
                accessLogService.logSearch("测试", 5, 100L, "192.168.1.1"));
        }
    }

    @Test
    void testLogAccess_MapperException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("测试用户");

            when(accessLogMapper.insert(any(AccessLog.class))).thenThrow(new RuntimeException("数据库错误"));

            // 即使数据库出错也不应该抛出异常
            assertDoesNotThrow(() -> 
                accessLogService.logAccess(100L, 200L, AccessLog.TYPE_VIEW, "192.168.1.1"));
        }
    }
}
