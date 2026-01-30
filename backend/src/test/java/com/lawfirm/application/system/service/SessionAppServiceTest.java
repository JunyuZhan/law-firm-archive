package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.UserSessionDTO;
import com.lawfirm.application.system.dto.UserSessionQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.entity.UserSession;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.system.repository.UserSessionRepository;
import com.lawfirm.infrastructure.persistence.mapper.UserSessionMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** SessionAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionAppService 会话服务测试")
class SessionAppServiceTest {

  private static final Long TEST_SESSION_ID = 100L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long OTHER_USER_ID = 999L;
  private static final String TEST_TOKEN = "test-token-123";

  @Mock private UserSessionRepository sessionRepository;

  @Mock private UserSessionMapper sessionMapper;

  @Mock private UserRepository userRepository;

  @InjectMocks private SessionAppService sessionAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    ReflectionTestUtils.setField(sessionAppService, "sessionExpireHours", 24);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建会话测试")
  class CreateSessionTests {

    @Test
    @DisplayName("应该成功创建会话")
    void createSession_shouldSuccess() {
      // Given
      when(sessionRepository.findActiveSessionsByUserId(TEST_USER_ID))
          .thenReturn(Collections.emptyList());
      when(sessionRepository.save(any(UserSession.class)))
          .thenAnswer(
              invocation -> {
                UserSession session = invocation.getArgument(0);
                session.setId(TEST_SESSION_ID);
                return true;
              });

      // When
      UserSession result =
          sessionAppService.createSession(
              TEST_USER_ID,
              "testuser",
              TEST_TOKEN,
              "refresh-token",
              "127.0.0.1",
              "Mozilla/5.0",
              "PC",
              "Chrome",
              "Windows",
              "北京");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
      assertThat(result.getIsCurrent()).isTrue();
      verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("应该将其他活跃会话标记为已登出（单点登录）")
    void createSession_shouldLogoutOtherSessions() {
      // Given
      UserSession oldSession =
          UserSession.builder()
              .id(200L)
              .userId(TEST_USER_ID)
              .status("ACTIVE")
              .isCurrent(true)
              .build();

      when(sessionRepository.findActiveSessionsByUserId(TEST_USER_ID))
          .thenReturn(List.of(oldSession));
      when(sessionRepository.updateBatchById(anyList())).thenReturn(true);
      when(sessionRepository.save(any(UserSession.class)))
          .thenAnswer(
              invocation -> {
                UserSession session = invocation.getArgument(0);
                session.setId(TEST_SESSION_ID);
                return true;
              });

      // When
      sessionAppService.createSession(
          TEST_USER_ID,
          "testuser",
          TEST_TOKEN,
          "refresh-token",
          "127.0.0.1",
          "Mozilla/5.0",
          "PC",
          "Chrome",
          "Windows",
          "北京");

      // Then
      assertThat(oldSession.getStatus()).isEqualTo("LOGGED_OUT");
      assertThat(oldSession.getIsCurrent()).isFalse();
      verify(sessionRepository).updateBatchById(anyList());
    }
  }

  @Nested
  @DisplayName("查询会话测试")
  class QuerySessionTests {

    @Test
    @DisplayName("应该成功分页查询会话（管理员）")
    void listSessions_shouldSuccess_whenAdmin() {
      // Given
      UserSession session =
          UserSession.builder()
              .id(TEST_SESSION_ID)
              .userId(TEST_USER_ID)
              .username("testuser")
              .status("ACTIVE")
              .build();

      @SuppressWarnings("unchecked")
      IPage<UserSession> page = mock(IPage.class);
      when(page.getRecords()).thenReturn(List.of(session));
      when(page.getTotal()).thenReturn(1L);

      securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
      when(sessionMapper.selectSessionPage(
              ArgumentMatchers.<Page<UserSession>>any(), any(UserSessionQueryDTO.class)))
          .thenReturn(page);
      when(userRepository.listByIds(anyList()))
          .thenReturn(List.of(User.builder().id(TEST_USER_ID).realName("测试用户").build()));

      UserSessionQueryDTO query = new UserSessionQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<UserSessionDTO> result = sessionAppService.listSessions(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("普通用户只能查看自己的会话")
    void listSessions_shouldFilterByUserId_whenNormalUser() {
      // Given
      UserSessionQueryDTO query = new UserSessionQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER"));

      @SuppressWarnings("unchecked")
      IPage<UserSession> page = mock(IPage.class);
      when(page.getRecords()).thenReturn(Collections.emptyList());
      when(page.getTotal()).thenReturn(0L);

      when(sessionMapper.selectSessionPage(
              ArgumentMatchers.<Page<UserSession>>any(), any(UserSessionQueryDTO.class)))
          .thenReturn(page);

      // When
      PageResult<UserSessionDTO> result = sessionAppService.listSessions(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(query.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("应该成功获取当前用户的活跃会话")
    void getMyActiveSessions_shouldSuccess() {
      // Given
      UserSession session =
          UserSession.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).status("ACTIVE").build();

      when(sessionRepository.findActiveSessionsByUserId(TEST_USER_ID)).thenReturn(List.of(session));
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).realName("测试用户").build());

      // When
      List<UserSessionDTO> result = sessionAppService.getMyActiveSessions();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
    }
  }

  @Nested
  @DisplayName("登出会话测试")
  class LogoutSessionTests {

    @Test
    @DisplayName("应该成功根据Token登出会话")
    void logoutSessionByToken_shouldSuccess() {
      // Given
      UserSession session =
          UserSession.builder()
              .id(TEST_SESSION_ID)
              .userId(TEST_USER_ID)
              .token(TEST_TOKEN)
              .status("ACTIVE")
              .build();

      when(sessionRepository.findByToken(TEST_TOKEN)).thenReturn(java.util.Optional.of(session));
      when(sessionRepository.updateById(any(UserSession.class))).thenReturn(true);

      // When
      sessionAppService.logoutSessionByToken(TEST_TOKEN);

      // Then
      assertThat(session.getStatus()).isEqualTo("LOGGED_OUT");
      assertThat(session.getIsCurrent()).isFalse();
      verify(sessionRepository).updateById(session);
    }

    @Test
    @DisplayName("应该成功登出会话")
    void logoutSession_shouldSuccess() {
      // Given
      UserSession session =
          UserSession.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).status("ACTIVE").build();

      securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER"));
      when(sessionRepository.getByIdOrThrow(eq(TEST_SESSION_ID), anyString())).thenReturn(session);
      when(sessionRepository.updateById(any(UserSession.class))).thenReturn(true);

      // When
      sessionAppService.logoutSession(TEST_SESSION_ID);

      // Then
      assertThat(session.getStatus()).isEqualTo("LOGGED_OUT");
      verify(sessionRepository).updateById(session);
    }

    @Test
    @DisplayName("应该失败当无权操作其他用户的会话")
    void logoutSession_shouldFail_whenNotOwnSession() {
      // Given
      UserSession session =
          UserSession.builder().id(TEST_SESSION_ID).userId(OTHER_USER_ID).status("ACTIVE").build();

      securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER"));
      when(sessionRepository.getByIdOrThrow(eq(TEST_SESSION_ID), anyString())).thenReturn(session);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> sessionAppService.logoutSession(TEST_SESSION_ID));
      assertThat(exception.getMessage()).contains("无权操作");
    }
  }

