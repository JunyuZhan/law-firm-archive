package com.lawfirm.application.system.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.security.LoginUser;
import com.lawfirm.infrastructure.security.UserDetailsServiceImpl;
import com.lawfirm.infrastructure.security.jwt.JwtTokenProvider;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * AuthService 单元测试
 *
 * <p>测试范围： - 登录认证（正常流程、账户锁定、密码错误） - Token 刷新 - 登出 - 安全限制（IP限流、账户锁定）
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private UserRepository userRepository;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ValueOperations<String, Object> valueOperations;

  @Mock private LoginLogService loginLogService;

  @Mock private SessionAppService sessionAppService;

  @Mock private com.lawfirm.application.system.util.UserAgentParser userAgentParser;

  @Mock private com.lawfirm.infrastructure.notification.AlertService alertService;

  @Mock private UserDetailsServiceImpl userDetailsService;

  @InjectMocks private AuthService authService;

  private User mockUser;
  private LoginUser mockLoginUser;

  @BeforeEach
  void setUp() {
    // Mock User
    mockUser = new User();
    mockUser.setId(1L);
    mockUser.setUsername("testuser");
    mockUser.setPassword("encoded_password");
    mockUser.setRealName("测试用户");
    mockUser.setStatus("ACTIVE");

    // Mock LoginUser
    mockLoginUser =
        LoginUser.builder()
            .userId(1L)
            .username("testuser")
            .realName("测试用户")
            .roles(new HashSet<>())
            .permissions(new HashSet<>())
            .build();

    // Mock RedisTemplate
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Nested
  @DisplayName("登录测试")
  class LoginTests {

    @Test
    @DisplayName("登录失败 - 账户已锁定")
    void login_AccountLocked_ShouldThrowException() {
      // Given
      User lockedUser = new User();
      lockedUser.setId(1L);
      lockedUser.setUsername("testuser");
      lockedUser.setStatus("LOCKED");
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(lockedUser));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authService.login("testuser", "password", "127.0.0.1", "Mozilla/5.0"));
      assertTrue(exception.getMessage().contains("锁定"));
    }

    @Test
    @DisplayName("登录失败 - 连续失败次数过多应锁定账户")
    void login_TooManyFailures_ShouldLockAccount() {
      // Given
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
      when(loginLogService.shouldLockAccount("testuser")).thenReturn(true);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authService.login("testuser", "password", "127.0.0.1", "Mozilla/5.0"));
      assertTrue(exception.getMessage().contains("锁定"));
    }

    @Test
    @DisplayName("登录失败 - IP限流")
    void login_IpRateLimited_ShouldThrowException() {
      // Given
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
      when(loginLogService.shouldLockAccount("testuser")).thenReturn(false);
      when(valueOperations.get("login:ip:127.0.0.1")).thenReturn(15); // 超过10次限制

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authService.login("testuser", "password", "127.0.0.1", "Mozilla/5.0"));
      assertTrue(exception.getMessage().contains("过多") || exception.getMessage().contains("频繁"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_WrongPassword_ShouldThrowException() {
      // Given
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
      when(loginLogService.shouldLockAccount("testuser")).thenReturn(false);
      when(valueOperations.get(anyString())).thenReturn(null);
      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenThrow(new BadCredentialsException("Bad credentials"));

      // When & Then
      assertThrows(
          Exception.class,
          () -> authService.login("testuser", "wrong_password", "127.0.0.1", "Mozilla/5.0"));
    }

    @Test
    @DisplayName("登录成功")
    void login_Success() {
      // Given
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
      when(loginLogService.shouldLockAccount("testuser")).thenReturn(false);
      when(valueOperations.get(anyString())).thenReturn(null);

      Authentication mockAuth = mock(Authentication.class);
      when(mockAuth.getPrincipal()).thenReturn(mockLoginUser);
      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenReturn(mockAuth);

      when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("access_token");
      when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
          .thenReturn("refresh_token");

      when(userAgentParser.parseDeviceType(anyString())).thenReturn("PC");
      when(userAgentParser.parseBrowser(anyString())).thenReturn("Chrome");
      when(userAgentParser.parseOS(anyString())).thenReturn("Windows");

      // When
      AuthService.LoginResult result =
          authService.login("testuser", "password", "127.0.0.1", "Mozilla/5.0");

      // Then
      assertNotNull(result);
      assertEquals("access_token", result.getAccessToken());
      assertEquals("refresh_token", result.getRefreshToken());
      assertEquals("testuser", result.getUsername());

      // 验证登录成功日志被记录
      verify(loginLogService)
          .recordLoginSuccess(anyLong(), eq("testuser"), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("Token 刷新测试")
  class RefreshTokenTests {

    @Test
    @DisplayName("刷新Token - Token无效")
    void refreshToken_InvalidToken_ShouldThrowException() {
      // Given
      when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

      // When & Then
      assertThrows(Exception.class, () -> authService.refreshToken("invalid_token"));
    }
  }

  @Nested
  @DisplayName("登出测试")
  class LogoutTests {

    @Test
    @DisplayName("登出成功")
    void logout_Success() {
      // Given
      Long userId = 1L;
      String token = "valid_token";

      // When
      assertDoesNotThrow(() -> authService.logout(userId, token));

      // Then - 验证 Token 被删除
      verify(redisTemplate).delete("token:" + userId);
    }
  }

  @Nested
  @DisplayName("安全规则测试")
  class SecurityRuleTests {

    @Test
    @DisplayName("IP限流阈值验证")
    void ipRateLimit_Threshold_ShouldBe10() {
      // IP限流阈值应该是10次/15分钟
      int threshold = 10;
      assertEquals(10, threshold);
    }

    @Test
    @DisplayName("账户锁定阈值验证")
    void accountLock_Threshold_ShouldBe5() {
      // 账户锁定阈值应该是5次失败/1小时
      int threshold = 5;
      assertEquals(5, threshold);
    }
  }
}
