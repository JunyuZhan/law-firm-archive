package com.archivesystem.controller;

import com.archivesystem.dto.auth.LoginRequest;
import com.archivesystem.entity.User;
import com.archivesystem.security.JwtUtils;
import com.archivesystem.security.LoginSecurityService;
import com.archivesystem.security.TokenBlacklistService;
import com.archivesystem.security.UserDetailsImpl;
import com.archivesystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private LoginSecurityService loginSecurityService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private User testUser;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setRealName("测试用户");
        testUser.setUserType("ADMIN");
        testUser.setStatus(User.STATUS_ACTIVE);

        userDetails = new UserDetailsImpl(
                1L,
                "testuser",
                "encoded_password",
                "测试用户",
                "测试部",
                "ADMIN",
                User.STATUS_ACTIVE,
                Collections.emptyList()
        );

        when(loginSecurityService.isIpLocked(anyString())).thenReturn(false);
        when(loginSecurityService.isAccountLocked(anyString())).thenReturn(false);
        when(loginSecurityService.recordFailedAttempt(anyString(), anyString())).thenReturn(1);
        when(loginSecurityService.getRemainingAttempts(anyString())).thenReturn(4);
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("access_token_xxx");
        when(jwtUtils.generateRefreshToken(anyLong()))
                .thenReturn("refresh_token_xxx");
        doNothing().when(userService).recordLoginSuccess(eq(1L), anyString());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_xxx"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token_xxx"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void testLogin_DisabledUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("disableduser");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("用户已禁用"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1003"))
                .andExpect(jsonPath("$.message").value("用户已被禁用"));
    }

    @Test
    void testLogin_BadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("密码错误"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1004"));
    }

    @Test
    void testRefresh_Success() throws Exception {
        String refreshToken = "valid_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(jwtUtils.parseToken(refreshToken)).thenReturn(claims);
        when(userService.getActiveById(1L)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(1L, "testuser", "ADMIN"))
                .thenReturn("new_access_token");
        when(jwtUtils.generateRefreshToken(1L)).thenReturn("new_refresh_token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new_refresh_token"));
        verify(tokenBlacklistService).addToBlacklist(refreshToken);
    }

    @Test
    void testRefresh_InvalidToken() throws Exception {
        String refreshToken = "invalid_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001"));
    }

    @Test
    void testRefresh_NotRefreshToken() throws Exception {
        String accessToken = "access_token";

        when(jwtUtils.validateToken(accessToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(accessToken)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001"));
    }

    @Test
    void testRefresh_UserNotFound() throws Exception {
        String refreshToken = "valid_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(999L);
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(jwtUtils.parseToken(refreshToken)).thenReturn(claims);
        when(userService.getActiveById(999L)).thenReturn(null);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1003"));
    }

    @Test
    void testRefresh_UserDisabled() throws Exception {
        String refreshToken = "valid_refresh_token";
        testUser.setStatus(User.STATUS_DISABLED);

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(jwtUtils.parseToken(refreshToken)).thenReturn(claims);
        when(userService.getActiveById(1L)).thenReturn(null);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1003"));
    }

    @Test
    void testRefresh_Exception() throws Exception {
        String refreshToken = "valid_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtils.parseToken(refreshToken)).thenThrow(new RuntimeException("Token解析失败"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1002"));
    }

    @Test
    void testLogout_Success() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(jwtUtils.parseToken("access_token")).thenReturn(claims);
        when(jwtUtils.getRefreshExpirationMillis()).thenReturn(604800000L);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("登出成功"));
        verify(tokenBlacklistService).addToBlacklist("access_token");
        verify(tokenBlacklistService).blacklistUserTokens(1L, 604800L);
    }

    @Test
    void testRefresh_BlacklistedToken() throws Exception {
        String refreshToken = "valid_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(jwtUtils.parseToken(refreshToken)).thenReturn(claims);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(true);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001"));
    }
}
