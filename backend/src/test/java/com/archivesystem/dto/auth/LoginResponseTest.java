package com.archivesystem.dto.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void testBuilder() {
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .username("admin")
                .realName("管理员")
                .userType("SYSTEM_ADMIN")
                .build();

        assertEquals("access-token-123", response.getAccessToken());
        assertEquals("refresh-token-456", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals(1L, response.getUserId());
        assertEquals("admin", response.getUsername());
        assertEquals("管理员", response.getRealName());
        assertEquals("SYSTEM_ADMIN", response.getUserType());
    }

    @Test
    void testDefaultTokenType() {
        LoginResponse response = LoginResponse.builder()
                .accessToken("token")
                .build();

        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void testNoArgsConstructor() {
        LoginResponse response = new LoginResponse();

        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
        assertNull(response.getExpiresIn());
        assertNull(response.getUserId());
        assertNull(response.getUsername());
        assertNull(response.getRealName());
        assertNull(response.getUserType());
    }

    @Test
    void testAllArgsConstructor() {
        LoginResponse response = new LoginResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                7200L,
                2L,
                "testuser",
                "测试用户",
                "ADMIN"
        );

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(7200L, response.getExpiresIn());
        assertEquals(2L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("测试用户", response.getRealName());
        assertEquals("ADMIN", response.getUserType());
    }

    @Test
    void testSettersAndGetters() {
        LoginResponse response = new LoginResponse();

        response.setAccessToken("new-access-token");
        response.setRefreshToken("new-refresh-token");
        response.setTokenType("JWT");
        response.setExpiresIn(1800L);
        response.setUserId(3L);
        response.setUsername("newuser");
        response.setRealName("新用户");
        response.setUserType("USER");

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("JWT", response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertEquals(3L, response.getUserId());
        assertEquals("newuser", response.getUsername());
        assertEquals("新用户", response.getRealName());
        assertEquals("USER", response.getUserType());
    }

    @Test
    void testEqualsAndHashCode() {
        LoginResponse response1 = LoginResponse.builder()
                .accessToken("token")
                .userId(1L)
                .build();

        LoginResponse response2 = LoginResponse.builder()
                .accessToken("token")
                .userId(1L)
                .build();

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void testToString() {
        LoginResponse response = LoginResponse.builder()
                .accessToken("token")
                .username("admin")
                .build();

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("LoginResponse"));
    }
}
