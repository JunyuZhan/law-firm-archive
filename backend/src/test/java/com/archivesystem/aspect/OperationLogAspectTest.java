package com.archivesystem.aspect;

import com.archivesystem.entity.OperationLog;
import com.archivesystem.service.OperationLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperationLogAspectTest {

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private OperationLogAspect operationLogAspect;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("User-Agent", "TestAgent/1.0");
    }

    @Test
    void testAfterReturning_WithLogAnnotation() throws Exception {
        // 创建测试方法
        Method method = TestService.class.getMethod("createArchive", Long.class, String.class);
        assertNotNull(method.getAnnotation(Log.class));

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "Test Archive"});

        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            operationLogAspect.afterReturning(joinPoint, null);

            ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
            verify(operationLogService).log(captor.capture());

            OperationLog log = captor.getValue();
            assertEquals("ARCHIVE", log.getObjectType());
            assertEquals("CREATE", log.getOperationType());
            assertEquals("创建档案", log.getOperationDesc());
            assertEquals("1", log.getObjectId());
            assertNotNull(log.getOperatedAt());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testAfterReturning_NoLogAnnotation() throws Exception {
        Method method = TestService.class.getMethod("methodWithoutLog");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);

        operationLogAspect.afterReturning(joinPoint, null);

        verify(operationLogService, never()).log(any(OperationLog.class));
    }

    @Test
    void testAfterReturning_WithPasswordParameter() throws Exception {
        Method method = TestService.class.getMethod("updatePassword", Long.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "secretPassword123"});

        operationLogAspect.afterReturning(joinPoint, null);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).log(captor.capture());

        OperationLog log = captor.getValue();
        // Password parameter should not be in details
        if (log.getOperationDetail() != null) {
            assertFalse(log.getOperationDetail().containsKey("password"));
        }
    }

    @Test
    void testAfterReturning_WithApiKeyAndTokenParameters() throws Exception {
        Method method = TestService.class.getMethod("rotateCredential", Long.class, String.class, String.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "visible-name", "api-key-123", "token-456"});

        operationLogAspect.afterReturning(joinPoint, null);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).log(captor.capture());

        OperationLog log = captor.getValue();
        assertEquals("1", log.getObjectId());
        assertNotNull(log.getOperationDetail());
        assertEquals("visible-name", log.getOperationDetail().get("name"));
        assertFalse(log.getOperationDetail().containsKey("apiKey"));
        assertFalse(log.getOperationDetail().containsKey("accessToken"));
    }

    @Test
    void testAfterReturning_WithXForwardedFor() throws Exception {
        Method method = TestService.class.getMethod("createArchive", Long.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "Test"});

        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            operationLogAspect.afterReturning(joinPoint, null);

            ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
            verify(operationLogService).log(captor.capture());

            OperationLog log = captor.getValue();
            assertEquals("10.0.0.1", log.getOperatorIp());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testAfterReturning_WithProxyClientIP() throws Exception {
        Method method = TestService.class.getMethod("createArchive", Long.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "Test"});

        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("Proxy-Client-IP", "10.0.0.5");
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            operationLogAspect.afterReturning(joinPoint, null);

            ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
            verify(operationLogService).log(captor.capture());

            OperationLog log = captor.getValue();
            assertEquals("10.0.0.5", log.getOperatorIp());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testAfterReturning_NoRequestContext() throws Exception {
        Method method = TestService.class.getMethod("createArchive", Long.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "Test"});

        RequestContextHolder.resetRequestAttributes();

        operationLogAspect.afterReturning(joinPoint, null);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).log(captor.capture());

        OperationLog log = captor.getValue();
        assertNull(log.getOperatorIp());
    }

    @Test
    void testAfterReturning_ExceptionDuringLogging() throws Exception {
        Method method = TestService.class.getMethod("createArchive", Long.class, String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "Test"});
        doThrow(new RuntimeException("Database error")).when(operationLogService).log(any(OperationLog.class));

        // Should not throw exception
        assertDoesNotThrow(() -> operationLogAspect.afterReturning(joinPoint, null));
    }

    // Test service class with Log annotations
    public static class TestService {

        @Log(objectType = "ARCHIVE", operationType = "CREATE", description = "创建档案")
        public void createArchive(Long id, String name) {
        }

        @Log(objectType = "USER", operationType = "UPDATE", description = "更新密码")
        public void updatePassword(Long id, String password) {
        }

        @Log(objectType = "USER", operationType = "UPDATE", description = "轮换凭据")
        public void rotateCredential(Long id, String name, String apiKey, String accessToken) {
        }

        public void methodWithoutLog() {
        }
    }
}