  @Nested
  @DisplayName("强制下线测试")
  class ForceLogoutTests {

    @Test
    @DisplayName("应该成功强制下线会话")
    void forceLogout_shouldSuccess() {
      // Given
      UserSession session =
          UserSession.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).status("ACTIVE").build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
          .thenReturn(true);
      when(sessionRepository.getByIdOrThrow(eq(TEST_SESSION_ID), anyString())).thenReturn(session);
      when(sessionRepository.updateById(any(UserSession.class))).thenReturn(true);

      // When
      sessionAppService.forceLogout(TEST_SESSION_ID, "安全原因");

      // Then
      assertThat(session.getStatus()).isEqualTo("FORCED_LOGOUT");
      verify(sessionRepository).updateById(session);
    }

    @Test
    @DisplayName("应该成功强制下线用户的所有会话")
    void forceLogoutUser_shouldSuccess() {
      // Given
      UserSession session1 =
          UserSession.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).status("ACTIVE").build();

      UserSession session2 =
          UserSession.builder().id(200L).userId(TEST_USER_ID).status("ACTIVE").build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString(), anyString()))
          .thenReturn(true);
      when(sessionRepository.findActiveSessionsByUserId(TEST_USER_ID))
          .thenReturn(List.of(session1, session2));
      when(sessionRepository.updateBatchById(anyList())).thenReturn(true);

      // When
      sessionAppService.forceLogoutUser(TEST_USER_ID, "安全原因");

      // Then
      assertThat(session1.getStatus()).isEqualTo("FORCED_LOGOUT");
      assertThat(session2.getStatus()).isEqualTo("FORCED_LOGOUT");
      verify(sessionRepository).updateBatchById(anyList());
    }
  }

  @Nested
  @DisplayName("验证会话测试")
  class ValidateSessionTests {

    @Test
    @DisplayName("应该成功验证有效会话")
    void validateSession_shouldSuccess() {
      // Given
      UserSession session =
          UserSession.builder()
              .id(TEST_SESSION_ID)
              .token(TEST_TOKEN)
              .status("ACTIVE")
              .expireTime(LocalDateTime.now().plusHours(1))
              .build();

      when(sessionRepository.findByToken(TEST_TOKEN)).thenReturn(java.util.Optional.of(session));

      // When
      UserSession result = sessionAppService.validateSession(TEST_TOKEN);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getToken()).isEqualTo(TEST_TOKEN);
    }

    @Test
    @DisplayName("应该返回null当会话已过期")
    void validateSession_shouldReturnNull_whenExpired() {
      // Given
      UserSession session =
          UserSession.builder()
              .id(TEST_SESSION_ID)
              .token(TEST_TOKEN)
              .status("ACTIVE")
              .expireTime(LocalDateTime.now().minusHours(1))
              .build();

      when(sessionRepository.findByToken(TEST_TOKEN)).thenReturn(java.util.Optional.of(session));

      // When
      UserSession result = sessionAppService.validateSession(TEST_TOKEN);

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("更新最后访问时间测试")
  class UpdateLastAccessTimeTests {

    @Test
    @DisplayName("应该成功更新最后访问时间")
    void updateLastAccessTime_shouldSuccess() {
      // Given
      when(sessionRepository.updateLastAccessTimeByToken(anyString(), any(LocalDateTime.class)))
          .thenReturn(1);

      // When
      sessionAppService.updateLastAccessTime(TEST_TOKEN);

      // Then
      verify(sessionRepository)
          .updateLastAccessTimeByToken(eq(TEST_TOKEN), any(LocalDateTime.class));
    }
  }
}
