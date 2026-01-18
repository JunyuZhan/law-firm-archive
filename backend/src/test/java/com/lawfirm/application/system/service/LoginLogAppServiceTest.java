package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.LoginLogDTO;
import com.lawfirm.application.system.dto.LoginLogQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.domain.system.repository.LoginLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginLogAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginLogAppService 登录日志服务测试")
class LoginLogAppServiceTest {

    private static final Long TEST_LOG_ID = 100L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private LoginLogAppService loginLogAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        ReflectionTestUtils.setField(loginLogAppService, "loginFailureWindowHours", 1);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("查询登录日志测试")
    class QueryLoginLogTests {

        @Test
        @DisplayName("应该成功分页查询登录日志（管理员）")
        void listLoginLogs_shouldSuccess_whenAdmin() {
            // Given
            LoginLog log = LoginLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(TEST_USER_ID)
                    .username("testuser")
                    .status("SUCCESS")
                    .loginTime(LocalDateTime.now())
                    .build();

            Page<LoginLog> page = new Page<>(1, 10);
            page.setRecords(List.of(log));
            page.setTotal(1L);

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            when(loginLogRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            LoginLogQueryDTO query = new LoginLogQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<LoginLogDTO> result = loginLogAppService.listLoginLogs(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("普通用户只能查看自己的登录日志")
        void listLoginLogs_shouldFilterByUserId_whenNormalUser() {
            // Given
            LoginLogQueryDTO query = new LoginLogQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(false);

            Page<LoginLog> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0L);

            when(loginLogRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // When
            PageResult<LoginLogDTO> result = loginLogAppService.listLoginLogs(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(query.getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("应该成功获取登录日志详情（管理员）")
        void getLoginLog_shouldSuccess_whenAdmin() {
            // Given
            LoginLog log = LoginLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(TEST_USER_ID)
                    .username("testuser")
                    .status("SUCCESS")
                    .loginTime(LocalDateTime.now())
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(loginLogRepository.findById(TEST_LOG_ID)).thenReturn(log);

            // When
            LoginLogDTO result = loginLogAppService.getLoginLog(TEST_LOG_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("普通用户只能查看自己的日志详情")
        void getLoginLog_shouldFail_whenNotOwnLog() {
            // Given
            LoginLog log = LoginLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(OTHER_USER_ID)
                    .username("otheruser")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(false);
            when(loginLogRepository.findById(TEST_LOG_ID)).thenReturn(log);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> loginLogAppService.getLoginLog(TEST_LOG_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }
    }

    @Nested
    @DisplayName("查询最近登录记录测试")
    class GetRecentLogsTests {

        @Test
        @DisplayName("应该成功获取最近登录记录")
        void getRecentLogsByUserId_shouldSuccess() {
            // Given
            LoginLog log = LoginLog.builder()
                    .id(TEST_LOG_ID)
                    .userId(TEST_USER_ID)
                    .username("testuser")
                    .loginTime(LocalDateTime.now())
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(loginLogRepository.findByUserId(TEST_USER_ID, 0, 10)).thenReturn(List.of(log));

            // When
            List<LoginLogDTO> result = loginLogAppService.getRecentLogsByUserId(TEST_USER_ID, 10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该限制最大查询数量")
        void getRecentLogsByUserId_shouldLimitMaxCount() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(loginLogRepository.findByUserId(eq(TEST_USER_ID), eq(0), eq(100)))
                    .thenReturn(Collections.emptyList());

            // When
            loginLogAppService.getRecentLogsByUserId(TEST_USER_ID, 200);

            // Then
            verify(loginLogRepository).findByUserId(eq(TEST_USER_ID), eq(0), eq(100));
        }
    }

    @Nested
    @DisplayName("统计登录失败次数测试")
    class CountFailureTests {

        @Test
        @DisplayName("应该成功统计登录失败次数")
        void countFailureByUsername_shouldSuccess() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(loginLogRepository.countFailureByUsername(eq("testuser"), any(LocalDateTime.class)))
                    .thenReturn(3);

            // When
            int result = loginLogAppService.countFailureByUsername("testuser");

            // Then
            assertThat(result).isEqualTo(3);
        }
    }
}
