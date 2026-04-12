package com.archivesystem.integration;

import com.archivesystem.dto.auth.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证模块集成测试.
 * @author junyuzhan
 */
@DisplayName("认证模块集成测试")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("登录成功 - 正确的用户名密码")
    void login_WithValidCredentials_ShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("登录失败 - 错误的密码")
    void login_WithInvalidPassword_ShouldReturnError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("1004"));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_WithNonExistentUser_ShouldReturnError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("获取当前用户信息 - 已认证")
    void getCurrentUser_WithValidToken_ShouldReturnUserInfo() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/users/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("获取当前用户信息 - 未认证")
    void getCurrentUser_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/users/current"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获取当前用户信息 - 无效Token")
    void getCurrentUser_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/users/current")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("刷新Token - 有效的RefreshToken")
    @org.junit.jupiter.api.Disabled("需要配置完整的Token刷新机制")
    void refreshToken_WithValidRefreshToken_ShouldReturnNewToken() throws Exception {
        // 先登录获取refreshToken
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        // 提取refreshToken
        int start = loginResponse.indexOf("\"refreshToken\":\"") + 16;
        int end = loginResponse.indexOf("\"", start);
        String refreshToken = loginResponse.substring(start, end);

        // 刷新Token - 使用请求体发送refreshToken
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"" + refreshToken + "\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("登出 - 已认证用户")
    void logout_WithValidToken_ShouldSucceed() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
