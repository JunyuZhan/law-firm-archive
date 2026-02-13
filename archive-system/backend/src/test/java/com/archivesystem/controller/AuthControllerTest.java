package com.archivesystem.controller;

import com.archivesystem.dto.auth.LoginRequest;
import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.security.JwtUtils;
import com.archivesystem.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserMapper userMapper;

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
                "ADMIN",
                User.STATUS_ACTIVE,
                Collections.emptyList()
        );
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
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

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
                .andExpect(jsonPath("$.code").value("1004"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void testRefresh_Success() throws Exception {
        String refreshToken = "valid_refresh_token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(1L, "testuser", "ADMIN"))
                .thenReturn("new_access_token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
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
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(999L);
        when(userMapper.selectById(999L)).thenReturn(null);

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
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);

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
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenThrow(new RuntimeException("Token解析失败"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1002"));
    }

    @Test
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }
}
